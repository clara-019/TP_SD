package Launcher;

import Node.*;
import Traffic.RoadEnum;

import java.awt.Point;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Logger;

/**
 * Model holding the visual and traffic-related state for the dashboard.
 * <p>
 * This class provides thread-safe access to the set of active
 * {@link VehicleSprite} objects, node positions, traffic light states,
 * and per-road signal queues and statistics. It contains helpers to
 * enqueue sprites into signal queues, remove sprites, compact queues
 * visually and compute queue positions in front of destination nodes.
 */
public class MapModel {
    public static final double NODE_HALF_WIDTH = 44.0;
    public static final double NODE_HALF_HEIGHT = 28.0;
    public static final double QUEUE_SPACING = 34.0;
    public static final double LIGHT_BACKOFF = 10.0;

    public static final long SIGNAL_ARRIVAL_ANIM_MS = 200L;
    public static final long COMPACT_ANIM_MS = 300L;
    private static final Logger LOGGER = Logger.getLogger(MapModel.class.getName());

    private final Map<String, VehicleSprite> sprites;
    private final Map<NodeEnum, Point> nodePositions;
    private final Map<RoadEnum, String> trafficLights;
    private final Map<RoadEnum, Deque<VehicleSprite>> signalQueues;
    private final Map<RoadEnum, QueueStats> queueStats;

    /**
     * Create a new MapModel and initialize per-road structures.
     *
     * <p>
     * Traffic lights, signal queues and queue statistics are created
     * for roads that lead to crossroads. Collections are chosen to be
     * safe for concurrent access from simulator and UI threads.
     */
    public MapModel() {
        this.sprites = new ConcurrentHashMap<>();
        this.nodePositions = java.util.Collections.synchronizedMap(new EnumMap<>(NodeEnum.class));
        this.trafficLights = java.util.Collections.synchronizedMap(new EnumMap<>(RoadEnum.class));
        this.signalQueues = new ConcurrentHashMap<>();
        this.queueStats = java.util.Collections.synchronizedMap(new EnumMap<>(RoadEnum.class));

        for (RoadEnum r : RoadEnum.values()) {
            if (r.getDestination().getType() == NodeType.CROSSROAD) {
                this.trafficLights.put(r, "RED");
                this.signalQueues.put(r, new ConcurrentLinkedDeque<>());
                this.queueStats.put(r, new QueueStats());
            }
        }
    }

    /**
     * Return the live sprite map keyed by vehicle id.
     *
     * @return map of id &rarr; {@link VehicleSprite}
     */
    public Map<String, VehicleSprite> getSprites() {
        return this.sprites;
    }

    /**
     * Return a synchronized map of node enum to canvas positions.
     *
     * @return map of {@link NodeEnum} to {@link Point}
     */
    public Map<NodeEnum, Point> getNodePositions() {
        return this.nodePositions;
    }

    /**
     * Return the current traffic light states for each road.
     *
     * @return map of {@link RoadEnum} to signal color string (e.g. "RED")
     */
    public Map<RoadEnum, String> getTrafficLights() {
        return this.trafficLights;
    }

    /**
     * Return the per-road signal queues holding sprites waiting at signals.
     *
     * @return map of {@link RoadEnum} to deque of {@link VehicleSprite}
     */
    public Map<RoadEnum, Deque<VehicleSprite>> getSignalQueues() {
        return this.signalQueues;
    }

    /**
     * Return queue sampling statistics for each road.
     *
     * @return map of {@link RoadEnum} to {@link QueueStats}
     */
    public Map<RoadEnum, QueueStats> getQueueStats() {
        return this.queueStats;
    }

    /**
     * Enqueue a sprite to the signal queue for the given road.
     * <p>
     * If the sprite is already present in the queue it will not be
     * duplicated; instead the existing index is used to compute the
     * visual target point. The sprite is moved (animated) to the
     * computed traffic point in front of the destination node. Queue
     * sampling statistics are updated when a new sprite is appended.
     *
     * @param road the road whose signal queue to use
     * @param s    the sprite to enqueue
     */
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

            s.setTarget(signal.x, signal.y, SIGNAL_ARRIVAL_ANIM_MS);
            LOGGER.fine(() -> "Enqueued sprite " + s.id + " to " + road + " index=" + index);
            if (!found) {
                q.addLast(s);
                if (st != null)
                    st.recordSample(q.size());
            }
        }
    }

    /**
     * Remove a sprite with the given id from all signal queues.
     *
     * @param id the sprite (vehicle) id to remove
     */
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

    /**
     * Compact the visual queue for a road by repositioning each sprite to
     * its canonical traffic point based on the current queue order.
     *
     * @param road the road whose queue should be compacted
     */
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

    /**
     * Compute the on-canvas point where queued vehicles should stop in
     * front of the destination node.
     *
     * @param origin the origin node canvas point
     * @param dest   the destination node canvas point
     * @param index  the zero-based position in the queue (0 = first)
     * @return the computed canvas point where the sprite should be positioned
     */
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
