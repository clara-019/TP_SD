package Launcher;

import javax.swing.*;
import javax.swing.Timer;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;

import Event.Event;
import Event.SignalChangeEvent;
import Node.NodeEnum;
import Node.NodeType;
import Node.RoadEnum;

public class Dashboard {

    private final Simulator simulator;
    private JFrame frame;

    
    private JButton startBtn, stopBtn;
    private JLabel queueLabel;

    private DefaultListModel<String> eventsModel;
    private NodeGridPanel gridPanel;

    private Timer uiTimer;
    private Thread consumerThread;

    private final Map<NodeEnum, String> signalColors = new ConcurrentHashMap<>();
    private final Map<NodeEnum, Long> signalClocks = new ConcurrentHashMap<>();
    private final Deque<String> recentEvents = new ConcurrentLinkedDeque<>();

    public Dashboard() {
        this.simulator = new Simulator();
        initUI();
        startEventConsumer();
        startUiTimer();
    }

    private void startEventConsumer() {
        consumerThread = new Thread(() -> {
            BlockingQueue<Event> queue = simulator.getEventQueue();

            while (!Thread.currentThread().isInterrupted()) {
                try {
                    Event ev = queue.take();

                    if (ev instanceof SignalChangeEvent sce) {
                        NodeEnum node = sce.getNode();
                        long clk = sce.getLogicalClock();

                        signalClocks.compute(node, (n, prev) -> {
                            if (prev == null || clk >= prev) {
                                signalColors.put(node, sce.getSignalColor());
                                return clk;
                            }
                            return prev;
                        });
                    }

                    recentEvents.addLast(ev.toString());
                    while (recentEvents.size() > 200)
                        recentEvents.removeFirst();

                } catch (InterruptedException ex) {
                    break;
                }
            }
        }, "Dashboard-Consumer");

        consumerThread.setDaemon(true);
        consumerThread.start();
    }

