package Launcher;

import Event.*;
import Node.NodeEnum;
import Node.RoadEnum;
import Vehicle.Vehicle;
import Vehicle.VehicleTypes;

import javax.swing.*;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.PriorityBlockingQueue;

public class Dashboard extends JFrame {

    private static final double ANIMATION_TIME_SCALE = 3.0; // Aumenta para andar mais devagar

    private Simulator simulator;
    private PriorityBlockingQueue<Event> eventQueue;

    private final Map<String, VehicleSprite> sprites = new HashMap<>();
    private final Map<NodeEnum, Point> nodePositions = new HashMap<>();
    private final Map<NodeEnum, String> trafficLights = new HashMap<>();

    private JTextArea logArea;
    private DrawPanel drawPanel;
    private JLabel statusLabel;
    private Thread eventConsumer;

    public Dashboard() {
        super("Traffic Simulator Dashboard");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(950, 500);
        setLayout(new BorderLayout());

        initNodes();

        drawPanel = new DrawPanel();
        add(drawPanel, BorderLayout.CENTER);

        // Top panel
        JPanel topPanel = new JPanel(new BorderLayout());
        JPanel controls = new JPanel();

        JButton startBtn = new JButton("Start Simulation");
        JButton stopBtn = new JButton("Stop Simulation");
        controls.add(startBtn);
        controls.add(stopBtn);
        topPanel.add(controls, BorderLayout.WEST);

        statusLabel = new JLabel("Simulation: STOPPED");
        statusLabel.setForeground(Color.RED);
        topPanel.add(statusLabel, BorderLayout.EAST);

        add(topPanel, BorderLayout.NORTH);

        logArea = new JTextArea(6, 40);
        logArea.setEditable(false);
        add(new JScrollPane(logArea), BorderLayout.SOUTH);

        startBtn.addActionListener(e -> startSimulation());
        stopBtn.addActionListener(e -> stopSimulation());

        // Timer da animação
        Timer timer = new Timer(40, e -> {
            boolean changed = false;
            synchronized (sprites) {
                Iterator<Map.Entry<String, VehicleSprite>> it = sprites.entrySet().iterator();
                while (it.hasNext()) {
                    VehicleSprite s = it.next().getValue();
                    if (s.updatePosition()) changed = true;
                    if (s.shouldRemoveNow()) {
                        it.remove();
                        changed = true;
                    }
                }
            }
            if (changed) drawPanel.repaint();
        });
        timer.start();
    }

