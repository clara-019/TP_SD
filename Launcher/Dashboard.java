package Launcher;

import javax.swing.*;
import java.awt.*;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Dashboard extends JFrame {
	private final JTabbedPane tabs = new JTabbedPane();
	private final JButton startButton = new JButton("Start Simulation");
	private final JButton stopButton = new JButton("Stop Simulation");
	private final JLabel statusLabel = new JLabel("Status: stopped");

	private Simulator simulator;
	private Thread simulatorThread;

	private final Map<String, Integer> vehiclePositions = new ConcurrentHashMap<>();
	private final GridPanel gridPanel = new GridPanel();

	public Dashboard() {
		super("Traffic Simulator Dashboard");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setSize(800, 600);
		setLayout(new BorderLayout());

		JScrollPane scrollPane = new JScrollPane(tabs);

		JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		topPanel.add(startButton);
		topPanel.add(stopButton);
		topPanel.add(statusLabel);

		add(topPanel, BorderLayout.NORTH);
		add(scrollPane, BorderLayout.CENTER);
		add(gridPanel, BorderLayout.SOUTH);

		startButton.addActionListener(e -> startSimulation());
		stopButton.addActionListener(e -> stopSimulation());

		redirectSystemOut();
	}

	private void redirectSystemOut() {
		PrintStream printStream = new PrintStream(new OutputStream() {
			private StringBuilder sb = new StringBuilder();

			@Override
			public void write(int b) {
				if (b == '\r') return;
				if (b == '\n') {
					final String text = sb.toString();
					sb.setLength(0);
					if (text.trim().isEmpty()) return;
					SwingUtilities.invokeLater(() -> appendToTab("Local", text + System.lineSeparator()));
				} else {
					sb.append((char) b);
				}
			}
		}, true);
		System.setOut(printStream);
		System.setErr(printStream);
	}

	private void startSimulation() {
		if (simulator != null && simulator.isRunning()) {
			appendLog("Simulation already running\n");
			return;
		}
		simulator = new Simulator();
		simulatorThread = new Thread(() -> {
			statusLabel.setText("Status: running");
			simulator.startSimulation();
			SwingUtilities.invokeLater(() -> statusLabel.setText("Status: stopped"));
		}, "Simulator-Thread");
		simulatorThread.start();
		appendLog("Simulator started\n");
	}

	private void stopSimulation() {
		if (simulator == null || !simulator.isRunning()) {
			appendLog("Simulation is not running\n");
			return;
		}
		simulator.stopSimulation();
		if (simulatorThread != null) simulatorThread.interrupt();
		appendLog("Stop command sent to simulator\n");
		statusLabel.setText("Status: stopping");
	}

	private void appendLog(String text) {
		SwingUtilities.invokeLater(() -> {
			appendToTab("Local", text);
		});
	}

	private void appendToTab(String process, String text) {
		JTextArea area = getOrCreateArea(process);
		area.append(text);
	}

	private JTextArea getOrCreateArea(String process) {
		Component comp = null;
		for (int i = 0; i < tabs.getTabCount(); i++) {
			if (tabs.getTitleAt(i).equals(process)) { comp = tabs.getComponentAt(i); break; }
		}
		if (comp == null) {
			JTextArea area = new JTextArea();
			area.setEditable(false);
			tabs.addTab(process, new JScrollPane(area));
			return area;
		} else {
			JScrollPane sp = (JScrollPane) comp;
			return (JTextArea) ((JViewport) sp.getViewport()).getView();
		}
	}

	private static final int GRID_HEIGHT = 160;

	private class GridPanel extends JPanel {
		public GridPanel() {
			setPreferredSize(new Dimension(800, GRID_HEIGHT));
		}

		@Override
		protected void paintComponent(Graphics g) {
			super.paintComponent(g);
			Graphics2D g2 = (Graphics2D) g;
			int cols = 3;
			int w = getWidth();
			int h = getHeight();
			int colW = w / cols;

			// draw column separators and labels
			String[] labels = new String[]{"E3", "CR3", "S"};
			for (int i = 0; i < cols; i++) {
				int x = i * colW;
				g2.setColor(Color.LIGHT_GRAY);
				g2.fillRect(x, 0, colW - 2, h);
				g2.setColor(Color.BLACK);
				g2.drawString(labels[i], x + 10, 15);
			}

			// draw vehicles
			int radius = 24;
			int y = 40;
			int spacing = 8;
			int colY = y;
			for (java.util.Map.Entry<String, Integer> e : vehiclePositions.entrySet()) {
				String id = e.getKey();
				int col = e.getValue();
				int cx = col * colW + 20;
				g2.setColor(Color.ORANGE);
				g2.fillOval(cx, colY, radius, radius);
				g2.setColor(Color.BLACK);
				g2.drawString(id, cx + radius + 4, colY + radius - 6);
				colY += radius + spacing;
				if (colY > h - 30) colY = y; // wrap
			}
		}
	}

	public static void main(String[] args) {
		SwingUtilities.invokeLater(() -> {
			Dashboard d = new Dashboard();
			d.setVisible(true);
		});
	}
}
