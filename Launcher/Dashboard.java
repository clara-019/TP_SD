package Launcher;

import Event.*;
import Node.NodeEnum;
import Vehicle.Vehicle;

import javax.swing.*;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.PriorityBlockingQueue;

public class Dashboard extends JFrame {
	private Simulator simulator;
	private PriorityBlockingQueue<Event> eventQueue;

	private final Map<String, VehicleSprite> sprites = new HashMap<>();
	private final Map<NodeEnum, Point> nodePositions = new HashMap<>();
	private final Map<NodeEnum, String> trafficLights = new HashMap<>();

	private JTextArea logArea;
	private DrawPanel drawPanel;

	private Thread eventConsumer;

	public Dashboard() {
		super("Traffic Simulator Dashboard");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setSize(900, 420);
		setLayout(new BorderLayout());

		initNodes();

		drawPanel = new DrawPanel();
		add(drawPanel, BorderLayout.CENTER);

		JPanel controls = new JPanel();
		JButton startBtn = new JButton("Start Simulation");
		JButton stopBtn = new JButton("Stop Simulation");
		controls.add(startBtn);
		controls.add(stopBtn);
		add(controls, BorderLayout.NORTH);

		logArea = new JTextArea(6, 40);
		logArea.setEditable(false);
		JScrollPane scroll = new JScrollPane(logArea);
		add(scroll, BorderLayout.SOUTH);

		startBtn.addActionListener(e -> startSimulation());
		stopBtn.addActionListener(e -> stopSimulation());

		// animation timer
		Timer t = new Timer(40, e -> {
			boolean changed = false;
			synchronized (sprites) {
				for (VehicleSprite s : sprites.values()) {
					if (s.updatePosition()) changed = true;
				}
			}
			if (changed) drawPanel.repaint();
		});
		t.start();
	}

	private void initNodes() {
		// fixed layout for the three nodes in NodeEnum
		nodePositions.put(NodeEnum.E3, new Point(80, 180));
		nodePositions.put(NodeEnum.CR3, new Point(420, 180));
		nodePositions.put(NodeEnum.S, new Point(760, 180));

		trafficLights.put(NodeEnum.CR3, "GREEN");
	}

	private void startSimulation() {
		if (simulator != null && simulator.isRunning()) {
			appendLog("Simulator already running");
			return;
		}
		appendLog("Starting simulator...");
		simulator = new Simulator();
		eventQueue = simulator.getEventQueue();

		Thread simThread = new Thread(() -> simulator.startSimulation());
		simThread.setDaemon(true);
		simThread.start();

		// consumer thread
		eventConsumer = new Thread(() -> {
			try {
				while (simulator != null && simulator.isRunning()) {
					Event ev = eventQueue.take();
					handleEvent(ev);
				}
			} catch (InterruptedException ignored) {
			}
		});
		eventConsumer.setDaemon(true);
		eventConsumer.start();
	}

	private void stopSimulation() {
		if (simulator != null) {
			appendLog("Stopping simulator...");
			simulator.stopSimulation();
			simulator = null;
		}
		if (eventConsumer != null) {
			eventConsumer.interrupt();
			eventConsumer = null;
		}
	}

	private void handleEvent(Event ev) {
		appendLog(ev.toString());
		if (ev instanceof SignalChangeEvent) {
			SignalChangeEvent s = (SignalChangeEvent) ev;
			trafficLights.put(s.getNode(), s.getSignalColor());
			SwingUtilities.invokeLater(() -> drawPanel.repaint());
			return;
		}

		if (ev instanceof VehicleEvent) {
			VehicleEvent ve = (VehicleEvent) ev;
			Vehicle v = ve.getVehicle();
			String vid = v.getId();

			switch (ve.getType()) {
				case NEW_VEHICLE:
					// put at entrance
					Point p = nodePositions.get(ve.getNode());
					VehicleSprite s = new VehicleSprite(vid, v, p.x, p.y);
					// target is crossroad
					Point target = nodePositions.get(NodeEnum.CR3);
					s.setTarget(target.x, target.y);
					synchronized (sprites) { sprites.put(vid, s); }
					break;
				case VEHICLE_ARRIVAL:
					// place at node
					VehicleSprite sa;
					synchronized (sprites) {
						sa = sprites.get(vid);
					}
					if (sa != null) {
						Point np = nodePositions.get(ve.getNode());
						sa.setTarget(np.x, np.y);
					}
					break;
				case VEHICLE_DEPARTURE:
					// move to next node (if from entrance->crossroad, to exit)
					VehicleSprite sd;
					synchronized (sprites) { sd = sprites.get(vid); }
					if (sd != null) {
						if (ve.getNode() == NodeEnum.CR3) {
							Point nex = nodePositions.get(NodeEnum.S);
							sd.setTarget(nex.x, nex.y);
						}
					}
					break;
				case VEHICLE_EXIT:
					synchronized (sprites) {
						sprites.remove(vid);
					}
					break;
				default:
					break;
			}
			SwingUtilities.invokeLater(() -> drawPanel.repaint());
		}
	}

