package Launcher;

import Event.Event;
import Event.EventType;
import Event.SignalChangeEvent;
import Event.VehicleEvent;
import Node.NodeEnum;
import Node.NodeType;
import Node.RoadEnum;
import Vehicle.Vehicle;
import Vehicle.VehicleTypes;
import java.awt.*;
import java.util.*;
import java.util.concurrent.PriorityBlockingQueue;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

public class Dashboard extends JFrame {

    private Simulator simulator;
    private PriorityBlockingQueue<Event> eventQueue;
    private Thread eventConsumer;
    private Timer autoStopTimer;
    private volatile boolean gracefulStopping = false;

    private final Map<String, VehicleSprite> sprites = new HashMap<>();
    private final Map<NodeEnum, Point> nodePositions = new HashMap<>();
    private final Map<RoadEnum, String> trafficLights = new HashMap<>();
    private final Map<RoadEnum, Deque<VehicleSprite>> signalQueues = new EnumMap<>(RoadEnum.class);
    private final Map<RoadEnum, QueueStats> queueStats = new EnumMap<>(RoadEnum.class);

    private JTextArea logArea;
    private JLabel statusLabel;
    private JLabel statsActiveLabel;
    private JLabel statsCreatedLabel;
    private JLabel statsExitedLabel;
    private JLabel statsAvgTimeLabel;
    private JLabel statsCreatedByTypeLabel;
    private JLabel statsActiveByTypeLabel;
    private JLabel statsExitedByTypeLabel;

    private int totalCreated = 0;
    private int totalExited = 0;
    private long totalTravelTimeMs = 0L;
    private int completedTrips = 0;
    private final Map<String, Long> departTimestamps = new HashMap<>();
    private final Map<VehicleTypes, Integer> createdByType = new EnumMap<>(VehicleTypes.class);
    private final Map<VehicleTypes, Integer> exitedByType = new EnumMap<>(VehicleTypes.class);

    private DashboardRenderer renderer;

    public Dashboard() {
        super("Traffic Simulator Dashboard - Enhanced");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1050, 900);
        setLayout(new BorderLayout());

        initNodes();

        renderer = new DashboardRenderer(nodePositions, sprites, trafficLights, signalQueues, queueStats);
        add(renderer, BorderLayout.CENTER);

