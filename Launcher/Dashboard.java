package Launcher;

import javax.swing.*;
import java.awt.*;
import java.io.OutputStream;
import java.io.PrintStream;

public class Dashboard extends JFrame {
	private final JTabbedPane tabs = new JTabbedPane();
	private final JButton startButton = new JButton("Start Simulation");
	private final JButton stopButton = new JButton("Stop Simulation");
	private final JLabel statusLabel = new JLabel("Status: stopped");

	private Simulator simulator;
	private Thread simulatorThread;

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

		startButton.addActionListener(e -> startSimulation());
		stopButton.addActionListener(e -> stopSimulation());

		redirectSystemOut();
		new Thread(this::pollLogs, "Dashboard-LogPoller").start();
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

	private void pollLogs() {
		while (true) {
			try {
				Thread.sleep(1000);
				if (simulator == null) continue;
				Comunication.LogServer ls = simulator.getLogServer();
				if (ls == null) continue;
				java.util.Map<String, java.util.List<String>> batch = ls.drainLogs();
				if (batch == null || batch.isEmpty()) continue;
				for (java.util.Map.Entry<String, java.util.List<String>> e : batch.entrySet()) {
					String proc = e.getKey();
					for (String line : e.getValue()) {
						final String out = line + System.lineSeparator();
						SwingUtilities.invokeLater(() -> appendToTab(proc, out));
					}
				}
			} catch (InterruptedException ignored) { break; } catch (Exception ignored) {}
		}
	}

	public static void main(String[] args) {
		SwingUtilities.invokeLater(() -> {
			Dashboard d = new Dashboard();
			d.setVisible(true);
		});
	}
}
