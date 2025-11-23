package Launcher;

import Node.NodeEnum;
import Node.NodeType;
import Node.RoadEnum;

import javax.swing.*;
import java.awt.*;
import java.util.Map;

/**
 * Painel responsável por desenhar o mapa (nós, estradas, semáforos) e os veículos.
 * Esta classe foi escrita para trabalhar com as estruturas do teu projecto: NodeEnum, RoadEnum, Vehicle, VehicleTypes.
 *
 * Construtor recebe referências partilhadas para as estruturas de estado do Dashboard:
 *  - nodePositions: mapa (mutável) com posições base dos nós. O render atualiza estas posições para uma grelha responsiva
 *  - sprites: mapa de VehicleSprite (state + animação) mantido pelo Dashboard
 *  - trafficLights: estado dos semáforos por RoadEnum
 */
public class DashboardRenderer extends JPanel {
    private final Map<NodeEnum, Point> nodePositions;
    private final Map<String, VehicleSprite> sprites;
    private final Map<RoadEnum, String> trafficLights;

    public DashboardRenderer(Map<NodeEnum, Point> nodePositions,
                             Map<String, VehicleSprite> sprites,
                             Map<RoadEnum, String> trafficLights) {
        this.nodePositions = nodePositions;
        this.sprites = sprites;
        this.trafficLights = trafficLights;
        setBackground(Color.WHITE);
        setDoubleBuffered(true);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // background subtle gradient
        paintBackground(g2);

        // recompute node positions into a responsive 3-column layout
        recomputeNodePositions();

        // draw grid cells lightly
        drawGridCells(g2);

        // draw roads (with parallel lanes and arrows)
        drawRoads(g2);

        // draw nodes (entrances, crossroads, exits)
        drawNodes(g2);

        // draw traffic lights for roads that lead to crossroads
        drawTrafficLights(g2);

        // legend
        drawLegend(g2);

        // draw vehicles (sprites) on top
        synchronized (sprites) {
            for (VehicleSprite vs : sprites.values()) vs.draw(g2);
        }

        g2.dispose();
    }

    private void paintBackground(Graphics2D g2) {
        int w = getWidth(), h = getHeight();
        GradientPaint gp = new GradientPaint(0, 0, new Color(245, 247, 250), 0, h, new Color(225, 230, 235));
        g2.setPaint(gp);
        g2.fillRect(0, 0, w, h);
    }

    private void recomputeNodePositions() {
        // Fixed 3x3 mapping requested by user:
        // row1: E1, E2, E3
        // row2: CR1, CR2, CR3
        // row3: CR4, CR5, S
        int w = Math.max(600, getWidth());
        int h = Math.max(400, getHeight());

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

        for (int i = 0; i < layout.length; i++) {
            int row = i / 3;
            int col = i % 3;
            int x = marginX + col * stepX;
            int y = marginY + row * stepY;
            nodePositions.put(layout[i], new Point(x, y));
        }
    }

    private void drawGridCells(Graphics2D g2) {
        g2.setColor(new Color(245, 245, 245));
        int cellW = 260, cellH = 120;
        for (Point p : nodePositions.values()) {
            g2.fillRoundRect(p.x - cellW/2, p.y - cellH/2, cellW, cellH, 14, 14);
            g2.setColor(new Color(200, 200, 200, 80));
            g2.drawRoundRect(p.x - cellW/2, p.y - cellH/2, cellW, cellH, 14, 14);
            g2.setColor(new Color(245, 245, 245));
        }
    }

