package Launcher;

import javax.swing.*;
import java.awt.*;
import java.io.OutputStream;
import java.io.PrintStream;

public class Dashboard extends JFrame {
	private final JTextArea logArea = new JTextArea();
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

		logArea.setEditable(false);
		JScrollPane scrollPane = new JScrollPane(logArea);

		JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		topPanel.add(startButton);
		topPanel.add(stopButton);
		topPanel.add(statusLabel);

		add(topPanel, BorderLayout.NORTH);
		add(scrollPane, BorderLayout.CENTER);

		startButton.addActionListener(e -> startSimulation());
		stopButton.addActionListener(e -> stopSimulation());

		redirectSystemOut();
	}

	private void redirectSystemOut() {
		PrintStream printStream = new PrintStream(new OutputStream() {
			private StringBuilder sb = new StringBuilder();

			@Override
			public void write(int b) {
				if (b == '\n') {
					final String text = sb.toString() + System.lineSeparator();
					SwingUtilities.invokeLater(() -> logArea.append(text));
					sb.setLength(0);
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
		SwingUtilities.invokeLater(() -> logArea.append(text));
	}

	public static void main(String[] args) {
		SwingUtilities.invokeLater(() -> {
			Dashboard d = new Dashboard();
			d.setVisible(true);
		});
	}
}
