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
    

    private final Map<String, VehicleSprite> sprites;
    private final Map<NodeEnum, Point> nodePositions;
    private final MapModel model;

    private DashboardController controller;

    private JTextArea statsPerCrossroadArea;

    private JTextArea logArea;
    private JLabel statusLabel;
    private JLabel statsActiveLabel;
    private JLabel statsCreatedLabel;
    private JLabel statsExitedLabel;
    private JLabel statsAvgTripLabel;
    private JLabel statsCreatedByTypeLabel;
    private JLabel statsActiveByTypeLabel;
    private JLabel statsExitedByTypeLabel;
    private JLabel statsAvgWaitByTypeLabel;
    private JLabel statsAvgRoadByTypeLabel;
    private JLabel statsTripByTypeLabel;

    private MapRenderer renderer;
    private JButton startBtn;
    private JButton stopBtn;

    public Dashboard() {
        super("Traffic Simulator Dashboard");
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setupWindowCloseHandler();
        setSize(1400, 950);
        setLayout(new BorderLayout());

        this.model = new MapModel();

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

        startBtn = UiUtils.makeButton("Start");
        stopBtn = UiUtils.makeButton("Stop");

        JPanel controls = new JPanel();
        controls.setOpaque(false);
        controls.add(startBtn);
        controls.add(stopBtn);

        top.add(controls, BorderLayout.WEST);

        this.statusLabel = new JLabel("STOPPED");
        this.statusLabel.setFont(UiUtils.segoeFont(Font.BOLD, 16));
        this.statusLabel.setForeground(Color.RED);
        top.add(this.statusLabel, BorderLayout.EAST);

        add(top, BorderLayout.NORTH);
    }

    private void createCenterContainer() {
        JPanel centerContainer = new JPanel(new BorderLayout());

        this.renderer = new MapRenderer(this.model);
        centerContainer.add(this.renderer, BorderLayout.CENTER);

        JPanel rightPanel = new JPanel(new BorderLayout());
        rightPanel.setBackground(new Color(245, 245, 245));
        rightPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(180, 180, 180), 1),
                BorderFactory.createEmptyBorder(8, 8, 8, 8)));

        JLabel logsTitle = new JLabel("Logs");
        logsTitle.setFont(UiUtils.segoeFont(Font.BOLD, 12));
        rightPanel.add(logsTitle, BorderLayout.NORTH);

        this.logArea = UiUtils.makeTextArea(15, 60, new Font("Consolas", Font.PLAIN, 11), false, false);
        JScrollPane logScroll = UiUtils.wrapInScroll(this.logArea, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
                JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        rightPanel.add(logScroll, BorderLayout.CENTER);

        centerContainer.add(rightPanel, BorderLayout.EAST);
        add(centerContainer, BorderLayout.CENTER);
    }

    private void createStatsContainer() {
        JPanel statsContainerPanel = new JPanel();
        statsContainerPanel.setLayout(new BoxLayout(statsContainerPanel, BoxLayout.X_AXIS));
        statsContainerPanel.setBackground(new Color(240, 240, 240));
        statsContainerPanel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        JPanel overallStatsPanel = UiUtils.createStatSection("Overall Statistics");
        statsCreatedLabel = new JLabel("Created: 0");
        statsActiveLabel = new JLabel("Active: 0");
        statsExitedLabel = new JLabel("Exited: 0");
        statsAvgTripLabel = new JLabel("Avg trip (s): 0.00");
        UiUtils.addLabelWithGap(overallStatsPanel, statsCreatedLabel, 4);
        UiUtils.addLabelWithGap(overallStatsPanel, statsActiveLabel, 4);
        UiUtils.addLabelWithGap(overallStatsPanel, statsExitedLabel, 4);
        overallStatsPanel.add(statsAvgTripLabel);
        overallStatsPanel.setMaximumSize(new Dimension(180, 200));
        statsContainerPanel.add(overallStatsPanel);
        statsContainerPanel.add(Box.createHorizontalStrut(10));

        JPanel typeStatsPanel = UiUtils.createStatSection("By Vehicle Type");
        statsCreatedByTypeLabel = new JLabel("<html>Created: -</html>");
        statsActiveByTypeLabel = new JLabel("<html>Active: -</html>");
        statsExitedByTypeLabel = new JLabel("<html>Exited: -</html>");
        statsAvgWaitByTypeLabel = new JLabel("<html>Avg Wait: -</html>");
        statsAvgRoadByTypeLabel = new JLabel("<html>Avg Road: -</html>");
        statsTripByTypeLabel = new JLabel("<html>Trip (min/avg/max): -</html>");

        JPanel typeStatsContent = new JPanel();
        typeStatsContent.setLayout(new BoxLayout(typeStatsContent, BoxLayout.Y_AXIS));
        typeStatsContent.setBackground(Color.WHITE);
        UiUtils.addLabelWithGap(typeStatsContent, statsCreatedByTypeLabel, 4);
        UiUtils.addLabelWithGap(typeStatsContent, statsActiveByTypeLabel, 4);
        UiUtils.addLabelWithGap(typeStatsContent, statsExitedByTypeLabel, 4);
        UiUtils.addLabelWithGap(typeStatsContent, statsAvgWaitByTypeLabel, 4);
        UiUtils.addLabelWithGap(typeStatsContent, statsAvgRoadByTypeLabel, 4);
        typeStatsContent.add(statsTripByTypeLabel);

        JScrollPane typeStatsScroll = UiUtils.wrapInScroll(typeStatsContent, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        typeStatsScroll.setPreferredSize(new Dimension(550, 160));
        typeStatsPanel.add(typeStatsScroll);
        typeStatsPanel.setMaximumSize(new Dimension(600, 200));
        statsContainerPanel.add(typeStatsPanel);
        statsContainerPanel.add(Box.createHorizontalStrut(10));

        JPanel crossroadStatsPanel = UiUtils.createStatSection("Crossroad Stats");
        statsPerCrossroadArea = UiUtils.makeTextArea(7, 25, new Font("Consolas", Font.PLAIN, 10), true, false);
        JScrollPane crossroadScroll = UiUtils.wrapInScroll(statsPerCrossroadArea,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
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

    private void setupWindowCloseHandler() {
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                if (controller != null) {
                    log("Closing dashboard: stopping simulator...");
                    try {
                        controller.shutdown();
                    } catch (Exception ex) {
                        log("Error shutting down simulator: " + ex.getMessage());
                    }
                }
                dispose();
                System.exit(0);
            }
        });
    }

    private void updateStatsLabels() {
        int active;
        synchronized (sprites) {
            active = sprites.size();
        }

        Statistics stats = (controller == null) ? new Statistics() : controller.getStatistics();
        int created = stats.getTotalCreated();
        int exited = stats.getTotalExited();
        long[] overallTrip = stats.getOverallTripStatsMillis();
        double overallAvg = overallTrip[1] / 1000.0;

        if (statsActiveLabel != null)
            statsActiveLabel.setText("Active: " + active);
        if (statsCreatedLabel != null)
            statsCreatedLabel.setText("Created: " + created);
        if (statsExitedLabel != null)
            statsExitedLabel.setText("Exited: " + exited);
        if (statsAvgTripLabel != null)
            statsAvgTripLabel.setText(String.format("Avg trip (s): %.2f", overallAvg));

        Map<VehicleType, Integer> createdMap = stats.getCreatedByType();
        Map<VehicleType, Integer> exitedMap = stats.getExitedByType();
        UiUtils.setLabelText(statsCreatedByTypeLabel, "Created by type: " + UiUtils.joinCounts(createdMap));

        Map<VehicleType, Integer> activeMap = new EnumMap<>(VehicleType.class);
        synchronized (sprites) {
            for (VehicleSprite vs : sprites.values()) {
                VehicleType vt = vs.vehicle == null ? null : vs.vehicle.getType();
                if (vt == null)
                    continue;
                activeMap.put(vt, activeMap.getOrDefault(vt, 0) + 1);
            }
        }
        UiUtils.setLabelText(statsActiveByTypeLabel, "Active by type: " + UiUtils.joinCounts(activeMap));
        UiUtils.setLabelText(statsExitedByTypeLabel, "Exited by type: " + UiUtils.joinCounts(exitedMap));

        Map<VehicleType, Long> avgWaitMs = stats.getAvgWaitByType();
        UiUtils.setLabelText(statsAvgWaitByTypeLabel, "Avg wait (s) by type: " + UiUtils.formatAvgWait(avgWaitMs));

        Map<VehicleType, Double> avgRoad = stats.getAvgRoadByTypeSeconds();
        UiUtils.setLabelText(statsAvgRoadByTypeLabel, "Avg road (s) by type: " + UiUtils.formatAvgRoad(avgRoad));

        Map<VehicleType, long[]> tripStats = stats.getTripStatsMillis();
        UiUtils.setLabelText(statsTripByTypeLabel,
                "Trip min/avg/max (s) by type: " + UiUtils.formatTripStats(tripStats));

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
