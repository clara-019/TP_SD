package Launcher;

import Event.Event;
import Event.EventType;
import Event.SignalChangeEvent;
import Event.VehicleEvent;
import Node.NodeEnum;
import Node.NodeType;
import Traffic.RoadEnum;
import Vehicle.Vehicle;
import Vehicle.VehicleType;
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

    private final Map<String, VehicleSprite> sprites;
    private final Map<NodeEnum, Point> nodePositions;
    private final DashboardModel model;

    private final Statistics stats = new Statistics();
    private javax.swing.JTextArea statsPerCrossroadArea;
    private static final long PASS_DELAY_MS = 200L;
    private final Map<RoadEnum, Deque<java.util.AbstractMap.SimpleEntry<Long, String>>> passingSchedule = new EnumMap<>(
            RoadEnum.class);

    private JTextArea logArea;
    private JLabel statusLabel;
    private JLabel statsActiveLabel;
    private JLabel statsCreatedLabel;
    private JLabel statsExitedLabel;
    private JLabel statsAvgTimeLabel;
    private JLabel statsCreatedByTypeLabel;
    private JLabel statsActiveByTypeLabel;
    private JLabel statsExitedByTypeLabel;
    private JLabel statsAvgWaitByTypeLabel;
    private JLabel statsAvgRoadByTypeLabel;
    private JLabel statsTripByTypeLabel;

    private DashboardRenderer renderer;

    public Dashboard() {
        super("Traffic Simulator Dashboard - Organized Layout");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1400, 950);
        setLayout(new BorderLayout());

        this.model = new DashboardModel();

        this.sprites = model.getSprites();
        this.nodePositions = model.getNodePositions();
        // initialize per-road UI schedule to keep animation order consistent
        for (RoadEnum r : RoadEnum.values()) {
            this.passingSchedule.put(r, new ArrayDeque<>());
        }

        JPanel top = new JPanel(new BorderLayout());
        top.setBackground(new Color(34, 40, 49));
        top.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        JButton startBtn = makeButton("Start");
        JButton stopBtn = makeButton("Stop");

        startBtn.addActionListener(e -> startSimulation());
        stopBtn.addActionListener(e -> requestGracefulStop());

        JPanel controls = new JPanel();
        controls.setOpaque(false);
        controls.add(startBtn);
        controls.add(stopBtn);

        top.add(controls, BorderLayout.WEST);

        this.statusLabel = new JLabel("STOPPED");
        this.statusLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        this.statusLabel.setForeground(Color.RED);
        top.add(this.statusLabel, BorderLayout.EAST);

        add(top, BorderLayout.NORTH);

        JPanel centerContainer = new JPanel(new BorderLayout());

        this.renderer = new DashboardRenderer(this.model);
        centerContainer.add(this.renderer, BorderLayout.CENTER);

        JPanel rightPanel = new JPanel(new BorderLayout());
        rightPanel.setBackground(new Color(245, 245, 245));
        rightPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(180, 180, 180), 1),
                BorderFactory.createEmptyBorder(8, 8, 8, 8)));

        JLabel logsTitle = new JLabel("Logs");
        logsTitle.setFont(new Font("Segoe UI", Font.BOLD, 12));
        rightPanel.add(logsTitle, BorderLayout.NORTH);

        this.logArea = new JTextArea(15, 30);
        this.logArea.setEditable(false);
        this.logArea.setFont(new Font("Consolas", Font.PLAIN, 11));
        this.logArea.setLineWrap(true);
        this.logArea.setWrapStyleWord(true);
        JScrollPane logScroll = new JScrollPane(this.logArea);
        logScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        rightPanel.add(logScroll, BorderLayout.CENTER);

        centerContainer.add(rightPanel, BorderLayout.EAST);
        add(centerContainer, BorderLayout.CENTER);

        // BOTTOM PANEL: Statistics (organized sections) - wrapped in scrollable
        // container
        JPanel statsContainerPanel = new JPanel();
        statsContainerPanel.setLayout(new BoxLayout(statsContainerPanel, BoxLayout.X_AXIS));
        statsContainerPanel.setBackground(new Color(240, 240, 240));
        statsContainerPanel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        // Section 1: Overall Stats
        JPanel overallStatsPanel = createStatSection("Overall Statistics");
        statsCreatedLabel = new JLabel("Created: 0");
        statsActiveLabel = new JLabel("Active: 0");
        statsExitedLabel = new JLabel("Exited: 0");
        statsAvgTimeLabel = new JLabel("Avg Trip: 0.0s");
        overallStatsPanel.add(statsCreatedLabel);
        overallStatsPanel.add(Box.createVerticalStrut(4));
        overallStatsPanel.add(statsActiveLabel);
        overallStatsPanel.add(Box.createVerticalStrut(4));
        overallStatsPanel.add(statsExitedLabel);
        overallStatsPanel.add(Box.createVerticalStrut(4));
        overallStatsPanel.add(statsAvgTimeLabel);
        overallStatsPanel.setMaximumSize(new Dimension(180, 200));
        statsContainerPanel.add(overallStatsPanel);
        statsContainerPanel.add(Box.createHorizontalStrut(10));

        // Section 2: Per-Type Stats (with scroll if needed)
        JPanel typeStatsPanel = createStatSection("By Vehicle Type");
        statsCreatedByTypeLabel = new JLabel("<html>Created: -</html>");
        statsActiveByTypeLabel = new JLabel("<html>Active: -</html>");
        statsExitedByTypeLabel = new JLabel("<html>Exited: -</html>");
        statsAvgWaitByTypeLabel = new JLabel("<html>Avg Wait: -</html>");
        statsAvgRoadByTypeLabel = new JLabel("<html>Avg Road: -</html>");
        statsTripByTypeLabel = new JLabel("<html>Trip (min/avg/max): -</html>");

        JPanel typeStatsContent = new JPanel();
        typeStatsContent.setLayout(new BoxLayout(typeStatsContent, BoxLayout.Y_AXIS));
        typeStatsContent.setBackground(Color.WHITE);
        typeStatsContent.add(statsCreatedByTypeLabel);
        typeStatsContent.add(Box.createVerticalStrut(4));
        typeStatsContent.add(statsActiveByTypeLabel);
        typeStatsContent.add(Box.createVerticalStrut(4));
        typeStatsContent.add(statsExitedByTypeLabel);
        typeStatsContent.add(Box.createVerticalStrut(4));
        typeStatsContent.add(statsAvgWaitByTypeLabel);
        typeStatsContent.add(Box.createVerticalStrut(4));
        typeStatsContent.add(statsAvgRoadByTypeLabel);
        typeStatsContent.add(Box.createVerticalStrut(4));
        typeStatsContent.add(statsTripByTypeLabel);

        JScrollPane typeStatsScroll = new JScrollPane(typeStatsContent);
        typeStatsScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        typeStatsScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        typeStatsScroll.setPreferredSize(new Dimension(550, 160));
        typeStatsPanel.add(typeStatsScroll);
        typeStatsPanel.setMaximumSize(new Dimension(600, 200));
        statsContainerPanel.add(typeStatsPanel);
        statsContainerPanel.add(Box.createHorizontalStrut(10));

        JPanel crossroadStatsPanel = createStatSection("Crossroad Stats");
        statsPerCrossroadArea = new JTextArea(7, 25);
        statsPerCrossroadArea.setEditable(false);
        statsPerCrossroadArea.setFont(new Font("Consolas", Font.PLAIN, 10));
        statsPerCrossroadArea.setLineWrap(true);
        statsPerCrossroadArea.setWrapStyleWord(true);
        JScrollPane crossroadScroll = new JScrollPane(statsPerCrossroadArea);
        crossroadScroll.setPreferredSize(new Dimension(200, 160));
        crossroadStatsPanel.add(crossroadScroll);
        crossroadStatsPanel.setMaximumSize(new Dimension(250, 200));
        statsContainerPanel.add(crossroadStatsPanel);

        JScrollPane bottomScroll = new JScrollPane(statsContainerPanel);
        bottomScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        bottomScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
        add(bottomScroll, BorderLayout.SOUTH);

        new Timer(Config.TIMER_DELAY_MS, e -> {
            boolean changed = false;
            synchronized (this.sprites) {
                for (Iterator<Map.Entry<String, VehicleSprite>> it = this.sprites.entrySet().iterator(); it
                        .hasNext();) {
                    VehicleSprite s = it.next().getValue();
                    if (s.updatePosition())
                        changed = true;
                    if (s.shouldRemoveNow()) {
                        it.remove();
                        changed = true;
                    }
                }
            }
            if (changed)
                renderer.repaint();
        }).start();
    }

    private JPanel createStatSection(String title) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(new Color(255, 255, 255));
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 200), 1),
                BorderFactory.createEmptyBorder(6, 6, 6, 6)));
        panel.setMaximumSize(new Dimension(300, 200));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 11));
        titleLabel.setForeground(new Color(40, 40, 40));
        panel.add(titleLabel);
        panel.add(Box.createVerticalStrut(6));

        return panel;
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
                    synchronized (this.sprites) {
                        spritesEmpty = this.sprites.isEmpty();
                    }
                    boolean queueEmpty = (this.eventQueue == null) || this.eventQueue.isEmpty();
                    if (spritesEmpty && queueEmpty)
                        break;
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

    private void startSimulation() {
        if (this.simulator != null && this.simulator.isRunning()) {
            log("Simulator already running");
            return;
        }

        if (this.renderer != null) {
            this.renderer.revalidate();
            this.renderer.repaint();
        }

        this.simulator = new Simulator();
        this.eventQueue = this.simulator.getEventQueue();

        Thread simThread = new Thread(this.simulator::startSimulation);
        simThread.setDaemon(true);
        simThread.start();

        this.statusLabel.setText("RUNNING");
        this.statusLabel.setForeground(Color.GREEN);

        this.eventConsumer = new Thread(() -> {
            try {
                while (this.simulator != null && this.simulator.isRunning()) {
                    Event ev = this.eventQueue.take();
                    handleEvent(ev);
                }
            } catch (InterruptedException ignored) {
            } catch (Exception ex) {
                log("Event consumer crashed: " + ex.getMessage());
            }
        });
        this.eventConsumer.setDaemon(true);
        this.eventConsumer.start();

        log("Simulator started");

        if (this.autoStopTimer != null && this.autoStopTimer.isRunning()) {
            this.autoStopTimer.stop();
        }
        this.autoStopTimer = new Timer(Config.AUTO_STOP_MS, e -> {
            log("Auto-stop: 60 seconds elapsed — requesting graceful stop.");
            requestGracefulStop();
        });
        this.autoStopTimer.setRepeats(false);
        this.autoStopTimer.start();
    }

    private void stopSimulation() {
        if (this.simulator != null)
            this.simulator.stopSimulation();
        if (this.eventConsumer != null)
            this.eventConsumer.interrupt();

        if (this.autoStopTimer != null) {
            this.autoStopTimer.stop();
            this.autoStopTimer = null;
        }

        this.statusLabel.setText("STOPPED");
        this.statusLabel.setForeground(Color.RED);
        if (this.eventQueue != null) {
            while ((this.eventQueue.poll()) != null) {
            }
        }

        synchronized (this.sprites) {
            this.sprites.clear();
        }

        this.renderer.repaint();
        log("Simulator stopped");
    }

    private void handleEvent(Event ev) {
        if (ev == null)
            return;
        log(ev.toString());

        if (ev instanceof SignalChangeEvent) {
            SignalChangeEvent s = (SignalChangeEvent) ev;
            RoadEnum road = s.getRoad();
            this.model.getTrafficLights().put(road, s.getSignalColor());
            this.model.compactQueue(road);
            SwingUtilities.invokeLater(() -> this.renderer.repaint());
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
                Point p = this.nodePositions.get(ve.getNode());
                synchronized (this.sprites) {
                    this.sprites.put(id, new VehicleSprite(id, v, p.x, p.y));
                }
                long ent = (v.getEntranceTime() > 0) ? v.getEntranceTime() : System.currentTimeMillis();
                this.stats.recordEntranceTimestamp(id, ent);
                this.stats.recordCreatedVehicle(v);
                break;
            }

            case VEHICLE_DEPARTURE: {
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
                SwingUtilities.invokeLater(this::updateStatsLabels);
                Point nodePoint = this.nodePositions.get(ve.getNode());
                synchronized (this.sprites) {
                    VehicleSprite s = this.sprites.get(id);
                    if (s != null) {
                        s.setTarget(nodePoint.x, nodePoint.y, 10);
                    }
                }
                RoadEnum road = roadFromPrevToNode(v, ve.getNode());
                this.model.compactQueue(road);
                break;
            }

            case VEHICLE_ROAD_ARRIVAL: {
                this.handlePassRoad(ve, v);
                break;
            }

            case VEHICLE_SIGNAL_ARRIVAL: {
                this.stats.recordSignalArrival(id);
                // Remove the vehicle's scheduled UI entry for the incoming road
                RoadEnum removeRoad = roadFromPrevToNode(v, ve.getNode());
                if (removeRoad != null) {
                    Deque<java.util.AbstractMap.SimpleEntry<Long, String>> dq = this.passingSchedule.get(removeRoad);
                    if (dq != null) {
                        synchronized (dq) {
                            Iterator<java.util.AbstractMap.SimpleEntry<Long, String>> it = dq.iterator();
                            while (it.hasNext()) {
                                java.util.AbstractMap.SimpleEntry<Long, String> e = it.next();
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
                    SwingUtilities.invokeLater(this::updateStatsLabels);
                }

                this.stats.recordPassedAtNode(ve.getNode(), v);

                RoadEnum incoming = roadFromPrevToNode(v, ve.getNode());
                if (incoming == null) {
                    log("Warning: cannot determine incoming road for vehicle " + id + " to node " + ve.getNode());
                    break;
                }

                synchronized (this.sprites) {
                    VehicleSprite s = this.sprites.get(id);
                    if (s != null) {
                        this.model.enqueueToSignal(incoming, s);
                    }
                }

                break;
            }

            case VEHICLE_EXIT: {
                synchronized (sprites) {
                    VehicleSprite s = sprites.get(id);
                    if (s != null)
                        s.markForRemoval();
                }
                this.stats.recordExitedVehicle(v);
                this.stats.recordTripTimeByType(v);
                this.stats.removeDepartureTimestamp(id);
                this.model.removeSpriteFromAllQueues(id);
                SwingUtilities.invokeLater(this::updateStatsLabels);
                break;
            }

            default: {
                log("Tipo de VehicleEvent não tratado: " + type);
                break;
            }
        }

        SwingUtilities.invokeLater(renderer::repaint);
    }

    private void updateStatsLabels() {
        int active;
        synchronized (sprites) {
            active = sprites.size();
        }

        int created = this.stats.getTotalCreated();
        int exited = this.stats.getTotalExited();
        long travelMs = this.stats.getTotalTravelTimeMs();
        int trips = this.stats.getCompletedTrips();

        double avgSec = (trips == 0) ? 0.0 : (travelMs / 1000.0 / trips);

        if (statsActiveLabel != null)
            statsActiveLabel.setText("Active: " + active);
        if (statsCreatedLabel != null)
            statsCreatedLabel.setText("Created: " + created);
        if (statsExitedLabel != null)
            statsExitedLabel.setText("Exited: " + exited);
        if (statsAvgTimeLabel != null)
            statsAvgTimeLabel.setText(String.format("Avg trip (s): %.2f", avgSec));

        StringBuilder createdBy = new StringBuilder();
        StringBuilder activeBy = new StringBuilder();
        StringBuilder exitedBy = new StringBuilder();

        Map<VehicleType, Integer> createdMap = this.stats.getCreatedByType();
        Map<VehicleType, Integer> exitedMap = this.stats.getExitedByType();
        for (VehicleType vt : VehicleType.values()) {
            int c = createdMap.getOrDefault(vt, 0);
            int x = exitedMap.getOrDefault(vt, 0);
            createdBy.append(vt.getTypeToString()).append("=").append(c).append(" ");
            exitedBy.append(vt.getTypeToString()).append("=").append(x).append(" ");
        }

        Map<VehicleType, Integer> activeMap = new EnumMap<>(VehicleType.class);
        synchronized (sprites) {
            for (VehicleSprite vs : sprites.values()) {
                VehicleType vt = vs.vehicle == null ? null : vs.vehicle.getType();
                if (vt == null)
                    continue;
                activeMap.put(vt, activeMap.getOrDefault(vt, 0) + 1);
            }
        }
        for (VehicleType vt : VehicleType.values()) {
            int a = activeMap.getOrDefault(vt, 0);
            activeBy.append(vt.getTypeToString()).append("=").append(a).append(" ");
        }

        if (statsCreatedByTypeLabel != null)
            statsCreatedByTypeLabel.setText("Created by type: " + createdBy.toString().trim());
        if (statsActiveByTypeLabel != null)
            statsActiveByTypeLabel.setText("Active by type: " + activeBy.toString().trim());
        if (statsExitedByTypeLabel != null)
            statsExitedByTypeLabel.setText("Exited by type: " + exitedBy.toString().trim());

        StringBuilder avgWaitSb = new StringBuilder();
        Map<VehicleType, Long> avgWaitMs = this.stats.getAvgWaitByType();
        StringBuilder avgRoadSb = new StringBuilder();
        Map<VehicleType, Double> avgRoad = this.stats.getAvgRoadByTypeSeconds();
        for (VehicleType vt : VehicleType.values()) {
            long avgMs = avgWaitMs.getOrDefault(vt, 0L);
            double avgW = (avgMs == 0L) ? 0.0 : (avgMs / 1000.0);
            avgWaitSb.append(vt.getTypeToString()).append("=").append(String.format("%.2f", avgW)).append(" ");

            double avgR = avgRoad.getOrDefault(vt, 0.0);
            avgRoadSb.append(vt.getTypeToString()).append("=").append(String.format("%.2f", avgR)).append(" ");
        }

        if (statsAvgWaitByTypeLabel != null)
            statsAvgWaitByTypeLabel.setText("Avg wait (s) by type: " + avgWaitSb.toString().trim());
        if (statsAvgRoadByTypeLabel != null)
            statsAvgRoadByTypeLabel.setText("Avg road (s) by type: " + avgRoadSb.toString().trim());

        StringBuilder tripSb = new StringBuilder();
        Map<VehicleType, long[]> tripStats = this.stats.getTripStatsMillis();
        for (VehicleType vt : VehicleType.values()) {
            long[] arr = tripStats.getOrDefault(vt, new long[] { 0L, 0L, 0L });
            double minS = (arr[0] == 0L) ? 0.0 : (arr[0] / 1000.0);
            double avgS = (arr[1] == 0L) ? 0.0 : (arr[1] / 1000.0);
            double maxS = (arr[2] == 0L) ? 0.0 : (arr[2] / 1000.0);
            tripSb.append(vt.getTypeToString()).append("=")
                    .append(String.format("%.2f", minS)).append("/")
                    .append(String.format("%.2f", avgS)).append("/")
                    .append(String.format("%.2f", maxS)).append(" ");
        }
        if (statsTripByTypeLabel != null)
            statsTripByTypeLabel.setText("Trip min/avg/max (s) by type: " + tripSb.toString().trim());

        StringBuilder perCross = new StringBuilder();
        Map<NodeEnum, Map<VehicleType, Integer>> perNode = this.stats.getPassedByNodeByType();
        for (NodeEnum n : NodeEnum.values()) {
            if (n.getType() != NodeType.CROSSROAD)
                continue;
            perCross.append(n.toString()).append(": ");
            Map<VehicleType, Integer> m = perNode.getOrDefault(n, Collections.emptyMap());
            for (VehicleType vt : VehicleType.values()) {
                int cnt = m.getOrDefault(vt, 0);
                perCross.append(vt.getTypeToString()).append("=").append(cnt).append(" ");
            }
            perCross.append("\n");
        }
        if (statsPerCrossroadArea != null)
            statsPerCrossroadArea.setText(perCross.toString().trim());

    }

    private void handlePassRoad(VehicleEvent ve, Vehicle v) {
        String id = v.getId();
        VehicleSprite s;
        synchronized (this.sprites) {
            s = this.sprites.get(id);
        }
        if (s == null) {
            log("Warning: sprite for " + id + " not found in handlePassRoad; skipping visual update");
            return;
        }

        RoadEnum road = roadFromPrevToNode(v, ve.getNode());
        if (road == null) {
            log("Warning: cannot determine road for vehicle " + id + " in handlePassRoad");
            return;
        }

        Point dest = this.nodePositions.get(ve.getNode());

        long baseTime = (road == null) ? 1000L : road.getTime();
        long passMs = v.getType().getTimeToPass(baseTime);
        long scheduledFinish = System.currentTimeMillis() + passMs;

        Deque<java.util.AbstractMap.SimpleEntry<Long, String>> dq = this.passingSchedule.get(road);
        if (dq == null) {
            synchronized (this.passingSchedule) {
                dq = this.passingSchedule.computeIfAbsent(road, r -> new ArrayDeque<>());
            }
        }

        long corrected;
        synchronized (dq) {
            java.util.AbstractMap.SimpleEntry<Long, String> last = dq.peekLast();
            if (last != null && scheduledFinish < last.getKey()) {
                corrected = last.getKey() + PASS_DELAY_MS;
            } else {
                corrected = scheduledFinish;
            }
            dq.addLast(new java.util.AbstractMap.SimpleEntry<>(corrected, id));
        }

        long anim = Math.max(200L, corrected - System.currentTimeMillis());
        s.clearFaceTarget();
        s.setTarget(dest.x, dest.y, (int) anim);
    }

    private void log(String s) {
        SwingUtilities.invokeLater(() -> {
            if (logArea != null) {
                String timestamp = new java.text.SimpleDateFormat("HH:mm:ss.SSS").format(new java.util.Date());
                logArea.append("[" + timestamp + "] " + s + "\n");
                logArea.setCaretPosition(logArea.getDocument().getLength());
            }
        });
    }

    private RoadEnum roadFromPrevToNode(Vehicle v, NodeEnum node) {
        if (v == null || node == null)
            return null;
        NodeEnum prev = v.findPreviousNode(node);
        if (prev == null)
            return null;
        return RoadEnum.toRoadEnum(prev.toString() + "_" + node.toString());
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new Dashboard().setVisible(true));
    }
}