    private void initUI() {
        frame = new JFrame("Traffic Simulator Dashboard");
        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        frame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                shutdown();
            }
        });

        frame.setSize(950, 620);
        frame.setLocationRelativeTo(null);
        frame.setLayout(new BorderLayout(5, 5));

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));

        startBtn = new JButton("Iniciar");
        stopBtn = new JButton("Parar");
        stopBtn.setEnabled(false);

        startBtn.addActionListener(e -> startSimulation());
        stopBtn.addActionListener(e -> stopSimulation());

        queueLabel = new JLabel("Events: 0");

        top.add(startBtn);
        top.add(stopBtn);
        top.add(Box.createHorizontalStrut(20));
        top.add(queueLabel);

        gridPanel = new NodeGridPanel();
        JScrollPane gridScroll = new JScrollPane(gridPanel);

        eventsModel = new DefaultListModel<>();
        JList<String> eventList = new JList<>(eventsModel);
        eventList.setFont(new Font("Monospaced", Font.PLAIN, 11));

        JScrollPane eventsScroll = new JScrollPane(eventList);
        eventsScroll.setPreferredSize(new Dimension(280, 600));

        JSplitPane mainSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, gridScroll, eventsScroll);
        mainSplit.setResizeWeight(0.7);

        frame.add(top, BorderLayout.NORTH);
        frame.add(mainSplit, BorderLayout.CENTER);
        frame.setVisible(true);
    }

    private void startSimulation() {
        startBtn.setEnabled(false);
        stopBtn.setEnabled(true);

        new Thread(() -> {
            try {
                simulator.startSimulation();
            } finally {
                SwingUtilities.invokeLater(() -> {
                    startBtn.setEnabled(true);
                    stopBtn.setEnabled(false);
                });
            }
        }, "Simulator").start();
    }

    private void stopSimulation() {
        stopBtn.setEnabled(false);
        simulator.stopSimulation();

        eventsModel.clear();
        simulator.getEventQueue().clear();

        startBtn.setEnabled(true);
    }

    private void shutdown() {
        try {
            simulator.stopSimulation();
        } catch (Exception ignored) {}

        try {
            consumerThread.interrupt();
        } catch (Exception ignored) {}

        System.exit(0);
    }

    private void startUiTimer() {
        uiTimer = new Timer(400, e -> {
            try {
                queueLabel.setText("Events: " + simulator.getEventQueue().size());

                eventsModel.clear();
                List<String> evs = new ArrayList<>(recentEvents);
                int n = evs.size();
                for (int i = Math.max(0, n - 50); i < n; i++)
                    eventsModel.addElement(evs.get(i));

                gridPanel.repaint();
            } catch (Exception ignored) {}
        });

        uiTimer.start();
    }

    private class NodeGridPanel extends JPanel {

        private final int rows = 3, cols = 3;

        public NodeGridPanel() {
            setBackground(Color.WHITE);
            setPreferredSize(new Dimension(600, 600));
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();

            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int cellW = getWidth() / cols;
            int cellH = getHeight() / rows;

            Map<NodeEnum, Point> centers = new EnumMap<>(NodeEnum.class);
            NodeEnum[] nodes = NodeEnum.values();

            for (int i = 0; i < nodes.length; i++) {
                NodeEnum node = nodes[i];

                int r = i / cols;
                int c = i % cols;

                int x = c * cellW + 10;
                int y = r * cellH + 10;
                int w = cellW - 20;
                int h = cellH - 20;

                drawNodeBox(g2, node, x, y, w, h);
                centers.put(node, new Point(x + w / 2, y + h / 2));
            }

            drawRoads(g2, centers);

            g2.dispose();
        }

        private void drawNodeBox(Graphics2D g2, NodeEnum node, int x, int y, int w, int h) {
            Color bg = switch (node.getType()) {
                case ENTRANCE -> new Color(200, 245, 200);
                case EXIT -> new Color(250, 210, 210);
                case CROSSROAD -> new Color(255, 245, 210);
                default -> new Color(240, 240, 240);
            };

            g2.setColor(bg);
            g2.fillRoundRect(x, y, w, h, 15, 15);

            g2.setColor(Color.DARK_GRAY);
            g2.drawRoundRect(x, y, w, h, 15, 15);

            g2.setColor(Color.BLACK);
            g2.setFont(getFont().deriveFont(Font.BOLD, 14));
            g2.drawString(node.name(), x + 10, y + 20);

            g2.setFont(getFont().deriveFont(Font.PLAIN, 12));
            g2.drawString("Port: " + node.getPort(), x + 10, y + 38);
        }

        private void drawRoads(Graphics2D g2, Map<NodeEnum, Point> centers) {
            g2.setStroke(new BasicStroke(2));
            g2.setColor(new Color(80, 80, 80));

            Map<String, List<RoadEnum>> forwardMap = new HashMap<>();
            Map<String, List<RoadEnum>> backwardMap = new HashMap<>();
            for (RoadEnum r : RoadEnum.values()) {
                String a = r.getOrigin().name();
                String b = r.getDestination().name();
                String key = a.compareTo(b) <= 0 ? a + "|" + b : b + "|" + a;
                String first = a.compareTo(b) <= 0 ? a : b;
                if (r.getOrigin().name().equals(first)) {
                    forwardMap.computeIfAbsent(key, k -> new ArrayList<>()).add(r);
                } else {
                    backwardMap.computeIfAbsent(key, k -> new ArrayList<>()).add(r);
                }
            }

            Set<String> keys = new HashSet<>();
            keys.addAll(forwardMap.keySet());
            keys.addAll(backwardMap.keySet());

            double sideSpacing = 14.0;
            double intraSpacing = 12.0;

            for (String key : keys) {
                List<RoadEnum> fwd = forwardMap.getOrDefault(key, Collections.emptyList());
                List<RoadEnum> bwd = backwardMap.getOrDefault(key, Collections.emptyList());

                String[] parts = key.split("\\|");
                if (parts.length != 2) continue;
                NodeEnum n1 = NodeEnum.valueOf(parts[0]);
                NodeEnum n2 = NodeEnum.valueOf(parts[1]);

                Point p1 = centers.get(n1);
                Point p2 = centers.get(n2);
                if (p1 == null || p2 == null) continue;

                double dx = p2.x - p1.x;
                double dy = p2.y - p1.y;
                double len = Math.hypot(dx, dy);
                if (len == 0) len = 1;
                double ux = dx / len;
                double uy = dy / len;
                double px = -uy;
                double py = ux;

                // forward (n1 -> n2) on positive side
                for (int i = 0; i < fwd.size(); i++) {
                    RoadEnum road = fwd.get(i);
                    double offset = sideSpacing + (i - (fwd.size() - 1) / 2.0) * intraSpacing;
                    int sx1 = (int) Math.round(p1.x + px * offset);
                    int sy1 = (int) Math.round(p1.y + py * offset);
                    int sx2 = (int) Math.round(p2.x + px * offset);
                    int sy2 = (int) Math.round(p2.y + py * offset);
                    g2.drawLine(sx1, sy1, sx2, sy2);
                    double ang = Math.atan2(sy2 - sy1, sx2 - sx1);
                    drawArrow(g2, sx2, sy2, ang);
                    if (road.getDestination().getType() == NodeType.CROSSROAD) {
                        drawTrafficLight(g2, sx2, sy2, road.getDestination());
                    }
                }

                // backward (n2 -> n1) on negative side
                for (int i = 0; i < bwd.size(); i++) {
                    RoadEnum road = bwd.get(i);
                    double offset = -sideSpacing + (i - (bwd.size() - 1) / 2.0) * intraSpacing;
                    int sx1 = (int) Math.round(p2.x + px * offset);
                    int sy1 = (int) Math.round(p2.y + py * offset);
                    int sx2 = (int) Math.round(p1.x + px * offset);
                    int sy2 = (int) Math.round(p1.y + py * offset);
                    g2.drawLine(sx1, sy1, sx2, sy2);
                    double ang = Math.atan2(sy2 - sy1, sx2 - sx1);
                    drawArrow(g2, sx2, sy2, ang);
                    if (road.getDestination().getType() == NodeType.CROSSROAD) {
                        drawTrafficLight(g2, sx2, sy2, road.getDestination());
                    }
                }
            }
        }

        private void drawArrow(Graphics2D g2, int x, int y, double angle) {
            int size = 10;
            Polygon p = new Polygon();
            p.addPoint(x, y);
            p.addPoint(x - (int)(size * Math.cos(angle - 0.5)),
                       y - (int)(size * Math.sin(angle - 0.5)));
            p.addPoint(x - (int)(size * Math.cos(angle + 0.5)),
                       y - (int)(size * Math.sin(angle + 0.5)));

            g2.fillPolygon(p);
        }

        private void drawTrafficLight(Graphics2D g2, int x, int y, NodeEnum node) {
            int w = 14, h = 28;
            int bx = x - w / 2, by = y - h - 8;

            g2.setColor(new Color(30, 30, 30));
            g2.fillRoundRect(bx, by, w, h, 4, 4);
            g2.setColor(Color.BLACK);
            g2.drawRoundRect(bx, by, w, h, 4, 4);

            String state = signalColors.getOrDefault(node, "red").toLowerCase();

            drawLight(g2, bx + w/2, by + 6, state.equals("red") ? Color.RED : new Color(90, 40, 40));
            drawLight(g2, bx + w/2, by + 14, state.equals("yellow") ? Color.YELLOW : new Color(120, 100, 40));
            drawLight(g2, bx + w/2, by + 22, state.equals("green") ? Color.GREEN : new Color(40, 120, 40));
        }

        private void drawLight(Graphics2D g2, int cx, int cy, Color color) {
            g2.setColor(color);
            g2.fillOval(cx - 4, cy - 4, 8, 8);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(Dashboard::new);
    }
}