    private void initNodes() {
        nodePositions.put(NodeEnum.E3, new Point(80, 220));
        nodePositions.put(NodeEnum.CR3, new Point(450, 220));
        nodePositions.put(NodeEnum.S, new Point(820, 220));
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

        Thread simThread = new Thread(simulator::startSimulation);
        simThread.setDaemon(true);
        simThread.start();

        statusLabel.setText("Simulation: RUNNING");
        statusLabel.setForeground(Color.GREEN);

        eventConsumer = new Thread(() -> {
            try {
                while (simulator != null && simulator.isRunning()) {
                    Event ev = eventQueue.take();
                    handleEvent(ev);
                }
            } catch (InterruptedException ignored) {}
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

        statusLabel.setText("Simulation: STOPPED");
        statusLabel.setForeground(Color.RED);

        if (eventQueue != null) {
            while (!eventQueue.isEmpty()) {
                eventQueue.poll();
            }
        }

        synchronized (sprites) {
            sprites.clear();
        }
        SwingUtilities.invokeLater(drawPanel::repaint);
    }

    private void handleEvent(Event ev) {
        appendLog(ev.toString());

        if (ev instanceof SignalChangeEvent s) {
            trafficLights.put(s.getNode(), s.getSignalColor());
            SwingUtilities.invokeLater(drawPanel::repaint);
            return;
        }

        if (!(ev instanceof VehicleEvent ve)) return;

        Vehicle v = ve.getVehicle();
        String vid = v.getId();

        switch (ve.getType()) {

            case NEW_VEHICLE -> {
                // Cria sprite parado no nó de origem
                Point p = nodePositions.get(ve.getNode()); // normalmente E3
                VehicleSprite sprite = new VehicleSprite(vid, v, p.x, p.y);
                synchronized (sprites) {
                    sprites.put(vid, sprite);
                }
            }

            case VEHICLE_DEPARTURE -> {
                // Começa movimento entre nós, com duração igual (x fator) ao tempo da simulação
                synchronized (sprites) {
                    VehicleSprite sprite = sprites.get(vid);
                    if (sprite == null) break;

                    RoadEnum road = null;
                    NodeEnum to = null;

                    if (ve.getNode() == NodeEnum.E3) {
                        road = RoadEnum.E3_CR3;
                        to = NodeEnum.CR3;
                    } else if (ve.getNode() == NodeEnum.CR3) {
                        road = RoadEnum.CR3_S;
                        to = NodeEnum.S;
                    }

                    if (road != null && to != null) {
                        Point dest = nodePositions.get(to);
                        long travelMs = v.getType().getTimeToPass(road.getTime());
                        long durationAnim = (long) (travelMs * ANIMATION_TIME_SCALE);
                        sprite.setTarget(dest.x - 40, dest.y, durationAnim);
                    }
                }
            }

            case VEHICLE_ARRIVAL -> {
                // Opcional: snap ao nó (se houver esse evento na tua simulação)
                synchronized (sprites) {
                    VehicleSprite s = sprites.get(vid);
                    if (s != null) {
                        Point p = nodePositions.get(ve.getNode());
                        s.x = p.x;
                        s.y = p.y;
                    }
                }
            }

            case VEHICLE_EXIT -> {
                // Marca para ser removido quando a animação atual terminar
                synchronized (sprites) {
                    VehicleSprite s = sprites.get(vid);
                    if (s != null) s.markForRemoval();
                }
            }

            default -> {}
        }

        SwingUtilities.invokeLater(drawPanel::repaint);
    }

    private void appendLog(String s) {
        SwingUtilities.invokeLater(() -> {
            logArea.append(s + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }

    // =======================
    //      PAINEL DESENHO
    // =======================
    private class DrawPanel extends JPanel {
        DrawPanel() {
            setBackground(Color.WHITE);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // Estradas
            drawRoad(g2, NodeEnum.E3, NodeEnum.CR3);
            drawRoad(g2, NodeEnum.CR3, NodeEnum.S);

            // Nós
            for (Map.Entry<NodeEnum, Point> e : nodePositions.entrySet()) {
                Point p = e.getValue();
                g2.setColor(Color.LIGHT_GRAY);
                g2.fillRect(p.x - 40, p.y - 30, 80, 60);
                g2.setColor(Color.BLACK);
                g2.drawRect(p.x - 40, p.y - 30, 80, 60);
                g2.drawString(e.getKey().toString(), p.x - 10, p.y - 40);
            }

            // Semáforo CR3
            drawTrafficLightCR3(g2);

            // Legenda
            g2.setColor(Color.BLACK);
            g2.drawString("Legend: Car=Blue, Truck=Gray, Motorcycle=Orange", 10, 15);

            // Veículos
            synchronized (sprites) {
                for (VehicleSprite vs : sprites.values()) {
                    vs.draw(g2);
                }
            }
        }

        private void drawRoad(Graphics2D g2, NodeEnum from, NodeEnum to) {
            Point p1 = nodePositions.get(from);
            Point p2 = nodePositions.get(to);
            g2.setColor(Color.DARK_GRAY);
            g2.setStroke(new BasicStroke(6));
            g2.drawLine(p1.x + 40, p1.y, p2.x - 40, p2.y);

            // seta
            double dx = p2.x - p1.x;
            double dy = p2.y - p1.y;
            double angle = Math.atan2(dy, dx);
            int arrowSize = 12;
            Path2D arrow = new Path2D.Double();
            arrow.moveTo(0, 0);
            arrow.lineTo(-arrowSize, -arrowSize / 2.0);
            arrow.lineTo(-arrowSize, arrowSize / 2.0);
            arrow.closePath();
            AffineTransform at = new AffineTransform();
            at.translate(p2.x - 50, p2.y);
            at.rotate(angle);
            Shape arrowShape = at.createTransformedShape(arrow);
            g2.fill(arrowShape);
        }

        private void drawTrafficLightCR3(Graphics2D g2) {
            Point cr3 = nodePositions.get(NodeEnum.CR3);
            String color = trafficLights.getOrDefault(NodeEnum.CR3, "GREEN");
            Color light = switch (color.toUpperCase()) {
                case "RED" -> new Color(255, 50, 50);
                case "YELLOW" -> new Color(255, 200, 0);
                default -> new Color(50, 255, 50);
            };
            g2.setColor(Color.BLACK);
            g2.fillRect(cr3.x - 60, cr3.y - 60, 30, 50);
            g2.setColor(light);
            g2.fillOval(cr3.x - 56, cr3.y - 54, 22, 22);
        }
    }

    // =======================
    //     SPRITE DO CARRO
    // =======================
    private static class VehicleSprite {
        final String id;
        final Vehicle vehicle;

        double x, y;
        double startX, startY;
        double tx, ty;
        long startTime;
        long durationMs;
        boolean moving = false;
        boolean removeWhenArrives = false;

        VehicleSprite(String id, Vehicle v, double x, double y) {
            this.id = id;
            this.vehicle = v;
            this.x = x;
            this.y = y;
            this.startX = x;
            this.startY = y;
            this.tx = x;
            this.ty = y;
        }

        void setTarget(double tx, double ty, long durationMs) {
            this.startX = this.x;
            this.startY = this.y;
            this.tx = tx;
            this.ty = ty;
            this.startTime = System.currentTimeMillis();
            this.durationMs = Math.max(1, durationMs);
            this.moving = true;
        }

        void markForRemoval() {
            this.removeWhenArrives = true;
        }

        boolean shouldRemoveNow() {
            return removeWhenArrives && !moving;
        }

        boolean updatePosition() {
            if (!moving) return false;

            long now = System.currentTimeMillis();
            double t = (now - startTime) / (double) durationMs;

            if (t >= 1.0) {
                x = tx;
                y = ty;
                moving = false;
                return true;
            }

            x = startX + (tx - startX) * t;
            y = startY + (ty - startY) * t;
            return true;
        }

        void draw(Graphics2D g2) {
            VehicleTypes vt = vehicle.getType();
            if (vt == null) vt = VehicleTypes.CAR;

            switch (vt) {
                case CAR -> {
                    g2.setColor(new Color(30, 144, 255));
                    g2.fillOval((int) x - 10, (int) y - 6, 20, 12);
                }
                case TRUCK -> {
                    g2.setColor(Color.DARK_GRAY);
                    g2.fillRect((int) x - 12, (int) y - 7, 24, 14);
                }
                case MOTORCYCLE -> {
                    g2.setColor(new Color(255, 140, 0));
                    int[] xs = {(int) x, (int) x - 8, (int) x + 8};
                    int[] ys = {(int) y - 6, (int) y + 6, (int) y + 6};
                    g2.fillPolygon(xs, ys, 3);
                }
            }
            g2.setColor(Color.WHITE);
            g2.drawString(id, (int) x - 6, (int) y + 22);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            Dashboard d = new Dashboard();
            d.setVisible(true);
        });
    }
}
