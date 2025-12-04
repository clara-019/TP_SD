package Launcher;

import Event.Event;
import Event.*;
import Node.NodeEnum;
import Traffic.RoadEnum;
import Vehicle.*;

import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.logging.*;

/**
 * Controller coordinating the simulator and the dashboard UI.
 * <p>
 * This class mediates between the {@link Simulator}, the UI-facing
 * model ({@link MapModel}), and the visual components such as
 * {@link MapRenderer} and {@code VehicleSprite} instances. It consumes
 * simulator {@link Event}s from a blocking queue, updates the
 * {@link Statistics} object and the sprite map, and invokes UI callbacks
 * (logging, status and stats updates) on the Swing EDT where appropriate.
 */
public class DashboardController {
    private static final Logger LOGGER = Logger.getLogger(DashboardController.class.getName());

    private static final long PASS_DELAY_MS = 200L;
    private static final int AUTO_STOP_MS = 60_000;

    private final MapModel model;
    private final Map<String, VehicleSprite> sprites;
    private final Map<NodeEnum, Point> nodePositions;
    private final MapRenderer renderer;

    private final Statistics stats = new Statistics();

    private Simulator simulator;
    private BlockingQueue<Event> eventQueue;

    private final ExecutorService executor = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r);
        t.setDaemon(true);
        return t;
    });

    private volatile Future<?> eventConsumerFuture;
    private javax.swing.Timer autoStopTimer;
    private final AtomicBoolean gracefulStopping = new AtomicBoolean(false);

    private final Map<RoadEnum, Deque<AbstractMap.SimpleEntry<Long, String>>> passingSchedule = new EnumMap<>(
            RoadEnum.class);

    private final Consumer<String> logCb;
    private final Runnable updateStatsCb;
    private final Consumer<String> statusTextCb;
    private final Consumer<Color> statusColorCb;

    /**
     * Create a new DashboardController.
     *
     * @param model         the shared {@link MapModel} containing sprites and node
     *                      positions
     * @param renderer      the renderer used to request repaints
     * @param logCb         callback to append messages to the dashboard log (called
     *                      on calling thread)
     * @param updateStatsCb callback to refresh UI statistics
     * @param statusTextCb  callback to update the status label text
     * @param statusColorCb callback to update the status label color
     */
    public DashboardController(MapModel model, MapRenderer renderer, Consumer<String> logCb, Runnable updateStatsCb,
            Consumer<String> statusTextCb, Consumer<Color> statusColorCb) {
        this.model = model;
        this.sprites = this.model.getSprites();
        this.nodePositions = this.model.getNodePositions();
        this.renderer = renderer;

        this.logCb = logCb;
        this.updateStatsCb = updateStatsCb;
        this.statusTextCb = statusTextCb;
        this.statusColorCb = statusColorCb;

        for (RoadEnum r : RoadEnum.values()) {
            this.passingSchedule.put(r, new ArrayDeque<>());
        }
    }

    /**
     * Return the current {@link Statistics} object maintained by the
     * controller.
     *
     * @return the statistics object
     */
    public Statistics getStatistics() {
        return this.stats;
    }

    /**
     * Start the simulator and the background event consumer.
     */
    public synchronized void startSimulation() {
        if (this.simulator != null && this.simulator.isRunning()) {
            logCb.accept("Simulator already running");
            return;
        }

        this.renderer.revalidate();
        this.renderer.repaint();

        this.simulator = new Simulator();
        this.eventQueue = this.simulator.getEventQueue();

        executor.execute(() -> {
            try {
                this.simulator.startSimulation();
            } catch (Exception ex) {
                LOGGER.log(Level.SEVERE, "Simulator thread crashed", ex);
                logCb.accept("Simulator crashed: " + ex.getMessage());
            }
        });

        startEventConsumer();

        this.statusTextCb.accept("RUNNING");
        this.statusColorCb.accept(Color.GREEN);
        logCb.accept("Simulator started");

        scheduleAutoStopTimer();
    }

    /**
     * Stop the simulator immediately and clear runtime state.
     */
    public synchronized void stopSimulation() {
        if (this.simulator != null) {
            try {
                this.simulator.stopSimulation();
            } catch (Exception ex) {
                LOGGER.log(Level.WARNING, "Error stopping simulator", ex);
                logCb.accept("Error stopping simulator: " + ex.getMessage());
            }
            this.simulator = null;
        }

        stopEventConsumer();

        if (this.autoStopTimer != null) {
            this.autoStopTimer.stop();
            this.autoStopTimer = null;
        }

        this.statusTextCb.accept("STOPPED");
        this.statusColorCb.accept(Color.RED);

        clearEventQueue();

        synchronized (this.sprites) {
            this.sprites.clear();
        }

        SwingUtilities.invokeLater(this.renderer::repaint);
        logCb.accept("Simulator stopped");
    }

    /**
     * Request a graceful shutdown: stop producing new vehicles and wait
     * for currently moving vehicles and pending events to finish.
     */
    public void requestGracefulStop() {
        if (this.simulator == null || !this.simulator.isRunning()) {
            logCb.accept("Simulator is not running");
            return;
        }
        if (!this.gracefulStopping.compareAndSet(false, true)) {
            logCb.accept("Graceful stop already in progress");
            return;
        }

        try {
            this.simulator.stopEntranceProcesses();
        } catch (Exception ex) {
            LOGGER.log(Level.WARNING, "Error requesting graceful stop", ex);
            logCb.accept("Error requesting graceful stop: " + ex.getMessage());
        }

        this.statusTextCb.accept("STOPPING (waiting vehicles...) ");
        this.statusColorCb.accept(new Color(200, 120, 0));

        this.executor.execute(() -> {
            try {
                while (true) {
                    boolean spritesEmpty;
                    synchronized (this.sprites) {
                        spritesEmpty = this.sprites.isEmpty();
                    }
                    boolean queueEmpty = (this.eventQueue == null) || this.eventQueue.isEmpty();
                    if (spritesEmpty && queueEmpty) {
                        break;
                    }
                    Thread.sleep(200);
                }
                SwingUtilities.invokeLater(() -> {
                    stopSimulation();
                    this.gracefulStopping.set(false);
                });
            } catch (InterruptedException ignored) {
                this.gracefulStopping.set(false);
                Thread.currentThread().interrupt();
            } catch (Exception ex) {
                this.gracefulStopping.set(false);
                LOGGER.log(Level.WARNING, "Graceful stop waiter failed", ex);
            }
        });
    }

    /**
     * Start a background task that consumes events from the simulator queue.
     */
    private void startEventConsumer() {
        stopEventConsumer();

        if (this.simulator == null || this.eventQueue == null) {
            logCb.accept("Cannot start event consumer: simulator or queue is null");
            return;
        }

        Callable<Void> consumerTask = () -> {
            try {
                while (this.simulator != null && this.simulator.isRunning()) {
                    Event ev = this.eventQueue.take();
                    handleEvent(ev);
                }
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            } catch (Exception ex) {
                LOGGER.log(Level.SEVERE, "Event consumer crashed", ex);
                logCb.accept("Event consumer crashed: " + ex.getMessage());
            }
            return null;
        };

        this.eventConsumerFuture = executor.submit(consumerTask);
    }

    /**
     * Stop and cancel the background event consumer task if running.
     */
    private void stopEventConsumer() {
        if (this.eventConsumerFuture != null && !this.eventConsumerFuture.isDone()) {
            this.eventConsumerFuture.cancel(true);
            this.eventConsumerFuture = null;
        }
    }

    /**
     * Schedule a one-shot auto-stop timer that stops the simulator after
     * a configured delay.
     */
    private void scheduleAutoStopTimer() {
        if (this.autoStopTimer != null && this.autoStopTimer.isRunning()) {
            this.autoStopTimer.stop();
        }
        int delay = (int) AUTO_STOP_MS;
        this.autoStopTimer = new javax.swing.Timer(delay, e -> {
            logCb.accept("Auto-stop: elapsed — requesting graceful stop.");
            stopSimulation();
        });
        this.autoStopTimer.setRepeats(false);
        this.autoStopTimer.start();
    }

    /**
     * Drain and discard all pending events from the simulator event queue.
     */
    private void clearEventQueue() {
        if (this.eventQueue == null)
            return;
        List<Event> drained = new ArrayList<>();
        this.eventQueue.drainTo(drained);
    }

    /**
     * Handle a single {@link Event} produced by the simulator.
     * <p>
     * The method distinguishes signal change events from vehicle events and
     * dispatches to the appropriate handler. Visual updates are scheduled on the
     * EDT via {@link SwingUtilities#invokeLater(Object)} where appropriate.
     * 
     * @param ev the event to process
     */
    private void handleEvent(Event ev) {
        logCb.accept(ev.toString());

        if (ev instanceof SignalChangeEvent) {
            handleSignalChange((SignalChangeEvent) ev);
            return;
        }

        VehicleEvent ve = (VehicleEvent) ev;
        Vehicle v = ve.getVehicle();

        EventType type = ve.getType();

        switch (type) {
            case NEW_VEHICLE:
                handleNewVehicle(ve, v);
                break;
            case VEHICLE_DEPARTURE:
                handleVehicleDeparture(ve, v);
                break;
            case VEHICLE_ROAD_ARRIVAL:
                handlePassRoad(ve, v);
                break;
            case VEHICLE_SIGNAL_ARRIVAL:
                handleVehicleSignalArrival(ve, v);
                break;
            case VEHICLE_EXIT:
                handleVehicleExit(ve, v);
                break;
            default:
                logCb.accept("Tipo de VehicleEvent não tratado: " + type);
        }

        SwingUtilities.invokeLater(this.renderer::repaint);
    }

    /**
     * Process a signal color change event.
     *
     * @param s the signal change event
     */
    private void handleSignalChange(SignalChangeEvent s) {
        RoadEnum road = s.getRoad();
        this.model.getTrafficLights().put(road, s.getSignalColor());
        this.model.compactQueue(road);
        SwingUtilities.invokeLater(() -> this.renderer.repaint());
    }

    /**
     * Create a new sprite for an entering vehicle and record entrance stats.
     *
     * @param ve the vehicle event
     * @param v  the vehicle instance
     */
    private void handleNewVehicle(VehicleEvent ve, Vehicle v) {
        Point p = this.nodePositions.get(ve.getNode());
        synchronized (this.sprites) {
            this.sprites.put(v.getId(), new VehicleSprite(v.getId(), v, p.x, p.y));
        }
        long ent = System.currentTimeMillis();
        this.stats.recordEntranceTimestamp(v.getId(), ent);
        this.stats.recordCreatedVehicle(v);
    }

    /**
     * Handle a vehicle departure from a node.
     * <p>
     * This records departure timestamps, computes and records wait
     * times (if any) and removes the sprite from visual queues.
     *
     * @param ve the vehicle event
     * @param v  the vehicle instance
     */
    private void handleVehicleDeparture(VehicleEvent ve, Vehicle v) {
        String id = v.getId();

        Long sigArr = this.stats.removeSignalArrival(id);
        if (sigArr != null) {
            long waitMs = System.currentTimeMillis() - sigArr;
            VehicleType vtype = v.getType();
            this.stats.recordWaitForType(vtype, waitMs);
        }

        this.stats.recordDepartureTimestamp(id);
        this.model.removeSpriteFromAllQueues(id);
        SwingUtilities.invokeLater(this.updateStatsCb);

        RoadEnum road = roadFromPrevToNode(v, ve.getNode());
        this.model.compactQueue(road);
    }

    /**
     * Handle a vehicle arriving at a traffic signal.
     * <p>
     * The handler records the signal arrival time, removes the vehicle
     * from the passing schedule for its previous road, records travel time
     * since departure, updates statistics, and enqueues the corresponding
     * sprite into the model's signal queue for the incoming road.
     *
     * @param ve the vehicle event
     * @param v  the vehicle instance
     */
    private void handleVehicleSignalArrival(VehicleEvent ve, Vehicle v) {
        String id = v.getId();
        this.stats.recordSignalArrival(id);

        RoadEnum removeRoad = roadFromPrevToNode(v, ve.getNode());
        Deque<AbstractMap.SimpleEntry<Long, String>> dq = this.passingSchedule.get(removeRoad);
        synchronized (dq) {
            Iterator<AbstractMap.SimpleEntry<Long, String>> it = dq.iterator();
            while (it.hasNext()) {
                AbstractMap.SimpleEntry<Long, String> e = it.next();
                if (id.equals(e.getValue())) {
                    it.remove();
                    break;
                }
            }
        }

        Long dep = this.stats.removeDepartureTimestamp(id);

        if (dep != null) {
            long dur = System.currentTimeMillis() - dep;
            this.stats.recordTravelTime(v, dur);
        }
        SwingUtilities.invokeLater(this.updateStatsCb);
        this.stats.recordPassedAtNode(ve.getNode(), v);

        RoadEnum incoming = roadFromPrevToNode(v, ve.getNode());

        synchronized (this.sprites) {
            VehicleSprite s = this.sprites.get(id);
            if (s != null) {
                this.model.enqueueToSignal(incoming, s);
            }
        }
    }

    /**
     * Handle vehicle exit events.
     * <p>
     * Marks the sprite for removal, records exit statistics and removes
     * any leftover timestamps and queue entries related to the vehicle.
     *
     * @param ve the vehicle event
     * @param v  the vehicle instance
     */
    private void handleVehicleExit(VehicleEvent ve, Vehicle v) {
        String id = v.getId();
        synchronized (sprites) {
            VehicleSprite s = sprites.get(id);
            s.markForRemoval();
        }
        this.stats.recordExitedVehicle(v);
        this.stats.recordTripTimeByType(v);
        this.stats.removeDepartureTimestamp(id);
        this.model.removeSpriteFromAllQueues(id);
        SwingUtilities.invokeLater(this.updateStatsCb);
    }

    /**
     * Update visual state when a vehicle passes a road segment.
     *
     * <p>
     * The method computes the destination traffic point in front of the
     * destination node (taking queueing into account), schedules the
     * sprite animation with a corrected completion time based on the
     * passing schedule and enqueues the sprite movement.
     *
     * @param ve the vehicle event describing the pass
     * @param v  the vehicle instance
     */
    private void handlePassRoad(VehicleEvent ve, Vehicle v) {
        String id = v.getId();
        VehicleSprite s;
        synchronized (this.sprites) {
            s = this.sprites.get(id);
        }

        RoadEnum road = roadFromPrevToNode(v, ve.getNode());

        Point dest = this.nodePositions.get(ve.getNode());
        Point origin = this.nodePositions.get(road.getOrigin());

        long baseTime = (road == null) ? 1000L : road.getTime();
        long passMs = (v.getType() == null) ? baseTime : v.getType().getTimeToPass(baseTime);
        long scheduledFinish = System.currentTimeMillis() + passMs;

        Deque<AbstractMap.SimpleEntry<Long, String>> dq = this.passingSchedule.get(road);
        synchronized (this.passingSchedule) {
            dq = this.passingSchedule.computeIfAbsent(road, r -> new ArrayDeque<>());
        }

        int posInSchedule;
        long corrected;
        synchronized (dq) {
            posInSchedule = dq.size();
            AbstractMap.SimpleEntry<Long, String> last = dq.peekLast();
            if (last != null && scheduledFinish < last.getKey()) {
                corrected = last.getKey() + PASS_DELAY_MS;
            } else {
                corrected = scheduledFinish;
            }
            dq.addLast(new AbstractMap.SimpleEntry<>(corrected, id));
        }

        java.util.Deque<VehicleSprite> q = model.getSignalQueues().get(road);
        int queued = (q == null) ? 0 : q.size();
        int queueIndex = queued + posInSchedule;

        Point signalPoint = MapModel.computeTrafficPoint(origin, dest, queueIndex);

        long anim = Math.max(200L, corrected - System.currentTimeMillis());
        s.setTarget(signalPoint.x, signalPoint.y, (int) anim);
    }

    /**
     * Compute the {@link RoadEnum} that connects a vehicle's previous node
     * to the given node.
     *
     * @param v    the vehicle whose route is consulted
     * @param node the target node
     * @return the corresponding RoadEnum
     */
    private RoadEnum roadFromPrevToNode(Vehicle v, NodeEnum node) {
        if (v == null || node == null)
            return null;
        NodeEnum prev = v.findPreviousNode(node);
        if (prev == null)
            return null;
        return RoadEnum.toRoadEnum(prev.toString() + "_" + node.toString());
    }

    /**
     * Shutdown the controller and underlying executor.
     * <p>
     * Attempts to stop the running simulator then forcibly shuts down
     * the executor service used for background tasks. After calling this
     * method the controller should not be used again.
     */
    public void shutdown() {
        try {
            stopSimulation();
        } finally {
            executor.shutdownNow();
        }
    }
}