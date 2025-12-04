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

/**
 * Swing component that renders the static map (roads and nodes) and
 * the dynamic vehicle sprites on top.
 * <p>
 * The renderer caches a static image of roads and nodes for the
 * current component size to avoid redrawing them on every frame.
 * Dynamic elements (vehicle sprites and traffic light overlays) are
 * painted on each {@link #paintComponent(Graphics)} call. The renderer
 * listens for component resize events to invalidate the static cache
 * and recompute node positions and road geometries.
 */
public class MapRenderer extends JPanel {

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

    /**
     * Create a new MapRenderer backed by the given {@link MapModel}.
     *
     * @param model the shared model containing node positions, sprites and traffic
     *              lights
     */
    public MapRenderer(MapModel model) {
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
                            synchronized (q) {
                                current = q.size();
                            }
                        }
                        int max = (st == null) ? 0 : st.getMax();
                        double avg = (st == null) ? 0.0 : st.getAverage();
                        long samples = (st == null) ? 0L : st.getSamples();
                        UiUtils.showQueueStatsDialog(MapRenderer.this, road, current, max, avg, samples);
                        break;
                    }
                }
            }
        });
    }

    /**
     * Mark the internal static map cache as dirty so it will be rebuilt
     * on the next paint. This method is synchronized with the cache
     * access to ensure thread-safety with the painting code.
     */
    private void markMapDirty() {
        synchronized (this) {
            staticMapCache = null;
        }
    }

    /**
     * Paint the map component.
     * <p>
     * This method draws the cached static map image (rebuilding it if
     * the component size changed), overlays traffic light indicators,
     * and then draws all vehicle sprites from the shared sprite map.
     */
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

    /**
     * Build the cached static map image at the specified size.
     * <p>
     * The image contains a white background, the road network and the
     * node shapes/labels. The generated image is stored in
     * {@code staticMapCache} and used by {@link #paintComponent}.
     *
     * @param w target image width in pixels
     * @param h target image height in pixels
     */
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

    /**
     * Recompute logical node positions for the current component size.
     *
     * @param panelW panel width in pixels
     * @param panelH panel height in pixels
     */
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

    /**
     * Compute the geometric start/end points for every road based on
     * current node positions.
     */
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

    /**
     * Project a point from the node center to the node border along the
     * line toward another point.
     * <p>
     * Given a rectangular node centered at {@code center} with the
     * provided {@code width}/{@code height}, this returns the point on
     * the rectangle border that lies on the line from {@code center}
     * toward {@code toward}.
     *
     * @param center the center point of the node
     * @param toward the point being approached
     * @param width  node width in pixels
     * @param height node height in pixels
     * @return a point positioned on the node border
     */
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

    /**
     * Draw static road lines and arrows into the provided graphics
     * context. This method is used while building the static map image.
     */
    private void drawRoadsStatic(Graphics2D g2) {
        g2.setStroke(ROAD_STROKE);
        g2.setColor(ROAD_COLOR);

        for (RoadGeom rg : roadGeom.values()) {
            g2.drawLine(rg.from.x, rg.from.y, rg.to.x, rg.to.y);
        }
    }

    /**
     * Draw all nodes (entrances, exits and crossroads) and their labels.
     * <p>
     * Node positions are read from the synchronized {@code nodePositions}
     * map.
     */
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
                int textW = fm.stringWidth(name);
                int baseline = (int) Math.round(p.y + (fm.getAscent() - fm.getDescent()) / 2.0);
                g2.drawString(name, p.x - textW / 2, baseline);

            }
        }
    }

    /**
     * Draw the traffic light indicators and populate the clickable
     * {@code signalRects} map used by the mouse listener.
     */
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

    /**
     * Simple immutable container that holds the projected start and end
     * points for a road segment.
     * <p>
     * The renderer computes these points by projecting node centers to
     * their border and stores a {@code RoadGeom} per {@link RoadEnum} so
     * painting code can draw road centerlines and overlays without
     * recomputing geometry repeatedly.
     */
    private static final class RoadGeom {
        final Point from, to;

        /**
         * Create a new RoadGeom with the provided endpoints.
         *
         * @param from projected start point on the origin node border
         * @param to   projected end point on the destination node border
         */
        RoadGeom(Point from, Point to) {
            this.from = from;
            this.to = to;
        }
    }
}
