package Launcher;

import Event.Event;
import Event.EventType;
import Event.SignalChangeEvent;
import Event.VehicleEvent;
import Node.NodeEnum;
import Traffic.RoadEnum;
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

    private final Map<String, VehicleSprite> sprites;
    private final Map<NodeEnum, Point> nodePositions;
    private final DashboardModel model;

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
        super("Traffic Simulator Dashboard");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1000, 900);
        setLayout(new BorderLayout());

        this.model = new DashboardModel();

        this.sprites = model.getSprites();
        this.nodePositions = model.getNodePositions();

        this.renderer = new DashboardRenderer(this.model);
        add(this.renderer, BorderLayout.CENTER);

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

        this.statusLabel = new JLabel("STOPPED");
        this.statusLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        this.statusLabel.setForeground(Color.RED);
        top.add(this.statusLabel, BorderLayout.EAST);

        add(top, BorderLayout.NORTH);

        this.logArea = new JTextArea(10, 50);
        this.logArea.setEditable(false);
        this.logArea.setFont(new Font("Consolas", Font.PLAIN, 13));
        add(new JScrollPane(this.logArea), BorderLayout.SOUTH);

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

        JPanel statsPanel = new JPanel();
        statsPanel.setLayout(new BoxLayout(statsPanel, BoxLayout.Y_AXIS));
        statsPanel.setBackground(new Color(250, 250, 250));
        statsPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

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
                synchronized (this) {
                    this.totalCreated++;
                    VehicleTypes vt = v.getType();
                    if (vt != null)
                        this.createdByType.put(vt, this.createdByType.getOrDefault(vt, 0) + 1);
                }
                SwingUtilities.invokeLater(this::updateStatsLabels);
                break;
            }

            case VEHICLE_DEPARTURE: {
                synchronized (this.departTimestamps) {
                    this.departTimestamps.put(id, System.currentTimeMillis());
                }
                this.model.removeSpriteFromAllQueues(id);
                SwingUtilities.invokeLater(this::updateStatsLabels);
                break;
            }

            case VEHICLE_ROAD_ARRIVAL: {
                this.handlePassRoad(ve, v);
                break;
            }

            case VEHICLE_SIGNAL_ARRIVAL: {
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
                    SwingUtilities.invokeLater(this::updateStatsLabels);
                }

                NodeEnum prevNode = v.findPreviousNode(ve.getNode());
                RoadEnum incoming = RoadEnum.toRoadEnum(prevNode.toString() + "_" + ve.getNode().toString());

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
                synchronized (this) {
                    this.totalExited++;
                    VehicleTypes vt = v.getType();
                    if (vt != null)
                        this.exitedByType.put(vt, this.exitedByType.getOrDefault(vt, 0) + 1);
                }
                synchronized (this.departTimestamps) {
                    this.departTimestamps.remove(id);
                }
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

        for (VehicleTypes vt : VehicleTypes.values()) {
            int c = createdByType.getOrDefault(vt, 0);
            int x = exitedByType.getOrDefault(vt, 0);
            createdBy.append(vt.getTypeToString()).append("=").append(c).append(" ");
            exitedBy.append(vt.getTypeToString()).append("=").append(x).append(" ");
        }

        Map<VehicleTypes, Integer> activeMap = new EnumMap<>(VehicleTypes.class);
        synchronized (sprites) {
            for (VehicleSprite vs : sprites.values()) {
                VehicleTypes vt = vs.vehicle == null ? null : vs.vehicle.getType();
                if (vt == null)
                    continue;
                activeMap.put(vt, activeMap.getOrDefault(vt, 0) + 1);
            }
        }
        for (VehicleTypes vt : VehicleTypes.values()) {
            int a = activeMap.getOrDefault(vt, 0);
            activeBy.append(vt.getTypeToString()).append("=").append(a).append(" ");
        }

        if (statsCreatedByTypeLabel != null)
            statsCreatedByTypeLabel.setText("Created by type: " + createdBy.toString().trim());
        if (statsActiveByTypeLabel != null)
            statsActiveByTypeLabel.setText("Active by type: " + activeBy.toString().trim());
        if (statsExitedByTypeLabel != null)
            statsExitedByTypeLabel.setText("Exited by type: " + exitedBy.toString().trim());

    }

    private void handlePassRoad(VehicleEvent ve, Vehicle v) {
        String id = v.getId();
        synchronized (this.sprites) {
            VehicleSprite s = this.sprites.get(id);

            RoadEnum road = RoadEnum
                    .toRoadEnum(v.findPreviousNode(ve.getNode()).toString() + "_" + ve.getNode().toString());
            Point dest = this.nodePositions.get(ve.getNode());

            long base = v.getType().getTimeToPass(road.getTime());
            long anim = (long) (base * 2.5);

            s.clearFaceTarget();
            s.setTarget(dest.x, dest.y, anim);
        }
    }

    

    private void log(String s) {
        SwingUtilities.invokeLater(() -> {
            if (logArea != null)
                logArea.append(s + "\n");
        });
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new Dashboard().setVisible(true));
    }
}
