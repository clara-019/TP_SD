package Launcher;

import Node.NodeEnum;
import Node.NodeType;
import Traffic.RoadEnum;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.image.BufferedImage;
import java.util.*;

public class DashboardRenderer extends JPanel {

    private static final int NODE_W = 80;
    private static final int NODE_H = 80;

    private final Map<NodeEnum, Point> nodePositions;
    private final Map<String, VehicleSprite> sprites;
    private final Map<RoadEnum, String> trafficLights;

    private BufferedImage staticMapCache;
    private Dimension lastSize = new Dimension(0, 0);

    private final Map<RoadEnum, RoadGeom> roadGeom = new EnumMap<>(RoadEnum.class);
    private final Map<RoadEnum, Rectangle> signalRects = new EnumMap<>(RoadEnum.class);

    private static final Stroke ROAD_STROKE = new BasicStroke(6, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
    private static final Color ROAD_COLOR = new Color(56, 56, 56);

    public DashboardRenderer(DashboardModel model) {
        this.nodePositions = model.getNodePositions();
        this.sprites = model.getSprites();
        this.trafficLights = model.getTrafficLights();

        setBackground(Color.WHITE);
        setDoubleBuffered(true);

        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                markMapDirty();
            }
        });

        addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                Point p = e.getPoint();
                for (Map.Entry<RoadEnum, Rectangle> en : signalRects.entrySet()) {
                    Rectangle rect = en.getValue();
                    if (rect != null && rect.contains(p)) {
                        RoadEnum road = en.getKey();
                        java.util.Deque<VehicleSprite> q = model.getSignalQueues().get(road);
                        QueueStats st = model.getQueueStats().get(road);

                        int current = 0;
                        if (q != null) {
                            synchronized (q) { current = q.size(); }
                        }
                        int max = (st == null) ? 0 : st.getMax();
                        double avg = (st == null) ? 0.0 : st.getAverage();
                        long samples = (st == null) ? 0L : st.getSamples();
                        String msg = String.format(
                                "Road: %s\nCurrent queue: %d\nMax queue: %d\nAverage size: %.2f\nSamples: %d",
                                road.name(), current, max, avg, samples);
                        JOptionPane.showMessageDialog(DashboardRenderer.this, msg, "Queue stats",
                                JOptionPane.INFORMATION_MESSAGE);
                        break;
                    }
                }
            }
        });
    }

    private void markMapDirty() {
        synchronized (this) {
            staticMapCache = null;
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        Dimension size = getSize();
        if (staticMapCache == null || !size.equals(lastSize)) {
            synchronized (this) {
                lastSize = new Dimension(size);
                recomputeNodePositions(size.width, size.height);
                buildStaticMap(size.width, size.height);
            }
        }

        synchronized (this) {
            if (staticMapCache != null)
                g2.drawImage(staticMapCache, 0, 0, null);
        }

        drawTrafficLightsOverlay(g2);

        synchronized (sprites) {
            for (VehicleSprite vs : sprites.values())
                vs.draw(g2);
        }

        g2.dispose();
    }

    private void buildStaticMap(int w, int h) {
        if (w <= 0 || h <= 0)
            return;

        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        g2.setColor(Color.WHITE);
        g2.fillRect(0, 0, w, h);

        computeRoadGeometries();
        drawRoadsStatic(g2);
        drawNodes(g2);

        g2.dispose();
        staticMapCache = img;
    }

    private void recomputeNodePositions(int panelW, int panelH) {
        int w = Math.max(600, panelW);
        int h = Math.max(400, panelH);

        int marginX = (int) (w * 0.12);
        int marginY = (int) (h * 0.12);
        int gridW = w - 2 * marginX;
        int gridH = h - 2 * marginY;

        int stepX = (gridW == 0) ? 0 : gridW / 2;
        int stepY = (gridH == 0) ? 0 : gridH / 2;

        NodeEnum[] layout = new NodeEnum[] {
                NodeEnum.E1, NodeEnum.E2, NodeEnum.E3,
                NodeEnum.CR1, NodeEnum.CR2, NodeEnum.CR3,
                NodeEnum.CR4, NodeEnum.CR5, NodeEnum.S
        };

        synchronized (nodePositions) {
            for (int i = 0; i < layout.length; i++) {
                int row = i / 3;
                int col = i % 3;
                int x = marginX + col * stepX;
                int y = marginY + row * stepY;
                nodePositions.put(layout[i], new Point(x, y));
            }
        }
    }

    private void computeRoadGeometries() {
        roadGeom.clear();

        for (RoadEnum r : RoadEnum.values()) {
            Point p1, p2;
            synchronized (nodePositions) {
                p1 = nodePositions.get(r.getOrigin());
                p2 = nodePositions.get(r.getDestination());
            }
            if (p1 == null || p2 == null)
                continue;

            Point adj1 = projectToNodeBorder(p1, p2, NODE_W, NODE_H);
            Point adj2 = projectToNodeBorder(p2, p1, NODE_W, NODE_H);

            roadGeom.put(r, new RoadGeom(adj1, adj2));
        }
    }

    private Point projectToNodeBorder(Point center, Point toward, int width, int height) {
        double dx = toward.x - center.x;
        double dy = toward.y - center.y;

        if (dx == 0 && dy == 0)
            return new Point(center);

        double absDx = Math.abs(dx);
        double absDy = Math.abs(dy);
        double halfW = width / 2.0;
        double halfH = height / 2.0;

        double scale = (absDx / halfW > absDy / halfH)
                ? halfW / absDx
                : halfH / absDy;

        return new Point(
                center.x + (int) Math.round(dx * scale),
                center.y + (int) Math.round(dy * scale));
    }

    private static final class RoadGeom {
        final Point from, to;

        RoadGeom(Point from, Point to) {
            this.from = from;
            this.to = to;
        }
    }

    private void drawRoadsStatic(Graphics2D g2) {
        g2.setStroke(ROAD_STROKE);
        g2.setColor(ROAD_COLOR);

        for (RoadGeom rg : roadGeom.values()) {
            g2.drawLine(rg.from.x, rg.from.y, rg.to.x, rg.to.y);
            drawArrow(g2, rg);
        }
    }

    private void drawArrow(Graphics2D g2, RoadGeom rg) {
        int x1 = rg.from.x, y1 = rg.from.y;
        int x2 = rg.to.x, y2 = rg.to.y;

        double angle = Math.atan2(y2 - y1, x2 - x1);
        int size = 12;

        int xA = x2 - (int) (size * Math.cos(angle - 0.4));
        int yA = y2 - (int) (size * Math.sin(angle - 0.4));

        int xB = x2 - (int) (size * Math.cos(angle + 0.4));
        int yB = y2 - (int) (size * Math.sin(angle + 0.4));

        Polygon arrow = new Polygon(new int[] { x2, xA, xB }, new int[] { y2, yA, yB }, 3);
        g2.setColor(new Color(200, 200, 200));
        g2.fillPolygon(arrow);
    }

    private void drawNodes(Graphics2D g2) {
        synchronized (nodePositions) {
            for (Map.Entry<NodeEnum, Point> e : nodePositions.entrySet()) {
                NodeEnum node = e.getKey();
                Point p = e.getValue();

                int x = p.x - NODE_W / 2;
                int y = p.y - NODE_H / 2;

                if (node.getType() == NodeType.ENTRANCE) {
                    g2.setColor(new Color(200, 245, 200));
                    g2.fillRoundRect(x, y, NODE_W, NODE_H, 12, 12);
                    g2.setColor(new Color(0, 120, 40));
                    g2.drawRoundRect(x, y, NODE_W, NODE_H, 12, 12);

                } else if (node.getType() == NodeType.EXIT) {
                    g2.setColor(new Color(255, 230, 230));
                    g2.fillRoundRect(x, y, NODE_W, NODE_H, 12, 12);
                    g2.setColor(new Color(160, 30, 30));
                    g2.drawRoundRect(x, y, NODE_W, NODE_H, 12, 12);

                } else {
                    g2.setColor(new Color(255, 245, 210));
                    g2.fillRect(x, y, NODE_W, NODE_H);
                    g2.setColor(Color.BLACK);
                    g2.drawRect(x, y, NODE_W, NODE_H);
                }

                g2.setColor(Color.BLACK);
                g2.setFont(g2.getFont().deriveFont(Font.BOLD, 12f));
                FontMetrics fm = g2.getFontMetrics();
                String name = node.name();
                if (node.getType() == NodeType.CROSSROAD) {
                    // draw label centered inside the node box
                    int textW = fm.stringWidth(name);
                    int baseline = (int) Math.round(p.y + (fm.getAscent() - fm.getDescent()) / 2.0);
                    g2.drawString(name, p.x - textW / 2, baseline);
                } else {
                    g2.drawString(name, p.x - fm.stringWidth(name) / 2, y - 6);
                }
            }
        }
    }

    private void drawTrafficLightsOverlay(Graphics2D g2) {
        signalRects.clear();

        for (RoadEnum road : trafficLights.keySet()) {
            RoadGeom rg = roadGeom.get(road);
            if (rg == null)
                continue;

            double dx = rg.to.x - rg.from.x;
            double dy = rg.to.y - rg.from.y;
            double len = Math.hypot(dx, dy);
            if (len == 0)
                continue;

            double ux = dx / len;
            double uy = dy / len;

            int bx = (int) (rg.to.x - ux * 20);
            int by = (int) (rg.to.y - uy * 20);

            int w = 14, h = 28;
            int x = bx - w / 2;
            int y = by - h / 2;

            signalRects.put(road, new Rectangle(x, y, w, h));

            g2.setColor(new Color(40, 40, 40));
            g2.fillRoundRect(x, y, w, h, 4, 4);

            boolean isRed = trafficLights.get(road).equalsIgnoreCase("red");

            g2.setColor(isRed ? Color.RED : new Color(70, 30, 30));
            g2.fillOval(x + 2, y + 3, 10, 10);

            g2.setColor(isRed ? new Color(40, 80, 40) : Color.GREEN);
            g2.fillOval(x + 2, y + 15, 10, 10);
        }
    }
}
