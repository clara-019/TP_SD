package Launcher;

import Vehicle.VehicleType;
import Traffic.RoadEnum;
import javax.swing.*;
import java.awt.*;
import java.util.Map;

/**
 * Utility helpers for building and formatting common Swing UI elements
 * used by the simulator dashboard.
 * <p>
 * All methods are static convenience helpers for consistent styling
 * and for formatting statistics data into human-readable strings.
 */
public final class UiUtils {

    /**
     * Create a standardized button used throughout the UI.
     *
     * @param text button label text
     * @return a styled `JButton` instance
     */
    public static JButton makeButton(String text) {
        JButton b = new JButton(text);
        b.setFont(new Font("Segoe UI", Font.BOLD, 14));
        b.setBackground(new Color(48, 71, 94));
        b.setForeground(Color.WHITE);
        b.setFocusable(false);
        b.setBorder(BorderFactory.createEmptyBorder(6, 14, 6, 14));
        return b;
    }

    /**
     * Create a configured `JTextArea`.
     *
     * @param rows     number of rows for the text area
     * @param cols     number of columns for the text area
     * @param font     optional font to apply (null to keep default)
     * @param lineWrap whether to enable line wrapping
     * @param editable whether the text area should be editable
     * @return a configured `JTextArea` instance
     */
    public static JTextArea makeTextArea(int rows, int cols, Font font, boolean lineWrap, boolean editable) {
        JTextArea ta = new JTextArea(rows, cols);
        ta.setEditable(editable);
        if (font != null)
            ta.setFont(font);
        ta.setLineWrap(lineWrap);
        ta.setWrapStyleWord(true);
        return ta;
    }

    /**
     * Wrap a component in a `JScrollPane` with the provided scrollbar policies.
     *
     * @param comp    component to wrap
     * @param vPolicy vertical scrollbar policy (e.g.
     *                `ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED`)
     * @param hPolicy horizontal scrollbar policy
     * @return a `JScrollPane` containing the component
     */
    public static JScrollPane wrapInScroll(Component comp, int vPolicy, int hPolicy) {
        JScrollPane sp = new JScrollPane(comp);
        sp.setVerticalScrollBarPolicy(vPolicy);
        sp.setHorizontalScrollBarPolicy(hPolicy);
        return sp;
    }

    /**
     * Add a label to a panel followed by a vertical gap.
     *
     * @param parent the parent panel to receive the label and gap
     * @param label  the label to add
     * @param gap    vertical gap in pixels to insert after the label
     */
    public static void addLabelWithGap(JPanel parent, JLabel label, int gap) {
        parent.add(label);
        parent.add(Box.createVerticalStrut(gap));
    }

    /**
     * Create a small vertical section panel used to display a titled
     * statistics block.
     *
     * @param title the title text to display at the top of the section
     * @return a configured `JPanel` prepared for stat components
     */
    public static JPanel createStatSection(String title) {
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

    /**
     * Safely set text on a label if it is non-null.
     *
     * @param lbl  label to update (may be null)
     * @param text text to set
     */
    public static void setLabelText(JLabel lbl, String text) {
        if (lbl != null)
            lbl.setText(text);
    }

    /**
     * Convenience factory for the Segoe UI font used in the UI.
     *
     * @param style font style (e.g. `Font.PLAIN`, `Font.BOLD`)
     * @param size  font size in points
     * @return a new `Font` instance
     */
    public static Font segoeFont(int style, int size) {
        return new Font("Segoe UI", style, size);
    }

    /**
     * Show a modal dialog containing queue statistics for a road.
     *
     * @param parent  parent component for the dialog (may be null)
     * @param road    the road being reported
     * @param current current queue length
     * @param max     maximum observed queue length
     * @param avg     average queue size
     * @param samples number of samples used to compute the average
     */
    public static void showQueueStatsDialog(Component parent, RoadEnum road, int current, int max, double avg,
            long samples) {
        String msg = String.format(
                "Road: %s\nCurrent queue: %d\nMax queue: %d\nAverage size: %.2f\nSamples: %d",
                road.name(), current, max, avg, samples);
        JOptionPane.showMessageDialog(parent, msg, "Queue stats", JOptionPane.INFORMATION_MESSAGE);
    }

    /**
     * Render a map of vehicle counts as a compact string in the order of
     * `VehicleType.values()`.
     *
     * @param map counts keyed by `VehicleType`
     * @return a single-line string like "CAR=3 BUS=1 BIKE=0"
     */
    public static String joinCounts(Map<VehicleType, Integer> map) {
        StringBuilder sb = new StringBuilder();
        for (VehicleType vt : VehicleType.values()) {
            int v = map.getOrDefault(vt, 0);
            sb.append(vt.getTypeToString()).append("=").append(v).append(" ");
        }
        return sb.toString().trim();
    }

    /**
     * Format average waiting times (milliseconds) as seconds per vehicle type.
     *
     * @param avgWaitMs map of vehicle type → average wait in milliseconds
     * @return formatted string like "CAR=1.23 BUS=0.00"
     */
    public static String formatAvgWait(Map<VehicleType, Long> avgWaitMs) {
        StringBuilder sb = new StringBuilder();
        for (VehicleType vt : VehicleType.values()) {
            long avgMs = avgWaitMs.getOrDefault(vt, 0L);
            double avgS = (avgMs == 0L) ? 0.0 : (avgMs / 1000.0);
            sb.append(vt.getTypeToString()).append("=")
                    .append(String.format("%.2f", avgS)).append(" ");
        }
        return sb.toString().trim();
    }

    /**
     * Format average road traversal times (or distances) per vehicle type.
     *
     * @param avgRoad map of vehicle type → average road value (double)
     * @return formatted string like "CAR=12.34 BUS=15.00"
     */
    public static String formatAvgRoad(Map<VehicleType, Double> avgRoad) {
        StringBuilder sb = new StringBuilder();
        for (VehicleType vt : VehicleType.values()) {
            double avgR = avgRoad.getOrDefault(vt, 0.0);
            sb.append(vt.getTypeToString()).append("=")
                    .append(String.format("%.2f", avgR)).append(" ");
        }
        return sb.toString().trim();
    }

    /**
     * Format trip statistics (min/avg/max) given in milliseconds per vehicle type.
     * Each value is converted to seconds and rendered as "min/avg/max".
     *
     * @param tripStats map of vehicle type → long[] containing {minMs, avgMs,
     *                  maxMs}
     * @return formatted string like "CAR=1.00/3.50/10.00 BUS=..."
     */
    public static String formatTripStats(Map<VehicleType, long[]> tripStats) {
        StringBuilder sb = new StringBuilder();
        for (VehicleType vt : VehicleType.values()) {
            long[] arr = tripStats.getOrDefault(vt, new long[] { 0L, 0L, 0L });
            double minS = (arr[0] == 0L) ? 0.0 : (arr[0] / 1000.0);
            double avgS = (arr[1] == 0L) ? 0.0 : (arr[1] / 1000.0);
            double maxS = (arr[2] == 0L) ? 0.0 : (arr[2] / 1000.0);
            sb.append(vt.getTypeToString()).append("=")
                    .append(String.format("%.2f", minS)).append("/")
                    .append(String.format("%.2f", avgS)).append("/")
                    .append(String.format("%.2f", maxS)).append(" ");
        }
        return sb.toString().trim();
    }
}
