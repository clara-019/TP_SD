package Launcher;

import Node.NodeEnum;
import Node.NodeType;
import Traffic.RoadEnum;

import java.awt.Point;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.logging.Logger;

public class DashboardModel {
    public static final double NODE_HALF_WIDTH = 44.0;
    public static final double NODE_HALF_HEIGHT = 28.0;
    public static final double QUEUE_SPACING = 34.0;
    public static final double LIGHT_BACKOFF = 10.0;

    public static final long SIGNAL_ARRIVAL_ANIM_MS = 200L;
    public static final long COMPACT_ANIM_MS = 300L;
    private static final Logger LOGGER = Logger.getLogger(DashboardModel.class.getName());

    private final Map<String, VehicleSprite> sprites;
    private final Map<NodeEnum, Point> nodePositions;
    private final Map<RoadEnum, String> trafficLights;
    private final Map<RoadEnum, Deque<VehicleSprite>> signalQueues;
    private final Map<RoadEnum, QueueStats> queueStats;

    public DashboardModel() {
        this.sprites = new ConcurrentHashMap<>();
        this.nodePositions = java.util.Collections.synchronizedMap(new EnumMap<>(NodeEnum.class));
        this.trafficLights = java.util.Collections.synchronizedMap(new EnumMap<>(RoadEnum.class));
        this.signalQueues = new ConcurrentHashMap<>();
        this.queueStats = java.util.Collections.synchronizedMap(new EnumMap<>(RoadEnum.class));

        for (RoadEnum r : RoadEnum.values()) {
            if (r.getDestination().getType() == NodeType.CROSSROAD) {
                trafficLights.put(r, "RED");
                signalQueues.put(r, new ConcurrentLinkedDeque<>());
                queueStats.put(r, new QueueStats());
            }
        }
    }

    public Map<String, VehicleSprite> getSprites() {
        return sprites;
    }

    public Map<NodeEnum, Point> getNodePositions() {
        return nodePositions;
    }

    public Map<RoadEnum, String> getTrafficLights() {
        return trafficLights;
    }

    public Map<RoadEnum, Deque<VehicleSprite>> getSignalQueues() {
        return signalQueues;
    }

    public Map<RoadEnum, QueueStats> getQueueStats() {
        return queueStats;
    }

    public void enqueueToSignal(RoadEnum road, VehicleSprite s) {
        if (road == null || s == null)
            return;
        Deque<VehicleSprite> q = signalQueues.get(road);
        QueueStats st = queueStats.get(road);
        if (q == null)
            return;
        synchronized (q) {
            boolean found = false;
            int pos = 0;
            for (VehicleSprite vs : q) {
                if (vs.id.equals(s.id)) {
                    found = true;
                    break;
                }
                pos++;
            }
            int index = found ? pos : q.size();
            Point origin = nodePositions.get(road.getOrigin());
            Point dest = nodePositions.get(road.getDestination());
            Point signal = computeTrafficPoint(origin, dest, index);
            s.setFaceTarget(dest.x, dest.y);
            s.setTarget(signal.x, signal.y, SIGNAL_ARRIVAL_ANIM_MS);
            LOGGER.fine(() -> "Enqueued sprite " + s.id + " to " + road + " index=" + index);
            if (!found) {
                q.addLast(s);
                if (st != null)
                    st.recordSample(q.size());
            }
        }
    }

    public void removeSpriteFromAllQueues(String id) {
        if (id == null)
            return;
        for (Map.Entry<RoadEnum, Deque<VehicleSprite>> e : signalQueues.entrySet()) {
            Deque<VehicleSprite> q = e.getValue();
            QueueStats st = queueStats.get(e.getKey());
            if (q != null) {
                synchronized (q) {
                    boolean removed = q.removeIf(sp -> sp.id.equals(id));
                    if (removed && st != null) {
                        st.recordSample(q.size());
                        LOGGER.fine(() -> "Removed sprite " + id + " from queue " + e.getKey());
                    }
                }
            }
        }
    }

    public void compactQueue(RoadEnum road) {
        if (road == null)
            return;
        Deque<VehicleSprite> q = signalQueues.get(road);
        if (q == null || q.isEmpty())
            return;
        Point origin = nodePositions.get(road.getOrigin());
        Point dest = nodePositions.get(road.getDestination());
        if (origin == null || dest == null)
            return;
        synchronized (q) {
            int idx = 0;
            for (VehicleSprite vs : q) {
                if (vs == null) {
                    idx++;
                    continue;
                }
                Point p = computeTrafficPoint(origin, dest, idx);
                vs.setTarget(p.x, p.y, COMPACT_ANIM_MS);
                LOGGER.fine(() -> "Compact queue " + road + " sprite=" + (vs == null ? "null" : vs.id) + " -> " + p);
                idx++;
            }
        }
    }

    public static Point computeTrafficPoint(Point origin, Point dest, int index) {
        if (origin == null || dest == null)
            return new Point(0, 0);
        double dx = dest.x - origin.x;
        double dy = dest.y - origin.y;
        double len = Math.hypot(dx, dy);
        if (len == 0)
            len = 1;
        double ux = dx / len;
        double uy = dy / len;

        double hw = NODE_HALF_WIDTH, hh = NODE_HALF_HEIGHT;
        double tx = (Math.abs(ux) < 1e-6) ? Double.POSITIVE_INFINITY : (hw / Math.abs(ux));
        double ty = (Math.abs(uy) < 1e-6) ? Double.POSITIVE_INFINITY : (hh / Math.abs(uy));
        double t = Math.min(tx, ty);
        double bx = dest.x - ux * t;
        double by = dest.y - uy * t;

        double spacing = QUEUE_SPACING;
        double shift = spacing * index;
        double finalX = bx - ux * shift;
        double finalY = by - uy * shift;
        return new Point((int) Math.round(finalX), (int) Math.round(finalY));
    }

    @Override
    public String toString() {
        return "DashboardModel[sprites=" + sprites.size() + ", queues=" + signalQueues.size() + "]";
    }
}
