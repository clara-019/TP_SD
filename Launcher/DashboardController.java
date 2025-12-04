package Launcher;

import Event.Event;
import Event.SignalChangeEvent;
import Event.VehicleEvent;
import Event.EventType;
import Node.NodeEnum;
import Traffic.RoadEnum;
import Vehicle.Vehicle;
import Vehicle.VehicleType;

import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

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

    public DashboardController(MapModel model, Map<String, VehicleSprite> sprites, Map<NodeEnum, Point> nodePositions,
            MapRenderer renderer, Consumer<String> logCb, Runnable updateStatsCb, Consumer<String> statusTextCb,
            Consumer<Color> statusColorCb) {
        this.model = model;
        this.sprites = sprites;
        this.nodePositions = nodePositions;
        this.renderer = renderer;

        this.logCb = logCb;
        this.updateStatsCb = updateStatsCb;
        this.statusTextCb = statusTextCb;
        this.statusColorCb = statusColorCb;

        for (RoadEnum r : RoadEnum.values()) {
            this.passingSchedule.put(r, new ArrayDeque<>());
        }
    }

    public Statistics getStatistics() {
        return stats;
    }

    public synchronized void startSimulation() {
        if (this.simulator != null && this.simulator.isRunning()) {
            logCb.accept("Simulator already running");
            return;
        }

        if (this.renderer != null) {
            this.renderer.revalidate();
            this.renderer.repaint();
        }

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

        if (this.renderer != null) {
            SwingUtilities.invokeLater(this.renderer::repaint);
        }

        logCb.accept("Simulator stopped");
    }

    public void requestGracefulStop() {
        if (simulator == null || !simulator.isRunning()) {
            logCb.accept("Simulator is not running");
            return;
        }
        if (!gracefulStopping.compareAndSet(false, true)) {
            logCb.accept("Graceful stop already in progress");
            return;
        }

        try {
            simulator.stopEntranceProcesses();
        } catch (Exception ex) {
            LOGGER.log(Level.WARNING, "Error requesting graceful stop", ex);
            logCb.accept("Error requesting graceful stop: " + ex.getMessage());
        }

        statusTextCb.accept("STOPPING (waiting vehicles...) ");
        statusColorCb.accept(new Color(200, 120, 0));

        executor.execute(() -> {
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
                    gracefulStopping.set(false);
                });
            } catch (InterruptedException ignored) {
                gracefulStopping.set(false);
                Thread.currentThread().interrupt();
            } catch (Exception ex) {
                gracefulStopping.set(false);
                LOGGER.log(Level.WARNING, "Graceful stop waiter failed", ex);
            }
        });
    }

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

    private void stopEventConsumer() {
        if (this.eventConsumerFuture != null && !this.eventConsumerFuture.isDone()) {
            this.eventConsumerFuture.cancel(true);
            this.eventConsumerFuture = null;
        }
    }

    private void scheduleAutoStopTimer() {
        if (this.autoStopTimer != null && this.autoStopTimer.isRunning()) {
            this.autoStopTimer.stop();
        }
        int delay = (int) AUTO_STOP_MS;
        this.autoStopTimer = new javax.swing.Timer(delay, e -> {
            logCb.accept("Auto-stop: elapsed — requesting graceful stop.");
            requestGracefulStop();
        });
        this.autoStopTimer.setRepeats(false);
        this.autoStopTimer.start();
    }

    private void clearEventQueue() {
        if (this.eventQueue == null)
            return;
        List<Event> drained = new ArrayList<>();
        this.eventQueue.drainTo(drained);
    }

    private void handleEvent(Event ev) {
        if (ev == null)
            return;

        logCb.accept(ev.toString());

        if (ev instanceof SignalChangeEvent) {
            handleSignalChange((SignalChangeEvent) ev);
            return;
        }

        if (!(ev instanceof VehicleEvent)) {
            logCb.accept("Evento não processado pelo DashboardController: " + ev.getClass().getSimpleName());
            return;
        }

        VehicleEvent ve = (VehicleEvent) ev;
        Vehicle v = ve.getVehicle();

        if (v == null) {
            logCb.accept("VehicleEvent sem veículo associado");
            return;
        }

        EventType type = ve.getType();
        if (type == null) {
            logCb.accept("VehicleEvent sem tipo definido");
            return;
        }

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

        SwingUtilities.invokeLater(renderer::repaint);
    }

    private void handleSignalChange(SignalChangeEvent s) {
        RoadEnum road = s.getRoad();
        this.model.getTrafficLights().put(road, s.getSignalColor());
        this.model.compactQueue(road);
        SwingUtilities.invokeLater(() -> this.renderer.repaint());
    }

    private void handleNewVehicle(VehicleEvent ve, Vehicle v) {
        Point p = this.nodePositions.get(ve.getNode());
        if (p == null) {
            logCb.accept("Nova vehicle sem posição de nó: " + ve.getNode());
            return;
        }
        synchronized (this.sprites) {
            this.sprites.put(v.getId(), new VehicleSprite(v.getId(), v, p.x, p.y));
        }
        long ent = (v.getEntranceTime() > 0) ? v.getEntranceTime() : System.currentTimeMillis();
        this.stats.recordEntranceTimestamp(v.getId(), ent);
        this.stats.recordCreatedVehicle(v);
    }

    private void handleVehicleDeparture(VehicleEvent ve, Vehicle v) {
        String id = v.getId();

        Long sigArr = this.stats.removeSignalArrival(id);
        if (sigArr != null) {
            long waitMs = System.currentTimeMillis() - sigArr;
            VehicleType vtype = v.getType();
            if (vtype != null) {
                this.stats.recordWaitForType(vtype, waitMs);
            }
        }

        this.stats.recordDepartureTimestamp(id);
        this.model.removeSpriteFromAllQueues(id);
        SwingUtilities.invokeLater(this.updateStatsCb);

        Point nodePoint = this.nodePositions.get(ve.getNode());
        if (nodePoint != null) {
            synchronized (this.sprites) {
                VehicleSprite s = this.sprites.get(id);
                if (s != null) {
                    s.setTarget(nodePoint.x, nodePoint.y, 10);
                }
            }
        }

        RoadEnum road = roadFromPrevToNode(v, ve.getNode());
        this.model.compactQueue(road);
    }

    private void handleVehicleSignalArrival(VehicleEvent ve, Vehicle v) {
        String id = v.getId();
        this.stats.recordSignalArrival(id);

        RoadEnum removeRoad = roadFromPrevToNode(v, ve.getNode());
        if (removeRoad != null) {
            Deque<AbstractMap.SimpleEntry<Long, String>> dq = this.passingSchedule.get(removeRoad);
            if (dq != null) {
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
            }
        }

        Long dep = this.stats.removeDepartureTimestamp(id);
        if (dep != null) {
            long dur = System.currentTimeMillis() - dep;
            this.stats.recordTravelTime(v, dur);
            SwingUtilities.invokeLater(this.updateStatsCb);
        }

        this.stats.recordPassedAtNode(ve.getNode(), v);

        RoadEnum incoming = roadFromPrevToNode(v, ve.getNode());
        if (incoming == null) {
            logCb.accept("Warning: cannot determine incoming road for vehicle " + id + " to node " + ve.getNode());
            return;
        }

        synchronized (this.sprites) {
            VehicleSprite s = this.sprites.get(id);
            if (s != null) {
                this.model.enqueueToSignal(incoming, s);
            }
        }
    }

    private void handleVehicleExit(VehicleEvent ve, Vehicle v) {
        String id = v.getId();
        synchronized (sprites) {
            VehicleSprite s = sprites.get(id);
            if (s != null) {
                s.markForRemoval();
            }
        }
        this.stats.recordExitedVehicle(v);
        this.stats.recordTripTimeByType(v);
        this.stats.removeDepartureTimestamp(id);
        this.model.removeSpriteFromAllQueues(id);
        SwingUtilities.invokeLater(this.updateStatsCb);
    }

    private void handlePassRoad(VehicleEvent ve, Vehicle v) {
        if (v == null || ve == null)
            return;
        String id = v.getId();

        VehicleSprite s;
        synchronized (this.sprites) {
            s = this.sprites.get(id);
        }
        if (s == null) {
            logCb.accept("Warning: sprite for " + id + " not found in handlePassRoad; skipping visual update");
            return;
        }

        RoadEnum road = roadFromPrevToNode(v, ve.getNode());
        if (road == null) {
            logCb.accept("Warning: cannot determine road for vehicle " + id + " in handlePassRoad");
            return;
        }

        Point dest = this.nodePositions.get(ve.getNode());
        Point origin = this.nodePositions.get(road.getOrigin());
        if (dest == null || origin == null) {
            logCb.accept("Warning: origin/destination node position missing for node " + ve.getNode());
            return;
        }

        long baseTime = (road == null) ? 1000L : road.getTime();
        long passMs = (v.getType() == null) ? baseTime : v.getType().getTimeToPass(baseTime);
        long scheduledFinish = System.currentTimeMillis() + passMs;

        Deque<AbstractMap.SimpleEntry<Long, String>> dq = this.passingSchedule.get(road);
        if (dq == null) {
            synchronized (this.passingSchedule) {
                dq = this.passingSchedule.computeIfAbsent(road, r -> new ArrayDeque<>());
            }
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
        s.clearFaceTarget();
        s.setTarget(signalPoint.x, signalPoint.y, (int) anim);
    }

    private RoadEnum roadFromPrevToNode(Vehicle v, NodeEnum node) {
        if (v == null || node == null)
            return null;
        NodeEnum prev = v.findPreviousNode(node);
        if (prev == null)
            return null;
        return RoadEnum.toRoadEnum(prev.toString() + "_" + node.toString());
    }

    public void shutdown() {
        try {
            stopSimulation();
        } finally {
            executor.shutdownNow();
        }
    }
}