    private void drawRoads(Graphics2D g2) {
        g2.setStroke(new BasicStroke(8, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

        // We'll draw each undirected pair once and offset parallel lanes.
        java.util.Map<String, java.util.List<RoadEnum>> forwardMap = new java.util.HashMap<>();
        java.util.Map<String, java.util.List<RoadEnum>> backwardMap = new java.util.HashMap<>();

        for (RoadEnum r : RoadEnum.values()) {
            String a = r.getOrigin().name();
            String b = r.getDestination().name();
            String key = a.compareTo(b) <= 0 ? a + "|" + b : b + "|" + a;
            String first = a.compareTo(b) <= 0 ? a : b;
            if (r.getOrigin().name().equals(first)) forwardMap.computeIfAbsent(key, k -> new java.util.ArrayList<>()).add(r);
            else backwardMap.computeIfAbsent(key, k -> new java.util.ArrayList<>()).add(r);
        }

        java.util.Set<String> keys = new java.util.HashSet<>();
        keys.addAll(forwardMap.keySet()); keys.addAll(backwardMap.keySet());

        double sideSpacing = 18.0;
        double intraSpacing = 14.0;

        g2.setColor(new Color(70,70,70));
        for (String key : keys) {
            String[] parts = key.split("\\|");
            if (parts.length != 2) continue;
            NodeEnum n1 = NodeEnum.valueOf(parts[0]);
            NodeEnum n2 = NodeEnum.valueOf(parts[1]);
            Point p1 = nodePositions.get(n1); Point p2 = nodePositions.get(n2);
            if (p1 == null || p2 == null) continue;

            double dx = p2.x - p1.x; double dy = p2.y - p1.y;
            double len = Math.hypot(dx, dy); if (len == 0) len = 1;
            double ux = dx / len; double uy = dy / len; double px = -uy; double py = ux;

            java.util.List<RoadEnum> fwd = forwardMap.getOrDefault(key, java.util.Collections.emptyList());
            java.util.List<RoadEnum> bwd = backwardMap.getOrDefault(key, java.util.Collections.emptyList());

            // forward lanes (n1 -> n2)
            for (int i = 0; i < fwd.size(); i++) {
                double offset = sideSpacing + (i - (fwd.size() - 1) / 2.0) * intraSpacing;
                int sx1 = (int) Math.round(p1.x + px * offset);
                int sy1 = (int) Math.round(p1.y + py * offset);
                int sx2 = (int) Math.round(p2.x + px * offset);
                int sy2 = (int) Math.round(p2.y + py * offset);

                // road body
                g2.setStroke(new BasicStroke(8, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.setColor(new Color(56,56,56));
                g2.drawLine(sx1, sy1, sx2, sy2);

                // center dashed line
                Stroke prev = g2.getStroke();
                g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, new float[]{8f,12f}, 0f));
                g2.setColor(new Color(230,230,230,160));
                g2.drawLine(sx1, sy1, sx2, sy2);
                g2.setStroke(prev);

                // arrow
                drawArrow(g2, sx2, sy2, Math.atan2(sy2 - sy1, sx2 - sx1));
            }

            // backward lanes (n2 -> n1)
            for (int i = 0; i < bwd.size(); i++) {
                double offset = -sideSpacing + (i - (bwd.size() - 1) / 2.0) * intraSpacing;
                int sx1 = (int) Math.round(p2.x + px * offset);
                int sy1 = (int) Math.round(p2.y + py * offset);
                int sx2 = (int) Math.round(p1.x + px * offset);
                int sy2 = (int) Math.round(p1.y + py * offset);

                g2.setStroke(new BasicStroke(8, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.setColor(new Color(56,56,56));
                g2.drawLine(sx1, sy1, sx2, sy2);

                Stroke prev = g2.getStroke();
                g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, new float[]{8f,12f}, 0f));
                g2.setColor(new Color(230,230,230,160));
                g2.drawLine(sx1, sy1, sx2, sy2);
                g2.setStroke(prev);

                drawArrow(g2, sx2, sy2, Math.atan2(sy2 - sy1, sx2 - sx1));
            }
        }
    }

    private void drawNodes(Graphics2D g2) {
        for (Map.Entry<NodeEnum, Point> e : nodePositions.entrySet()) {
            NodeEnum node = e.getKey(); Point p = e.getValue();
            int boxW = 88, boxH = 56;

            if (node.getType() == NodeType.ENTRANCE) {
                g2.setColor(new Color(200, 245, 200));
                g2.fillRoundRect(p.x - boxW/2, p.y - boxH/2, boxW, boxH, 14, 14);
                g2.setColor(new Color(0,120,40));
                g2.setStroke(new BasicStroke(2));
                g2.drawRoundRect(p.x - boxW/2, p.y - boxH/2, boxW, boxH, 14, 14);
            } else if (node.getType() == NodeType.EXIT) {
                g2.setColor(new Color(255,230,230));
                g2.fillRoundRect(p.x - boxW/2, p.y - boxH/2, boxW, boxH, 10, 10);
                g2.setColor(new Color(160,30,30));
                g2.setStroke(new BasicStroke(2));
                g2.drawRoundRect(p.x - boxW/2, p.y - boxH/2, boxW, boxH, 10, 10);
            } else {
                g2.setColor(new Color(255,245,210));
                g2.fillRect(p.x - boxW/2, p.y - boxH/2, boxW, boxH);
                g2.setColor(Color.BLACK);
                g2.setStroke(new BasicStroke(1));
                g2.drawRect(p.x - boxW/2, p.y - boxH/2, boxW, boxH);
            }

            // node label
            g2.setColor(Color.BLACK);
            Font orig = g2.getFont();
            Font f = orig.deriveFont(Font.BOLD, 12f);
            g2.setFont(f);
            FontMetrics fm = g2.getFontMetrics(f);
            int textW = fm.stringWidth(node.name());
            g2.drawString(node.name(), p.x - textW/2, p.y - boxH/2 - 8);
            g2.setFont(orig);
        }
    }

    private void drawTrafficLights(Graphics2D g2) {
        for (RoadEnum road : RoadEnum.values()) {
            if (road.getDestination().getType() != NodeType.CROSSROAD) continue;
            Point orig = nodePositions.get(road.getOrigin());
            Point dest = nodePositions.get(road.getDestination());
            if (orig == null || dest == null) continue;

            double dx = dest.x - orig.x; double dy = dest.y - orig.y;
            double len = Math.hypot(dx, dy); if (len == 0) len = 1;
            double ux = dx / len; double uy = dy / len; double px = -uy; double py = ux;

            // compute an offset similar to the road drawing so the light sits beside the lane arriving at dest
            double sideSpacing = 18.0; double intraSpacing = 14.0;
            String a = road.getOrigin().name(), b = road.getDestination().name();
            String key = a.compareTo(b) <= 0 ? a + "|" + b : b + "|" + a;
            String first = a.compareTo(b) <= 0 ? a : b;

            java.util.List<RoadEnum> fwd = new java.util.ArrayList<>();
            java.util.List<RoadEnum> bwd = new java.util.ArrayList<>();
            for (RoadEnum r : RoadEnum.values()) {
                String aa = r.getOrigin().name(), bb = r.getDestination().name();
                String k = aa.compareTo(bb) <= 0 ? aa + "|" + bb : bb + "|" + aa;
                String ff = aa.compareTo(bb) <= 0 ? aa : bb;
                if (!k.equals(key)) continue;
                if (r.getOrigin().name().equals(ff)) fwd.add(r); else bwd.add(r);
            }

            double offset;
            if (road.getOrigin().name().equals(first)) {
                int idx = fwd.indexOf(road);
                offset = sideSpacing + (idx - (fwd.size() - 1) / 2.0) * intraSpacing;
            } else {
                int idx = bwd.indexOf(road);
                offset = -sideSpacing + (idx - (bwd.size() - 1) / 2.0) * intraSpacing;
            }

            // compute intersection point with destination node box
            double hw = 44.0, hh = 28.0;
            double tx = (Math.abs(ux) < 1e-6) ? Double.POSITIVE_INFINITY : (hw / Math.abs(ux));
            double ty = (Math.abs(uy) < 1e-6) ? Double.POSITIVE_INFINITY : (hh / Math.abs(uy));
            double t = Math.min(tx, ty);
            double bx = dest.x - ux * t + px * offset;
            double by = dest.y - uy * t + py * offset;

            int w = 14, h = 28;
            int ix = (int) Math.round(bx - w/2.0);
            int iy = (int) Math.round(by - h/2.0);

            // black housing
            g2.setColor(new Color(40,40,40));
            g2.fillRoundRect(ix, iy, w, h, 4, 4);
            g2.setColor(new Color(30,30,30));
            g2.drawRoundRect(ix, iy, w, h, 4, 4);

            String state = trafficLights.getOrDefault(road, "RED").toLowerCase();
            boolean isRed = state.equals("red");

            Color redOn = new Color(230,60,60);
            Color redOff = new Color(80,40,40);
            Color greenOn = new Color(50,200,80);
            Color greenOff = new Color(40,100,40);

            g2.setColor(isRed ? redOn : redOff);
            g2.fillOval(ix + 2, iy + 4, 10, 10);
            g2.setColor(isRed ? greenOff : greenOn);
            g2.fillOval(ix + 2, iy + 14, 10, 10);

            // label with road name — draw on a small rounded semi-opaque background for readability
            Font origLabelFont = g2.getFont();
            Font labelFont = origLabelFont.deriveFont(Font.BOLD, 12f);
            g2.setFont(labelFont);
            FontMetrics fmLabel = g2.getFontMetrics(labelFont);
            String label = road.name();
            int padding = 6;
            int textW = fmLabel.stringWidth(label);
            int textH = fmLabel.getHeight();
            int labelX = ix + w + 8;
            int labelBaseline = iy + h/2 + fmLabel.getAscent()/2;

            int rectX = labelX - padding;
            int rectY = labelBaseline - fmLabel.getAscent() - padding;
            int rectW = textW + padding * 2;
            int rectH = textH + padding * 2;

            g2.setColor(new Color(255,255,255,230));
            g2.fillRoundRect(rectX, rectY, rectW, rectH, 8, 8);
            g2.setColor(new Color(0,0,0,160));
            g2.drawRoundRect(rectX, rectY, rectW, rectH, 8, 8);

            g2.setColor(Color.BLACK);
            g2.drawString(label, labelX, labelBaseline);
            g2.setFont(origLabelFont);
        }
    }

    private void drawLegend(Graphics2D g2) {
        g2.setColor(new Color(0,0,0,160));
        Font orig = g2.getFont();
        g2.setFont(orig.deriveFont(Font.PLAIN, 12f));
        g2.drawString("Legend: Car=Blue, Truck=Gray, Motorcycle=Orange", 12, 18);
        g2.setFont(orig);
    }

    private void drawArrow(Graphics2D g2, int x, int y, double angle) {
        int size = 10;
        int x2 = x - (int) Math.round(size * Math.cos(angle - 0.4));
        int y2 = y - (int) Math.round(size * Math.sin(angle - 0.4));
        int x3 = x - (int) Math.round(size * Math.cos(angle + 0.4));
        int y3 = y - (int) Math.round(size * Math.sin(angle + 0.4));
        Polygon p = new Polygon();
        p.addPoint(x, y);
        p.addPoint(x2, y2);
        p.addPoint(x3, y3);
        g2.setColor(new Color(220,220,220,200));
        g2.fillPolygon(p);
    }
}
