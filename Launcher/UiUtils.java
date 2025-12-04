package Launcher;

import Vehicle.VehicleType;
import Traffic.RoadEnum;
import javax.swing.*;
import java.awt.*;
import java.util.Map;

public final class UiUtils {
    private UiUtils() {
    }

    public static JButton makeButton(String text) {
        JButton b = new JButton(text);
        b.setFont(new Font("Segoe UI", Font.BOLD, 14));
        b.setBackground(new Color(48, 71, 94));
        b.setForeground(Color.WHITE);
        b.setFocusable(false);
        b.setBorder(BorderFactory.createEmptyBorder(6, 14, 6, 14));
        return b;
    }

    public static JTextArea makeTextArea(int rows, int cols, Font font, boolean lineWrap, boolean editable) {
        JTextArea ta = new JTextArea(rows, cols);
        ta.setEditable(editable);
        if (font != null)
            ta.setFont(font);
        ta.setLineWrap(lineWrap);
        ta.setWrapStyleWord(true);
        return ta;
    }

    public static JScrollPane wrapInScroll(Component comp, int vPolicy, int hPolicy) {
        JScrollPane sp = new JScrollPane(comp);
        sp.setVerticalScrollBarPolicy(vPolicy);
        sp.setHorizontalScrollBarPolicy(hPolicy);
        return sp;
    }

    public static void addLabelWithGap(JPanel parent, JLabel label, int gap) {
        parent.add(label);
        parent.add(Box.createVerticalStrut(gap));
    }

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

    public static void setLabelText(JLabel lbl, String text) {
        if (lbl != null)
            lbl.setText(text);
    }

    public static Font segoeFont(int style, int size) {
        return new Font("Segoe UI", style, size);
    }

    public static void showQueueStatsDialog(Component parent, RoadEnum road, int current, int max, double avg,
            long samples) {
        String msg = String.format(
                "Road: %s\nCurrent queue: %d\nMax queue: %d\nAverage size: %.2f\nSamples: %d",
                road.name(), current, max, avg, samples);
        JOptionPane.showMessageDialog(parent, msg, "Queue stats", JOptionPane.INFORMATION_MESSAGE);
    }

    public static String joinCounts(Map<VehicleType, Integer> map) {
        StringBuilder sb = new StringBuilder();
        for (VehicleType vt : VehicleType.values()) {
            int v = map.getOrDefault(vt, 0);
            sb.append(vt.getTypeToString()).append("=").append(v).append(" ");
        }
        return sb.toString().trim();
    }

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

    public static String formatAvgRoad(Map<VehicleType, Double> avgRoad) {
        StringBuilder sb = new StringBuilder();
        for (VehicleType vt : VehicleType.values()) {
            double avgR = avgRoad.getOrDefault(vt, 0.0);
            sb.append(vt.getTypeToString()).append("=")
                    .append(String.format("%.2f", avgR)).append(" ");
        }
        return sb.toString().trim();
    }

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
