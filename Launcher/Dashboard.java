package Launcher;

import Node.NodeEnum;
import Node.NodeType;
import Vehicle.VehicleType;
import java.awt.*;
import java.util.*;
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
    public static final int TIMER_DELAY_MS = 30;
    public static final int AUTO_STOP_MS = 60_000;

    private final Map<String, VehicleSprite> sprites;
    private final Map<NodeEnum, Point> nodePositions;
    private final DashboardModel model;

    private DashboardController controller;

    private javax.swing.JTextArea statsPerCrossroadArea;

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
    private JButton startBtn;
    private JButton stopBtn;

    public Dashboard() {
        super("Traffic Simulator Dashboard - Organized Layout");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1400, 950);
        setLayout(new BorderLayout());

        this.model = new DashboardModel();

        this.sprites = model.getSprites();
        this.nodePositions = model.getNodePositions();

        createTopPanel();
        createCenterContainer();
        createStatsContainer();
        createController();
        attachControlListeners();
        startSpriteTimer();
    }

    private void createTopPanel() {
        JPanel top = new JPanel(new BorderLayout());
        top.setBackground(new Color(34, 40, 49));
        top.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        startBtn = makeButton("Start");
        stopBtn = makeButton("Stop");

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
    }

    private void createCenterContainer() {
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

        this.logArea = makeTextArea(15, 30, new Font("Consolas", Font.PLAIN, 11), true, false);
        JScrollPane logScroll = wrapInScroll(this.logArea, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        rightPanel.add(logScroll, BorderLayout.CENTER);

        centerContainer.add(rightPanel, BorderLayout.EAST);
        add(centerContainer, BorderLayout.CENTER);
    }

    private void createStatsContainer() {
        JPanel statsContainerPanel = new JPanel();
        statsContainerPanel.setLayout(new BoxLayout(statsContainerPanel, BoxLayout.X_AXIS));
        statsContainerPanel.setBackground(new Color(240, 240, 240));
        statsContainerPanel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        JPanel overallStatsPanel = createStatSection("Overall Statistics");
        statsCreatedLabel = new JLabel("Created: 0");
        statsActiveLabel = new JLabel("Active: 0");
        statsExitedLabel = new JLabel("Exited: 0");
        statsAvgTimeLabel = new JLabel("Avg Trip: 0.0s");
        addLabelWithGap(overallStatsPanel, statsCreatedLabel, 4);
        addLabelWithGap(overallStatsPanel, statsActiveLabel, 4);
        addLabelWithGap(overallStatsPanel, statsExitedLabel, 4);
        overallStatsPanel.add(statsAvgTimeLabel);
        overallStatsPanel.setMaximumSize(new Dimension(180, 200));
        statsContainerPanel.add(overallStatsPanel);
        statsContainerPanel.add(Box.createHorizontalStrut(10));

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
        addLabelWithGap(typeStatsContent, statsCreatedByTypeLabel, 4);
        addLabelWithGap(typeStatsContent, statsActiveByTypeLabel, 4);
        addLabelWithGap(typeStatsContent, statsExitedByTypeLabel, 4);
        addLabelWithGap(typeStatsContent, statsAvgWaitByTypeLabel, 4);
        addLabelWithGap(typeStatsContent, statsAvgRoadByTypeLabel, 4);
        typeStatsContent.add(statsTripByTypeLabel);

        JScrollPane typeStatsScroll = wrapInScroll(typeStatsContent, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        typeStatsScroll.setPreferredSize(new Dimension(550, 160));
        typeStatsPanel.add(typeStatsScroll);
        typeStatsPanel.setMaximumSize(new Dimension(600, 200));
        statsContainerPanel.add(typeStatsPanel);
        statsContainerPanel.add(Box.createHorizontalStrut(10));

        JPanel crossroadStatsPanel = createStatSection("Crossroad Stats");
        statsPerCrossroadArea = makeTextArea(7, 25, new Font("Consolas", Font.PLAIN, 10), true, false);
        JScrollPane crossroadScroll = wrapInScroll(statsPerCrossroadArea, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        crossroadScroll.setPreferredSize(new Dimension(200, 160));
        crossroadStatsPanel.add(crossroadScroll);
        crossroadStatsPanel.setMaximumSize(new Dimension(250, 200));
        statsContainerPanel.add(crossroadStatsPanel);

        JScrollPane bottomScroll = new JScrollPane(statsContainerPanel);
        bottomScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        bottomScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
        add(bottomScroll, BorderLayout.SOUTH);
    }

    private void createController() {
        this.controller = new DashboardController(this.model, this.sprites, this.nodePositions,
                this.renderer,
                this::log,
                this::updateStatsLabels,
                s -> {
                    if (this.statusLabel != null)
                        this.statusLabel.setText(s);
                },
                c -> {
                    if (this.statusLabel != null)
                        this.statusLabel.setForeground(c);
                });
    }

    private void attachControlListeners() {
        if (startBtn != null)
            startBtn.addActionListener(e -> controller.startSimulation());
        if (stopBtn != null)
            stopBtn.addActionListener(e -> controller.requestGracefulStop());
    }

    private void startSpriteTimer() {
        new Timer(TIMER_DELAY_MS, e -> {
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

    private JButton makeButton(String text) {
        JButton b = new JButton(text);
        b.setFont(new Font("Segoe UI", Font.BOLD, 14));
        b.setBackground(new Color(48, 71, 94));
        b.setForeground(Color.WHITE);
        b.setFocusable(false);
        b.setBorder(BorderFactory.createEmptyBorder(6, 14, 6, 14));
        return b;
    }

    private JTextArea makeTextArea(int rows, int cols, Font font, boolean lineWrap, boolean editable) {
        JTextArea ta = new JTextArea(rows, cols);
        ta.setEditable(editable);
        if (font != null) ta.setFont(font);
        ta.setLineWrap(lineWrap);
        ta.setWrapStyleWord(true);
        return ta;
    }

    private JScrollPane wrapInScroll(Component comp, int vPolicy, int hPolicy) {
        JScrollPane sp = new JScrollPane(comp);
        sp.setVerticalScrollBarPolicy(vPolicy);
        sp.setHorizontalScrollBarPolicy(hPolicy);
        return sp;
    }

    private void addLabelWithGap(JPanel parent, JLabel label, int gap) {
        parent.add(label);
        parent.add(Box.createVerticalStrut(gap));
    }

    private void updateStatsLabels() {
        int active;
        synchronized (sprites) {
            active = sprites.size();
        }

        Statistics stats = (controller == null) ? new Statistics() : controller.getStatistics();
        int created = stats.getTotalCreated();
        int exited = stats.getTotalExited();
        long travelMs = stats.getTotalTravelTimeMs();
        int trips = stats.getCompletedTrips();

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

        Map<VehicleType, Integer> createdMap = stats.getCreatedByType();
        Map<VehicleType, Integer> exitedMap = stats.getExitedByType();
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
        Map<VehicleType, Long> avgWaitMs = stats.getAvgWaitByType();
        StringBuilder avgRoadSb = new StringBuilder();
        Map<VehicleType, Double> avgRoad = stats.getAvgRoadByTypeSeconds();
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
        Map<VehicleType, long[]> tripStats = stats.getTripStatsMillis();
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
        Map<NodeEnum, Map<VehicleType, Integer>> perNode = stats.getPassedByNodeByType();
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

    private void log(String s) {
        SwingUtilities.invokeLater(() -> {
            if (logArea != null) {
                String timestamp = new java.text.SimpleDateFormat("HH:mm:ss.SSS").format(new java.util.Date());
                logArea.append("[" + timestamp + "] " + s + "\n");
                logArea.setCaretPosition(logArea.getDocument().getLength());
            }
        });
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new Dashboard().setVisible(true));
    }
}
