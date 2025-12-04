package Launcher;

import Node.*;
import Vehicle.VehicleType;
import java.awt.*;
import java.util.*;
import javax.swing.*;
import javax.swing.Timer;

/**
 * Dashboard is the main Swing window for the Traffic Simulator application.
 * <p>
 * Responsibilities:
 * - Display the map renderer and animated vehicle sprites.
 * - Provide controls to start/stop the simulation.
 * - Show logs and a variety of runtime statistics (overall, by vehicle type,
 * and per-crossroad counts).
 * <p>
 * The dashboard owns a {@link MapModel} and a {@link DashboardController}
 * which encapsulates simulator logic. It drives a short-period
 * {@link javax.swing.Timer}
 * to animate {@link VehicleSprite} objects and repaints the {@link MapRenderer}
 * only when sprite positions change.
 */
public class Dashboard extends JFrame {
    public static final int TIMER_DELAY_MS = 30;

    private final Map<String, VehicleSprite> sprites;
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

    /**
     * Create and initialize the dashboard window.
     * <p>
     * The constructor builds the UI, creates the model and controller,
     * attaches listeners and starts the internal sprite timer. The window is
     * configured but not shown — callers should call {@code setVisible(true)}.
     */
    public Dashboard() {
        super("Traffic Simulator Dashboard");
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setupWindowClose();
        setSize(1400, 950);
        setLayout(new BorderLayout());

        this.model = new MapModel();
        this.sprites = this.model.getSprites();

        createTopPanel();
        createCenterContainer();
        createStatsContainer();
        createController();
        attachControlListeners();
        startSpriteTimer();
    }

    /**
     * Build and attach the top control panel.
     * <p>
     * The top panel contains the primary control buttons (Start/Stop)
     * on the left and a status label on the right. Buttons are created
     * using {@link UiUtils} helper methods so they follow a consistent
     * application style.
     */
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

    /**
     * Create the central UI area which contains the map renderer and the
     * log panel.
     * <p>
     * The {@link MapRenderer} is placed at the center and a scrollable
     * log area is placed to the right. The log area is produced with
     * {@link UiUtils#makeTextArea} and wrapped in a {@link JScrollPane}.
     */
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

    /**
     * Create the bottom statistics container.
     * <p>
     * This method creates three sections: overall statistics, per-vehicle
     * type statistics and per-crossroad counters. Each section is placed
     * in a small, scrollable region so content can grow without breaking
     * the layout.
     */
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

    /**
     * Instantiate the {@link DashboardController} and wire callbacks.
     * <p>
     * The controller is passed references to the model and renderer, and is given
     * method references for logging, updating UI labels, and updating the status
     * label text and color. These callbacks allow the controller to update the
     * UI without keeping UI-specific logic inside the controller.
     */
    private void createController() {
        this.controller = new DashboardController(this.model, this.renderer, this::log,
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

    /**
     * Attach action listeners to Start/Stop buttons.
     * <p>
     * The listeners call into the controller to start or request a
     * graceful stop of the simulation.
     */
    private void attachControlListeners() {
        this.startBtn.addActionListener(e -> this.controller.startSimulation());
        this.stopBtn.addActionListener(e -> this.controller.requestGracefulStop());
    }

    /**
     * Start a Swing {@link Timer} that advances and repaints vehicle sprites.
     * <p>
     * On every tick the method iterates the {@code sprites} map (synchronized)
     * and calls {@link VehicleSprite#updatePosition} to advance each sprite.
     * Sprites that report they should be removed are deleted from the map.
     * The {@link MapRenderer} is repainted only when a change occurs.
     */
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
                this.renderer.repaint();
        }).start();
    }

    /**
     * Configure window close.
     * <p>
     * When the user closes the window this listener attempts to shut the
     * simulator down via the controller, disposes the window and exits the
     * JVM. Any exceptions during shutdown are logged to the dashboard log.
     */
    private void setupWindowClose() {
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

    /**
     * Refresh all statistics labels using the latest {@link Statistics}.
     * <p>
     * This method reads current statistics from the controller and from
     * the in-memory {@code sprites} map (synchronized) then updates the
     * visible labels and text areas.
     */
    private void updateStatsLabels() {
        int active;
        synchronized (this.sprites) {
            active = this.sprites.size();
        }
        Statistics stats = (controller == null) ? new Statistics() : controller.getStatistics();

        statsActiveLabel.setText("Active: " + active);
        statsCreatedLabel.setText("Created: " + stats.getTotalCreated());
        statsExitedLabel.setText("Exited: " + stats.getTotalExited());
        statsAvgTripLabel.setText(String.format("Avg trip (s): %.2f", stats.getOverallTripStatsMillis()[1] / 1000.0));

        UiUtils.setLabelText(statsCreatedByTypeLabel,
                "Created by type: " + UiUtils.joinCounts(stats.getCreatedByType()));
        UiUtils.setLabelText(statsExitedByTypeLabel, "Exited by type: " + UiUtils.joinCounts(stats.getExitedByType()));

        Map<VehicleType, Integer> activeMap = new EnumMap<>(VehicleType.class);
        synchronized (this.sprites) {
            for (VehicleSprite vs : this.sprites.values()) {
                VehicleType vt = vs.vehicle == null ? null : vs.vehicle.getType();
                if (vt == null)
                    continue;
                activeMap.put(vt, activeMap.getOrDefault(vt, 0) + 1);
            }
        }
        UiUtils.setLabelText(statsActiveByTypeLabel, "Active by type: " + UiUtils.joinCounts(activeMap));

        UiUtils.setLabelText(statsAvgWaitByTypeLabel,
                "Avg wait (s) by type: " + UiUtils.formatAvgWait(stats.getAvgWaitByType()));
        UiUtils.setLabelText(statsAvgRoadByTypeLabel,
                "Avg road (s) by type: " + UiUtils.formatAvgRoad(stats.getAvgRoadByTypeSeconds()));
        UiUtils.setLabelText(statsTripByTypeLabel,
                "Trip min/avg/max (s) by type: " + UiUtils.formatTripStats(stats.getTripStatsMillis()));

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

    /**
     * Append a timestamped message to the log area on the Swing EDT.
     * <p>
     * Formatting and UI updates are performed via
     * {@link SwingUtilities#invokeLater} to ensure thread-safety. If the log area
     * has been disposed or is null the call becomes a no-op.
     *
     * @param s the message to append (must not be null)
     */
    private void log(String s) {
        SwingUtilities.invokeLater(() -> {
            if (logArea != null) {
                String timestamp = new java.text.SimpleDateFormat("HH:mm:ss.SSS").format(new java.util.Date());
                logArea.append("[" + timestamp + "] " + s + "\n");
                logArea.setCaretPosition(logArea.getDocument().getLength());
            }
        });
    }

    /**
     * Launch the dashboard application.
     * <p>
     * Starts the Swing event thread and shows the dashboard window.
     * This is a convenient entry point for running the simulator from the JVM.
     *
     * @param args command-line arguments (ignored)
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new Dashboard().setVisible(true));
    }
}
