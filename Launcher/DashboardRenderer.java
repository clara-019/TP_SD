package Launcher;

import Node.NodeEnum;
import Node.NodeType;
import Node.RoadEnum;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.image.BufferedImage;
import java.util.*;

/**
 * Versão otimizada do DashboardRenderer.
 * - Cache do mapa "estático" (background + roads + nodes) em BufferedImage
 * - Recalculo de posições apenas em resize do painel (ComponentListener)
 * - Pré-cálculo de geometrias das roads para reduzir Math/alloc em paint
 * - Desenha semáforos dinamicamente (são voláteis) e sprites por cima
 */
public class DashboardRenderer extends JPanel {
    private final Map<NodeEnum, Point> nodePositions;
    private final Map<String, VehicleSprite> sprites;
    private final Map<RoadEnum, String> trafficLights;

    // cached static map (background, grid cells, roads, nodes, legend)
    private BufferedImage staticMapCache;
    private Dimension lastSize = new Dimension(0, 0);

    // precomputed road geometries used both to draw static roads and to place
    // lights
    private final Map<RoadEnum, RoadGeom> roadGeom = new EnumMap<>(RoadEnum.class);

    // commonly reused objects to avoid reallocation
    private static final Stroke ROAD_STROKE = new BasicStroke(8, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
    private static final Stroke DASHED = new BasicStroke(2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f,
            new float[] { 8f, 12f }, 0f);
    private static final Color ROAD_COLOR = new Color(56, 56, 56);

    public DashboardRenderer(Map<NodeEnum, Point> nodePositions,
            Map<String, VehicleSprite> sprites,
            Map<RoadEnum, String> trafficLights) {
        this.nodePositions = nodePositions;
        this.sprites = sprites;
        this.trafficLights = trafficLights;
        setBackground(Color.WHITE);
        setDoubleBuffered(true);

        // recompute only when resized — keeps paintComponent cheap
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                markMapDirty();
            }
        });
    }

    private void markMapDirty() {
        synchronized (this) {
            staticMapCache = null; // lazy rebuild on next paint
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        Dimension size = getSize();
        // If panel size changed (or cache missing) rebuild static map
        if (staticMapCache == null || !size.equals(lastSize)) {
            synchronized (this) {
                lastSize = new Dimension(size);
                recomputeNodePositionsIfNeeded(size.width, size.height);
                buildStaticMap(size.width, size.height);
            }
        }

        // draw cached static map
        synchronized (this) {
            if (staticMapCache != null)
                g2.drawImage(staticMapCache, 0, 0, null);
        }

        // draw dynamic parts: traffic lights (they change frequently)
        drawTrafficLightsOverlay(g2);

        // draw vehicles on top
        synchronized (sprites) {
            for (VehicleSprite vs : sprites.values())
                vs.draw(g2);
        }

        g2.dispose();
    }

    // ---------- STATIC MAP BUILD ----------
    private void buildStaticMap(int w, int h) {
        if (w <= 0 || h <= 0)
            return;
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // background gradient
        GradientPaint gp = new GradientPaint(0, 0, new Color(245, 247, 250), 0, h, new Color(225, 230, 235));
        g2.setPaint(gp);
        g2.fillRect(0, 0, w, h);

        // draw grid cells lightly (uses nodePositions)
        drawGridCells(g2);

        // compute road geometries once
        computeRoadGeometries();

        // draw roads and arrows (static parts)
        drawRoadsStatic(g2);

        // draw nodes (entrances, crossroads, exits)
        drawNodes(g2);

        // legend (static)
        drawLegend(g2);

        g2.dispose();
        staticMapCache = img;
    }

    // recompute nodePositions only when needed (on resize)
    private void recomputeNodePositionsIfNeeded(int panelW, int panelH) {
        // Similar logic to original but only executed on resize
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

    // ---------- ROAD GEOMETRY CACHE ----------
    private void computeRoadGeometries() {
        roadGeom.clear();

        // Build a map of undirected pairs to lists (like original) but cached
        Map<String, java.util.List<RoadEnum>> forwardMap = new HashMap<>();
        Map<String, java.util.List<RoadEnum>> backwardMap = new HashMap<>();

        for (RoadEnum r : RoadEnum.values()) {
            String a = r.getOrigin().name();
            String b = r.getDestination().name();
            String key = a.compareTo(b) <= 0 ? a + "|" + b : b + "|" + a;
            String first = a.compareTo(b) <= 0 ? a : b;
            if (r.getOrigin().name().equals(first))
                forwardMap.computeIfAbsent(key, k -> new ArrayList<>()).add(r);
            else
                backwardMap.computeIfAbsent(key, k -> new ArrayList<>()).add(r);
        }

        double sideSpacing = 18.0;
        double intraSpacing = 14.0;

        Set<String> keys = new HashSet<>();
        keys.addAll(forwardMap.keySet());
        keys.addAll(backwardMap.keySet());

        for (String key : keys) {
            String[] parts = key.split("\\|");
            if (parts.length != 2)
                continue;
            NodeEnum n1 = NodeEnum.valueOf(parts[0]);
            NodeEnum n2 = NodeEnum.valueOf(parts[1]);
            Point p1, p2;
            synchronized (nodePositions) {
                p1 = nodePositions.get(n1);
                p2 = nodePositions.get(n2);
            }
            if (p1 == null || p2 == null)
                continue;

            double dx = p2.x - p1.x;
            double dy = p2.y - p1.y;
            double len = Math.hypot(dx, dy);
            if (len == 0)
                len = 1;
            double ux = dx / len;
            double uy = dy / len;
            double px = -uy;
            double py = ux;

            java.util.List<RoadEnum> fwd = forwardMap.getOrDefault(key, Collections.emptyList());
            java.util.List<RoadEnum> bwd = backwardMap.getOrDefault(key, Collections.emptyList());

            // forward lanes
            for (int i = 0; i < fwd.size(); i++) {
                RoadEnum r = fwd.get(i);
                double offset = sideSpacing + (i - (fwd.size() - 1) / 2.0) * intraSpacing;
                RoadGeom g = new RoadGeom(p1, p2, ux, uy, px, py, offset);
                roadGeom.put(r, g);
            }
            // backward lanes
            for (int i = 0; i < bwd.size(); i++) {
                RoadEnum r = bwd.get(i);
                double offset = -sideSpacing + (i - (bwd.size() - 1) / 2.0) * intraSpacing;
                RoadGeom g = new RoadGeom(p2, p1, ux, uy, px, py, offset);
                roadGeom.put(r, g);
            }
        }
    }

    private static final class RoadGeom {
        final Point from, to;
        final double ux, uy, px, py;
        final double offset;

        RoadGeom(Point from, Point to, double ux, double uy, double px, double py, double offset) {
            this.from = from;
            this.to = to;
            this.ux = ux;
            this.uy = uy;
            this.px = px;
            this.py = py;
            this.offset = offset;
        }

        int sx1() {
            return (int) Math.round(from.x + px * offset);
        }

        int sy1() {
            return (int) Math.round(from.y + py * offset);
        }

        int sx2() {
            return (int) Math.round(to.x + px * offset);
        }

        int sy2() {
            return (int) Math.round(to.y + py * offset);
        }
    }

    // draw roads static part (bodies and dashed center)
    private void drawRoadsStatic(Graphics2D g2) {
        g2.setStroke(ROAD_STROKE);
        g2.setColor(ROAD_COLOR);
        // draw road bodies
        for (RoadGeom rg : roadGeom.values()) {
            int sx1 = rg.sx1();
            int sy1 = rg.sy1();
            int sx2 = rg.sx2();
            int sy2 = rg.sy2();
            g2.drawLine(sx1, sy1, sx2, sy2);
        }

        // dashed centers
        Stroke prev = g2.getStroke();
        g2.setStroke(DASHED);
        g2.setColor(new Color(230, 230, 230, 160));
        for (RoadGeom rg : roadGeom.values()) {
            int sx1 = rg.sx1();
            int sy1 = rg.sy1();
            int sx2 = rg.sx2();
            int sy2 = rg.sy2();
            g2.drawLine(sx1, sy1, sx2, sy2);
        }
        g2.setStroke(prev);

        // arrows (lightweight) — draw small triangles at end of each road
        for (RoadGeom rg : roadGeom.values()) {
            drawArrowStatic(g2, rg);
        }
    }

    private void drawArrowStatic(Graphics2D g2, RoadGeom rg) {
        int sx1 = rg.sx1();
        int sy1 = rg.sy1();
        int sx2 = rg.sx2();
        int sy2 = rg.sy2();
        double angle = Math.atan2(sy2 - sy1, sx2 - sx1);
        int size = 10;
        int x2 = sx2 - (int) Math.round(size * Math.cos(angle - 0.4));
        int y2 = sy2 - (int) Math.round(size * Math.sin(angle - 0.4));
        int x3 = sx2 - (int) Math.round(size * Math.cos(angle + 0.4));
        int y3 = sy2 - (int) Math.round(size * Math.sin(angle + 0.4));
        Polygon p = new Polygon(new int[] { sx2, x2, x3 }, new int[] { sy2, y2, y3 }, 3);
        g2.setColor(new Color(220, 220, 220, 200));
        g2.fillPolygon(p);
    }

    private void drawGridCells(Graphics2D g2) {
        g2.setColor(new Color(245, 245, 245));
        int cellW = 260, cellH = 120;
        synchronized (nodePositions) {
            for (Point p : nodePositions.values()) {
                g2.fillRoundRect(p.x - cellW / 2, p.y - cellH / 2, cellW, cellH, 14, 14);
                g2.setColor(new Color(200, 200, 200, 80));
                g2.drawRoundRect(p.x - cellW / 2, p.y - cellH / 2, cellW, cellH, 14, 14);
                g2.setColor(new Color(245, 245, 245));
            }
        }
    }

    private void drawNodes(Graphics2D g2) {
        synchronized (nodePositions) {
            for (Map.Entry<NodeEnum, Point> e : nodePositions.entrySet()) {
                NodeEnum node = e.getKey();
                Point p = e.getValue();
                int boxW = 88, boxH = 56;

                if (node.getType() == NodeType.ENTRANCE) {
                    g2.setColor(new Color(200, 245, 200));
                    g2.fillRoundRect(p.x - boxW / 2, p.y - boxH / 2, boxW, boxH, 14, 14);
                    g2.setColor(new Color(0, 120, 40));
                    g2.setStroke(new BasicStroke(2));
                    g2.drawRoundRect(p.x - boxW / 2, p.y - boxH / 2, boxW, boxH, 14, 14);
                } else if (node.getType() == NodeType.EXIT) {
                    g2.setColor(new Color(255, 230, 230));
                    g2.fillRoundRect(p.x - boxW / 2, p.y - boxH / 2, boxW, boxH, 10, 10);
                    g2.setColor(new Color(160, 30, 30));
                    g2.setStroke(new BasicStroke(2));
                    g2.drawRoundRect(p.x - boxW / 2, p.y - boxH / 2, boxW, boxH, 10, 10);
                } else {
                    g2.setColor(new Color(255, 245, 210));
                    g2.fillRect(p.x - boxW / 2, p.y - boxH / 2, boxW, boxH);
                    g2.setColor(Color.BLACK);
                    g2.setStroke(new BasicStroke(1));
                    g2.drawRect(p.x - boxW / 2, p.y - boxH / 2, boxW, boxH);
                }

                // node label
                g2.setColor(Color.BLACK);
                Font orig = g2.getFont();
                Font f = orig.deriveFont(Font.BOLD, 12f);
                g2.setFont(f);
                FontMetrics fm = g2.getFontMetrics(f);
                int textW = fm.stringWidth(node.name());
                g2.drawString(node.name(), p.x - textW / 2, p.y - boxH / 2 - 8);
                g2.setFont(orig);
            }
        }
    }

    // ---------- DYNAMIC TRAFFIC LIGHTS OVERLAY ----------
    private void drawTrafficLightsOverlay(Graphics2D g2) {
        // Draw lights using the precomputed roadGeom positions for consistent placement
        for (Map.Entry<RoadEnum, RoadGeom> entry : roadGeom.entrySet()) {
            RoadEnum road = entry.getKey();
            RoadGeom rg = entry.getValue();

            // compute intersection point with destination node box (approx)
            double hw = 44.0, hh = 28.0;
            double ux = rg.ux, uy = rg.uy, px = rg.px, py = rg.py;
            Point dest = rg.to;
            double tx = (Math.abs(ux) < 1e-6) ? Double.POSITIVE_INFINITY : (hw / Math.abs(ux));
            double ty = (Math.abs(uy) < 1e-6) ? Double.POSITIVE_INFINITY : (hh / Math.abs(uy));
            double t = Math.min(tx, ty);
            double bx = dest.x - ux * t + px * rg.offset;
            double by = dest.y - uy * t + py * rg.offset;

            int w = 14, h = 28;
            int ix = (int) Math.round(bx - w / 2.0);
            int iy = (int) Math.round(by - h / 2.0);

            // housing
            g2.setColor(new Color(40, 40, 40));
            g2.fillRoundRect(ix, iy, w, h, 4, 4);
            g2.setColor(new Color(30, 30, 30));
            g2.drawRoundRect(ix, iy, w, h, 4, 4);

            String state = trafficLights.getOrDefault(road, "RED").toLowerCase();
            boolean isRed = state.equals("red");

            Color redOn = new Color(230, 60, 60);
            Color redOff = new Color(80, 40, 40);
            Color greenOn = new Color(50, 200, 80);
            Color greenOff = new Color(40, 100, 40);

            g2.setColor(isRed ? redOn : redOff);
            g2.fillOval(ix + 2, iy + 4, 10, 10);
            g2.setColor(isRed ? greenOff : greenOn);
            g2.fillOval(ix + 2, iy + 14, 10, 10);

            // small label
            Font origLabelFont = g2.getFont();
            Font labelFont = origLabelFont.deriveFont(Font.BOLD, 12f);
            g2.setFont(labelFont);
            FontMetrics fmLabel = g2.getFontMetrics(labelFont);
            String label = road.name();
            int padding = 6;
            int textW = fmLabel.stringWidth(label);
            int textH = fmLabel.getHeight();
            int labelX = ix + w + 8;
            int labelBaseline = iy + h / 2 + fmLabel.getAscent() / 2;

            int rectX = labelX - padding;
            int rectY = labelBaseline - fmLabel.getAscent() - padding;
            int rectW = textW + padding * 2;
            int rectH = textH + padding * 2;

            g2.setColor(new Color(255, 255, 255, 230));
            g2.fillRoundRect(rectX, rectY, rectW, rectH, 8, 8);
            g2.setColor(new Color(0, 0, 0, 160));
            g2.drawRoundRect(rectX, rectY, rectW, rectH, 8, 8);

            g2.setColor(Color.BLACK);
            g2.drawString(label, labelX, labelBaseline);
            g2.setFont(origLabelFont);
        }
    }

    private void drawLegend(Graphics2D g2) {
        g2.setColor(new Color(0, 0, 0, 160));
        Font orig = g2.getFont();
        g2.setFont(orig.deriveFont(Font.PLAIN, 12f));
        g2.drawString("Legend: Car=Blue, Truck=Gray, Motorcycle=Orange", 12, 18);
        g2.setFont(orig);
    }
}