	private void appendLog(String s) {
		SwingUtilities.invokeLater(() -> {
			logArea.append(s + "\n");
			logArea.setCaretPosition(logArea.getDocument().getLength());
		});
	}

	private class DrawPanel extends JPanel {
		DrawPanel() { setBackground(Color.WHITE); }

		@Override
		protected void paintComponent(Graphics g) {
			super.paintComponent(g);
			Graphics2D g2 = (Graphics2D) g;
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

			// draw nodes
			for (Map.Entry<NodeEnum, Point> e : nodePositions.entrySet()) {
				Point p = e.getValue();
				g2.setColor(Color.LIGHT_GRAY);
				g2.fillRect(p.x - 40, p.y - 30, 80, 60);
				g2.setColor(Color.BLACK);
				g2.drawRect(p.x - 40, p.y - 30, 80, 60);
				g2.drawString(e.getKey().toString(), p.x - 10, p.y - 40);
			}

			// draw road lines
			Point e3 = nodePositions.get(NodeEnum.E3);
			Point cr3 = nodePositions.get(NodeEnum.CR3);
			Point s = nodePositions.get(NodeEnum.S);
			g2.setColor(Color.DARK_GRAY);
			g2.setStroke(new BasicStroke(6));
			g2.drawLine(e3.x + 40, e3.y, cr3.x - 40, cr3.y);
			g2.drawLine(cr3.x + 40, cr3.y, s.x - 40, s.y);

			// draw traffic light at CR3
			String color = trafficLights.getOrDefault(NodeEnum.CR3, "GREEN");
			Color light = switch (color.toUpperCase()) {
				case "RED" -> Color.RED;
				case "YELLOW" -> Color.ORANGE;
				default -> Color.GREEN;
			};
			g2.setColor(Color.BLACK);
			g2.fillRect(cr3.x - 60, cr3.y - 60, 30, 50);
			g2.setColor(light);
			g2.fillOval(cr3.x - 56, cr3.y - 54, 22, 22);

			// draw vehicles
			synchronized (sprites) {
				for (VehicleSprite vs : sprites.values()) {
					vs.draw(g2);
				}
			}
		}
	}

	private static class VehicleSprite {
		final String id;
		final Vehicle vehicle;
		double x, y;
		double tx, ty;
		double speed = 2.5;

		VehicleSprite(String id, Vehicle v, double x, double y) {
			this.id = id; this.vehicle = v; this.x = x; this.y = y; this.tx = x; this.ty = y;
		}

		void setTarget(double tx, double ty) { this.tx = tx; this.ty = ty; }

		boolean updatePosition() {
			double dx = tx - x; double dy = ty - y;
			double dist = Math.hypot(dx, dy);
			if (dist < 1.5) { x = tx; y = ty; return false; }
			double vx = dx / dist * speed; double vy = dy / dist * speed;
			x += vx; y += vy; return true;
		}

		void draw(Graphics2D g2) {
			Color c = Color.BLUE;
			if (vehicle.getType() != null) c = Color.MAGENTA;
			g2.setColor(c);
			g2.fillOval((int)x - 8, (int)y - 8, 16, 16);
			g2.setColor(Color.WHITE);
			g2.drawString(id, (int)x - 6, (int)y + 4);
		}
	}

	public static void main(String[] args) {
		SwingUtilities.invokeLater(() -> {
			Dashboard d = new Dashboard();
			d.setVisible(true);
		});
	}
}