        JPanel top = new JPanel(new BorderLayout());
        top.setBackground(new Color(34, 40, 49));
        top.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));

        JButton startBtn = makeButton("Start");
        JButton stopBtn = makeButton("Stop");

        startBtn.addActionListener(e -> startSimulation());
        stopBtn.addActionListener(e -> requestGracefulStop());

        JPanel controls = new JPanel();
        controls.setOpaque(false);
        controls.add(startBtn);
        controls.add(stopBtn);

        top.add(controls, BorderLayout.WEST);

        statusLabel = new JLabel("STOPPED");
        statusLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        statusLabel.setForeground(Color.RED);
        top.add(statusLabel, BorderLayout.EAST);

        add(top, BorderLayout.NORTH);

        logArea = new JTextArea(10, 50);
        logArea.setEditable(false);
        logArea.setFont(new Font("Consolas", Font.PLAIN, 13));
        add(new JScrollPane(logArea), BorderLayout.SOUTH);

        new Timer(80, e -> {
            boolean changed = false;
            synchronized (sprites) {
                for (Iterator<Map.Entry<String, VehicleSprite>> it = sprites.entrySet().iterator(); it.hasNext(); ) {
                    VehicleSprite s = it.next().getValue();
                    if (s.updatePosition()) changed = true;
                    if (s.shouldRemoveNow()) {
                        it.remove();
                        changed = true;
                    }
                }
            }
            if (changed) renderer.repaint();
        }).start();

        JPanel statsPanel = new JPanel();
        statsPanel.setLayout(new BoxLayout(statsPanel, BoxLayout.Y_AXIS));
        statsPanel.setBackground(new Color(250, 250, 250));
        statsPanel.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));

        JLabel title = new JLabel("Statistics");
        title.setFont(new Font("Segoe UI", Font.BOLD, 14));
        statsPanel.add(title);
        statsPanel.add(Box.createVerticalStrut(8));

        statsCreatedLabel = new JLabel("Created: 0");
        statsActiveLabel = new JLabel("Active: 0");
        statsExitedLabel = new JLabel("Exited: 0");
        statsAvgTimeLabel = new JLabel("Avg trip (s): 0.0");

        statsCreatedByTypeLabel = new JLabel("Created by type: ");
        statsActiveByTypeLabel = new JLabel("Active by type: ");
        statsExitedByTypeLabel = new JLabel("Exited by type: ");

        statsPanel.add(statsCreatedLabel);
        statsPanel.add(statsActiveLabel);
        statsPanel.add(statsExitedLabel);
        statsPanel.add(statsAvgTimeLabel);
        statsPanel.add(Box.createVerticalStrut(6));
        statsPanel.add(statsCreatedByTypeLabel);
        statsPanel.add(statsActiveByTypeLabel);
        statsPanel.add(statsExitedByTypeLabel);
        // (crossroad stats area removed)

        add(statsPanel, BorderLayout.EAST);
    }

    private void requestGracefulStop() {
        if (simulator == null || !simulator.isRunning()) {
            log("Simulator is not running");
            return;
        }
        if (gracefulStopping) {
            log("Graceful stop already in progress");
            return;
        }

        gracefulStopping = true;

        try {
            simulator.stopEntranceProcesses();
        } catch (Exception ex) {
            log("Error requesting graceful stop: " + ex.getMessage());
        }

        statusLabel.setText("STOPPING (waiting vehicles...) ");
        statusLabel.setForeground(new Color(200, 120, 0));

        Thread waiter = new Thread(() -> {
            try {
                while (true) {
                    boolean spritesEmpty;
                    synchronized (sprites) { spritesEmpty = sprites.isEmpty(); }
                    boolean queueEmpty = (eventQueue == null) || eventQueue.isEmpty();
                    if (spritesEmpty && queueEmpty) break;
                    Thread.sleep(200);
                }
                SwingUtilities.invokeLater(() -> {
                    stopSimulation();
                    gracefulStopping = false;
                });
            } catch (InterruptedException ignored) {
                gracefulStopping = false;
            }
        });
        waiter.setDaemon(true);
        waiter.start();
    }

    private JButton makeButton(String text) {
        JButton b = new JButton(text);
        b.setFont(new Font("Segoe UI", Font.BOLD, 14));
        b.setBackground(new Color(48, 71, 94));
        b.setForeground(Color.WHITE);
        b.setFocusable(false);
        b.setBorder(BorderFactory.createEmptyBorder(6, 14, 6, 14));
        return b;
    }

    private void initNodes() {
    int cellW = 300;
    int cellH = 200;

    Map<NodeEnum, Point> grid = new LinkedHashMap<>();

    grid.put(NodeEnum.E1, new Point(0 * cellW + cellW/2, 0 * cellH + cellH/2));
    grid.put(NodeEnum.E2, new Point(0 * cellW + cellW/2, 1 * cellH + cellH/2));
    grid.put(NodeEnum.E3, new Point(0 * cellW + cellW/2, 2 * cellH + cellH/2));

    grid.put(NodeEnum.CR1, new Point(1 * cellW + cellW/2, 0 * cellH + cellH/2));
    grid.put(NodeEnum.CR2, new Point(1 * cellW + cellW/2, 1 * cellH + cellH/2));
    grid.put(NodeEnum.CR3, new Point(1 * cellW + cellW/2, 2 * cellH + cellH/2));

    grid.put(NodeEnum.CR4, new Point(2 * cellW + cellW/2, 0 * cellH + cellH/2));
    grid.put(NodeEnum.CR5, new Point(2 * cellW + cellW/2, 1 * cellH + cellH/2));
    grid.put(NodeEnum.S,   new Point(2 * cellW + cellW/2, 2 * cellH + cellH/2));

    nodePositions.clear();
    nodePositions.putAll(grid);

    trafficLights.clear();
    signalQueues.clear();
    queueStats.clear();
    // initialize per-type counters
    createdByType.clear();
    exitedByType.clear();
    for (VehicleTypes vt : VehicleTypes.values()) {
        createdByType.put(vt, 0);
        exitedByType.put(vt, 0);
    }
    // initialize per-crossroad counters
    // (per-crossroad counters removed)
    for (RoadEnum r : RoadEnum.values()) {
        if (r.getDestination().getType() == NodeType.CROSSROAD) {
            trafficLights.put(r, "RED");
            signalQueues.put(r, new ArrayDeque<>());
            queueStats.put(r, new QueueStats());
        }
    }
}

    private void startSimulation() {
        if (simulator != null && simulator.isRunning()) {
            log("Simulator already running");
            return;
        }

        simulator = new Simulator();
        eventQueue = simulator.getEventQueue();

        Thread simThread = new Thread(simulator::startSimulation);
        simThread.setDaemon(true);
        simThread.start();

        statusLabel.setText("RUNNING");
        statusLabel.setForeground(Color.GREEN);

        eventConsumer = new Thread(() -> {
            try {
                while (simulator != null && simulator.isRunning()) {
                    Event ev = eventQueue.take();
                    handleEvent(ev);
                }
            } catch (InterruptedException ignored) {
            } catch (Exception ex) {
                log("Event consumer crashed: " + ex.getMessage());
            }
        });
        eventConsumer.setDaemon(true);
        eventConsumer.start();

        log("Simulator started");

        if (autoStopTimer != null && autoStopTimer.isRunning()) {
            autoStopTimer.stop();
        }
        autoStopTimer = new Timer(60_000, e -> {
            log("Auto-stop: 60 seconds elapsed — requesting graceful stop.");
            requestGracefulStop();
        });
        autoStopTimer.setRepeats(false);
        autoStopTimer.start();
    }

    private void stopSimulation() {
        if (simulator != null) simulator.stopSimulation();
        if (eventConsumer != null) eventConsumer.interrupt();

        if (autoStopTimer != null) {
            autoStopTimer.stop();
            autoStopTimer = null;
        }

        statusLabel.setText("STOPPED");
        statusLabel.setForeground(Color.RED);

        if (eventQueue != null) {
            while ((eventQueue.poll()) != null) {
            }
        }

        synchronized (sprites) {
            sprites.clear();
        }

        renderer.repaint();
        log("Simulator stopped");
    }

    private void handleEvent(Event ev) {
        if (ev == null) return;
        log(ev.toString());

        if (ev instanceof SignalChangeEvent) {
            SignalChangeEvent s = (SignalChangeEvent) ev;
            if (s.getRoad() != null) {
                trafficLights.put(s.getRoad(), s.getSignalColor());
            } else {
                for (RoadEnum r : RoadEnum.getRoadsToCrossroad(s.getNode())) {
                    trafficLights.put(r, s.getSignalColor());
                }
            }
            SwingUtilities.invokeLater(renderer::repaint);
            return;
        }

        if (!(ev instanceof VehicleEvent)) {
            log("Evento não processado pelo Dashboard: " + ev.getClass().getSimpleName());
            return;
        }
        VehicleEvent ve = (VehicleEvent) ev;

        Vehicle v = ve.getVehicle();
        if (v == null) {
            log("VehicleEvent sem veículo associado");
            return;
        }
        String id = v.getId();

        EventType type = ve.getType();
        if (type == null) {
            log("VehicleEvent sem tipo definido");
            return;
        }

        switch (type) {
            case NEW_VEHICLE: {
                Point p = nodePositions.get(ve.getNode());
                if (p == null) {
                    p = new Point(renderer.getWidth() / 2, renderer.getHeight() / 2);
                    nodePositions.put(ve.getNode(), p);
                }
                synchronized (sprites) {
                    if (!sprites.containsKey(id)) {
                        sprites.put(id, new VehicleSprite(id, v, p.x, p.y));
                    } else {
                        VehicleSprite s = sprites.get(id);
                        s.x = p.x; s.y = p.y;
                    }
                }
                synchronized (this) {
                    totalCreated++;
                    VehicleTypes vt = v.getType();
                    if (vt != null) createdByType.put(vt, createdByType.getOrDefault(vt, 0) + 1);
                }
                updateStatsLabelsAsync();
                break;
            }

            case VEHICLE_DEPARTURE: {
                handleDeparture(ve, v);
                synchronized (departTimestamps) {
                    departTimestamps.put(id, System.currentTimeMillis());
                }
                // remove sprite from visual queue for the incoming road (vehicle leaving intersection)
                NodeEnum incomingPrev = findPreviousNode(v, ve.getNode());
                if (incomingPrev != null) {
                    RoadEnum incomingRoad = RoadEnum.toRoadEnum(incomingPrev.toString() + "_" + ve.getNode().toString());
                    if (incomingRoad != null) {
                        Deque<VehicleSprite> q = signalQueues.get(incomingRoad);
                        QueueStats st = queueStats.get(incomingRoad);
                        if (q != null) {
                            synchronized (q) {
                                q.removeIf(sp -> sp.id.equals(id));
                                if (st != null) st.recordSample(q.size());
                            }
                        }
                    }
                }
                updateStatsLabelsAsync();
                break;
            }

            case VEHICLE_ROAD_ARRIVAL: {
                // Vehicle entered the road towards ve.getNode()
                synchronized (sprites) {
                    VehicleSprite s = sprites.get(id);
                    // ensure sprite exists; do not override an ongoing animation
                    if (s == null) {
                        // place at origin node (previous node in path) if available
                        NodeEnum prev = findPreviousNode(v, ve.getNode());
                        Point origin = (prev == null) ? nodePositions.get(ve.getNode()) : nodePositions.get(prev);
                        if (origin == null) origin = new Point(renderer.getWidth() / 2, renderer.getHeight() / 2);
                        s = new VehicleSprite(id, v, origin.x, origin.y);
                        sprites.put(id, s);

                        // animate part-way along the road towards destination
                        NodeEnum destNode = ve.getNode();
                        Point dest = nodePositions.get(destNode);
                        if (dest != null) {
                            Point mid = pointAlong(origin, dest, 0.45);
                            RoadEnum road = RoadEnum.toRoadEnum((prev == null ? ve.getNode().toString() : prev.toString()) + "_" + destNode.toString());
                            long base = (road == null) ? 800 : v.getType().getTimeToPass(road.getTime());
                            long anim = Math.max(200, (long) (base * 1.2));
                            s.setTarget(mid.x, mid.y, anim);
                        }
                    }
                }
                break;
            }

            case VEHICLE_SIGNAL_ARRIVAL: {
                // Vehicle arrived at the semaphore for ve.getNode()
                synchronized (sprites) {
                    VehicleSprite s = sprites.get(id);
                    if (s == null) {
                        // create sprite at node center if missing
                        Point p = nodePositions.get(ve.getNode());
                        if (p == null) p = new Point(renderer.getWidth() / 2, renderer.getHeight() / 2);
                        s = new VehicleSprite(id, v, p.x, p.y);
                        sprites.put(id, s);
                    }

                    // compute semaphore (signal) position at node boundary and animate briefly to it
                    NodeEnum prev = findPreviousNode(v, ve.getNode());
                    Point origin = (prev == null) ? nodePositions.get(ve.getNode()) : nodePositions.get(prev);
                    Point dest = nodePositions.get(ve.getNode());
                    if (origin != null && dest != null) {
                        Point signal = computeSignalPoint(origin, dest);
                        s.setTarget(signal.x, signal.y, 120);
                    }
                }

                // arriving at a signal typically finalizes a trip segment; check travel times
                Long dep;
                synchronized (departTimestamps) {
                    dep = departTimestamps.remove(id);
                }
                if (dep != null) {
                    long dur = System.currentTimeMillis() - dep;
                    synchronized (this) {
                        totalTravelTimeMs += dur;
                        completedTrips++;
                    }
                    updateStatsLabelsAsync();
                }

                // add sprite to visual queue for the incoming road
                NodeEnum prevNode = findPreviousNode(v, ve.getNode());
                if (prevNode != null) {
                    RoadEnum incoming = RoadEnum.toRoadEnum(prevNode.toString() + "_" + ve.getNode().toString());
                    if (incoming != null) {
                        Deque<VehicleSprite> q = signalQueues.get(incoming);
                        QueueStats st = queueStats.get(incoming);
                        if (q != null) {
                            synchronized (q) {
                                VehicleSprite sprite = sprites.get(id);
                                if (sprite != null && q.stream().noneMatch(sp -> sp.id.equals(id))) {
                                    q.addLast(sprite);
                                }
                                if (st != null) st.recordSample(q.size());
                            }
                        }
                    }
                }

                // (per-crossroad counting removed)

                break;
            }

            case VEHICLE_EXIT: {
                synchronized (sprites) {
                    VehicleSprite s = sprites.get(id);
                    if (s != null) s.markForRemoval();
                    else {
                    }
                }
                synchronized (this) {
                    totalExited++;
                    VehicleTypes vt = v.getType();
                    if (vt != null) exitedByType.put(vt, exitedByType.getOrDefault(vt, 0) + 1);
                }
                synchronized (departTimestamps) {
                    departTimestamps.remove(id);
                }
                // ensure vehicle removed from any semaphore queues and update stats
                for (Map.Entry<RoadEnum, Deque<VehicleSprite>> e : signalQueues.entrySet()) {
                    Deque<VehicleSprite> q = e.getValue();
                    QueueStats st = queueStats.get(e.getKey());
                    if (q != null) {
                        synchronized (q) {
                            q.removeIf(sp -> sp.id.equals(id));
                            if (st != null) st.recordSample(q.size());
                        }
                    }
                }
                updateStatsLabelsAsync();
                break;
            }

            default: {
                log("Tipo de VehicleEvent não tratado: " + type);
                break;
            }
        }

        SwingUtilities.invokeLater(renderer::repaint);
    }

    private void updateStatsLabelsAsync() {
        SwingUtilities.invokeLater(this::updateStatsLabels);
    }

    private void updateStatsLabels() {
        int active;
        synchronized (sprites) { active = sprites.size(); }

        int created;
        int exited;
        long travelMs;
        int trips;
        synchronized (this) {
            created = totalCreated;
            exited = totalExited;
            travelMs = totalTravelTimeMs;
            trips = completedTrips;
        }

        double avgSec = (trips == 0) ? 0.0 : (travelMs / 1000.0 / trips);

        if (statsActiveLabel != null) statsActiveLabel.setText("Active: " + active);
        if (statsCreatedLabel != null) statsCreatedLabel.setText("Created: " + created);
        if (statsExitedLabel != null) statsExitedLabel.setText("Exited: " + exited);
        if (statsAvgTimeLabel != null) statsAvgTimeLabel.setText(String.format("Avg trip (s): %.2f", avgSec));

        // per-type stats
        StringBuilder createdBy = new StringBuilder();
        StringBuilder activeBy = new StringBuilder();
        StringBuilder exitedBy = new StringBuilder();

        // created and exited come from maps; active derived from sprites
        for (VehicleTypes vt : VehicleTypes.values()) {
            int c = createdByType.getOrDefault(vt, 0);
            int x = exitedByType.getOrDefault(vt, 0);
            createdBy.append(vt.getTypeToString()).append("=").append(c).append(" ");
            exitedBy.append(vt.getTypeToString()).append("=").append(x).append(" ");
        }

        // compute active per type
        Map<VehicleTypes, Integer> activeMap = new EnumMap<>(VehicleTypes.class);
        synchronized (sprites) {
            for (VehicleSprite vs : sprites.values()) {
                VehicleTypes vt = vs.vehicle == null ? null : vs.vehicle.getType();
                if (vt == null) continue;
                activeMap.put(vt, activeMap.getOrDefault(vt, 0) + 1);
            }
        }
        for (VehicleTypes vt : VehicleTypes.values()) {
            int a = activeMap.getOrDefault(vt, 0);
            activeBy.append(vt.getTypeToString()).append("=").append(a).append(" ");
        }

        if (statsCreatedByTypeLabel != null) statsCreatedByTypeLabel.setText("Created by type: " + createdBy.toString().trim());
        if (statsActiveByTypeLabel != null) statsActiveByTypeLabel.setText("Active by type: " + activeBy.toString().trim());
        if (statsExitedByTypeLabel != null) statsExitedByTypeLabel.setText("Exited by type: " + exitedBy.toString().trim());

        // (per-crossroad stats removed)
    }

    private void handleDeparture(VehicleEvent ve, Vehicle v) {
        String id = v.getId();
        synchronized (sprites) {
            VehicleSprite s = sprites.get(id);

            if (s == null) {
                Point originPos = nodePositions.get(ve.getNode());
                if (originPos == null) {
                    originPos = new Point(renderer.getWidth() / 2, renderer.getHeight() / 2);
                    nodePositions.put(ve.getNode(), originPos);
                }
                s = new VehicleSprite(id, v, originPos.x, originPos.y);
                sprites.put(id, s);
            }

            NodeEnum next = findNextNode(v, ve.getNode());
            if (next == null) {
                s.markForRemoval();
                return;
            }

            RoadEnum road = RoadEnum.toRoadEnum(ve.getNode().toString() + "_" + next.toString());
            if (road == null) {
                Point destP = nodePositions.get(next);
                if (destP != null) {
                    s.setTarget(destP.x, destP.y, 500);
                } else {
                    s.markForRemoval();
                }
                return;
            }

            Point dest = nodePositions.get(next);
            if (dest == null) {
                dest = new Point(renderer.getWidth()/2, renderer.getHeight()/2);
                nodePositions.put(next, dest);
            }

            long base = v.getType().getTimeToPass(road.getTime());
            long anim = (long) (base * 2.5);
            s.setTarget(dest.x, dest.y, anim);
        }
    }

    private NodeEnum findNextNode(Vehicle v, NodeEnum current) {
        if (v.getPath() == null) return null;

        java.util.List<NodeEnum> list = v.getPath().getPath();
        for (int i = 0; i < list.size() - 1; i++)
            if (list.get(i) == current)
                return list.get(i + 1);

        return null;
    }

    private NodeEnum findPreviousNode(Vehicle v, NodeEnum current) {
        if (v.getPath() == null) return null;

        java.util.List<NodeEnum> list = v.getPath().getPath();
        for (int i = 1; i < list.size(); i++)
            if (list.get(i) == current)
                return list.get(i - 1);

        return null;
    }

    // point at fraction t along the line origin->dest (0..1)
    private Point pointAlong(Point origin, Point dest, double t) {
        double x = origin.x + (dest.x - origin.x) * t;
        double y = origin.y + (dest.y - origin.y) * t;
        return new Point((int) Math.round(x), (int) Math.round(y));
    }

    // compute approximate semaphore point on the destination node rectangle
    private Point computeSignalPoint(Point origin, Point dest) {
        double dx = dest.x - origin.x;
        double dy = dest.y - origin.y;
        double len = Math.hypot(dx, dy); if (len == 0) len = 1;
        double ux = dx / len; double uy = dy / len;

        double hw = 44.0, hh = 28.0;
        double tx = (Math.abs(ux) < 1e-6) ? Double.POSITIVE_INFINITY : (hw / Math.abs(ux));
        double ty = (Math.abs(uy) < 1e-6) ? Double.POSITIVE_INFINITY : (hh / Math.abs(uy));
        double t = Math.min(tx, ty);
        double bx = dest.x - ux * t;
        double by = dest.y - uy * t;
        return new Point((int)Math.round(bx), (int)Math.round(by));
    }

    private void log(String s) {
        SwingUtilities.invokeLater(() -> {
            if (logArea != null) logArea.append(s + "\n");
        });
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new Dashboard().setVisible(true));
    }
}
