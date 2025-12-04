package Launcher;

import Node.NodeEnum;
import Vehicle.*;
import java.util.*;

/**
 * Collects runtime statistics about vehicles and their trips.
 * <p>
 * This thread-safe helper aggregates counts and timing information
 * produced by the simulator: total vehicles created/exited, per-type
 * counters, wait/road/trip timing aggregates, and per-node pass counts.
 * Methods are synchronized to allow safe concurrent updates from
 * simulator threads and reads from the UI thread.
 */
public class Statistics {
    private int totalCreated = 0;
    private int totalExited = 0;
    private int completedTrips = 0;

    private final Map<String, Long> departTimestamps = new HashMap<>();
    private final Map<String, Long> entranceTimestamps = new HashMap<>();
    private final Map<String, Long> signalArrivalTimestamps = new HashMap<>();

    private final Map<VehicleType, Integer> createdByType = new EnumMap<>(VehicleType.class);
    private final Map<VehicleType, Integer> exitedByType = new EnumMap<>(VehicleType.class);
    private final Map<VehicleType, Long> totalWaitByType = new EnumMap<>(VehicleType.class);
    private final Map<VehicleType, Integer> waitCountByType = new EnumMap<>(VehicleType.class);
    private final Map<VehicleType, Long> totalRoadByType = new EnumMap<>(VehicleType.class);
    private final Map<VehicleType, Integer> roadCountByType = new EnumMap<>(VehicleType.class);
    private final Map<VehicleType, Long> totalTripByType = new EnumMap<>(VehicleType.class);
    private final Map<VehicleType, Integer> tripCountByType = new EnumMap<>(VehicleType.class);
    private final Map<VehicleType, Long> minTripByType = new EnumMap<>(VehicleType.class);
    private final Map<VehicleType, Long> maxTripByType = new EnumMap<>(VehicleType.class);

    private final Map<NodeEnum, Map<VehicleType, Integer>> passedByNodeByType = new EnumMap<>(NodeEnum.class);

    /**
     * Record that a vehicle was created.
     *
     * @param v the created vehicle (may be null)
     */
    public synchronized void recordCreatedVehicle(Vehicle v) {
        if (v == null)
            return;
        this.totalCreated++;
        VehicleType vt = v.getType();
        if (vt != null)
            this.createdByType.put(vt, this.createdByType.getOrDefault(vt, 0) + 1);
    }

    /**
     * Record that a vehicle has exited the simulation.
     *
     * @param v the vehicle that exited (may be null)
     */
    public synchronized void recordExitedVehicle(Vehicle v) {
        if (v == null)
            return;
        this.totalExited++;
        VehicleType vt = v.getType();
        if (vt != null)
            this.exitedByType.put(vt, this.exitedByType.getOrDefault(vt, 0) + 1);
    }

    /**
     * Increment the counter for a vehicle passing a particular node.
     *
     * @param node the node passed
     * @param v    the vehicle that passed the node
     */
    public synchronized void recordPassedAtNode(NodeEnum node, Vehicle v) {
        if (node == null || v == null)
            return;
        Map<VehicleType, Integer> m = this.passedByNodeByType.get(node);
        if (m == null) {
            m = new EnumMap<>(VehicleType.class);
            this.passedByNodeByType.put(node, m);
        }
        VehicleType vt = v.getType();
        if (vt != null)
            m.put(vt, m.getOrDefault(vt, 0) + 1);
    }

    /**
     * Record a departure timestamp for the given vehicle id (now).
     *
     * @param id vehicle id
     */
    public synchronized void recordDepartureTimestamp(String id) {
        if (id == null)
            return;
        this.departTimestamps.put(id, System.currentTimeMillis());
    }

    /**
     * Remove and return a previously recorded departure timestamp.
     *
     * @param id vehicle id
     * @return the recorded departure timestamp in ms or null if none
     */
    public synchronized Long removeDepartureTimestamp(String id) {
        if (id == null)
            return null;
        return this.departTimestamps.remove(id);
    }

    /**
     * Record the entrance timestamp for a vehicle.
     *
     * @param id        vehicle id
     * @param timestamp entrance epoch milliseconds
     */
    public synchronized void recordEntranceTimestamp(String id, long timestamp) {
        if (id == null)
            return;
        this.entranceTimestamps.put(id, timestamp);
    }

    /**
     * Record the time when a vehicle arrived at a traffic signal.
     *
     * @param id vehicle id
     */
    public synchronized void recordSignalArrival(String id) {
        if (id == null)
            return;
        this.signalArrivalTimestamps.put(id, System.currentTimeMillis());
    }

    /**
     * Remove and return the recorded signal arrival timestamp for a vehicle.
     *
     * @param id vehicle id
     * @return timestamp in ms or null if none
     */
    public synchronized Long removeSignalArrival(String id) {
        if (id == null)
            return null;
        return this.signalArrivalTimestamps.remove(id);
    }

    /**
     * Record travel time for a vehicle's road traversal.
     *
     * @param v  the vehicle
     * @param ms travel duration in milliseconds
     */
    public synchronized void recordTravelTime(Vehicle v, long ms) {
        if (v == null)
            return;
        this.completedTrips++;
        VehicleType vt = v.getType();
        if (vt != null) {
            this.totalRoadByType.put(vt, this.totalRoadByType.getOrDefault(vt, 0L) + ms);
            this.roadCountByType.put(vt, this.roadCountByType.getOrDefault(vt, 0) + 1);
        }
    }

    /**
     * Compute and record trip statistics (min/avg/max) for the vehicle's type
     * based on its entrance and exit timestamps.
     *
     * @param v the vehicle whose trip should be recorded
     */
    public synchronized void recordTripTimeByType(Vehicle v) {
        if (v == null)
            return;
        String id = v.getId();
        Long entrance = this.entranceTimestamps.remove(id);
        if (entrance == null || entrance <= 0)
            entrance = v.getEntranceTime();
        long exit = v.getExitTime();
        if (exit <= 0)
            exit = System.currentTimeMillis();
        if (entrance == null || entrance <= 0 || exit < entrance)
            return;
        long travelMs = exit - entrance;
        VehicleType vt = v.getType();
        if (vt == null)
            return;
        this.totalTripByType.put(vt, this.totalTripByType.getOrDefault(vt, 0L) + travelMs);
        this.tripCountByType.put(vt, this.tripCountByType.getOrDefault(vt, 0) + 1);
        Long prevMin = this.minTripByType.get(vt);
        if (prevMin == null || travelMs < prevMin)
            this.minTripByType.put(vt, travelMs);
        Long prevMax = this.maxTripByType.get(vt);
        if (prevMax == null || travelMs > prevMax)
            this.maxTripByType.put(vt, travelMs);
    }

    /**
     * Record a wait duration at a signal for a given vehicle type.
     *
     * @param vt     vehicle type
     * @param waitMs wait duration in milliseconds
     */
    public synchronized void recordWaitForType(VehicleType vt, long waitMs) {
        if (vt == null)
            return;
        this.totalWaitByType.put(vt, this.totalWaitByType.getOrDefault(vt, 0L) + waitMs);
        this.waitCountByType.put(vt, this.waitCountByType.getOrDefault(vt, 0) + 1);
    }

    /**
     * Return total number of created vehicles so far.
     */
    public synchronized int getTotalCreated() {
        return totalCreated;
    }

    /**
     * Return total number of vehicles that have exited so far.
     */
    public synchronized int getTotalExited() {
        return totalExited;
    }

    /**
     * Return the number of completed trip recordings.
     */
    public synchronized int getCompletedTrips() {
        return completedTrips;
    }

    /**
     * Return a snapshot map of vehicles created grouped by type.
     *
     * @return copy of created-by-type counts
     */
    public synchronized Map<VehicleType, Integer> getCreatedByType() {
        return new EnumMap<>(createdByType);
    }

    /**
     * Return a snapshot map of vehicles exited grouped by type.
     *
     * @return copy of exited-by-type counts
     */
    public synchronized Map<VehicleType, Integer> getExitedByType() {
        return new EnumMap<>(exitedByType);
    }

    /**
     * Return average wait times (in milliseconds) per vehicle type.
     *
     * @return map of vehicle type to average wait in milliseconds
     */
    public synchronized Map<VehicleType, Long> getAvgWaitByType() {
        Map<VehicleType, Long> out = new EnumMap<>(VehicleType.class);
        for (VehicleType vt : VehicleType.values()) {
            long total = totalWaitByType.getOrDefault(vt, 0L);
            int cnt = waitCountByType.getOrDefault(vt, 0);
            out.put(vt, cnt == 0 ? 0L : total / cnt);
        }
        return out;
    }

    /**
     * Return a snapshot of counts of vehicles that passed each node grouped by
     * type.
     *
     * @return map of node -> (map of vehicle type -> count)
     */
    public synchronized Map<NodeEnum, Map<VehicleType, Integer>> getPassedByNodeByType() {
        Map<NodeEnum, Map<VehicleType, Integer>> copy = new EnumMap<>(NodeEnum.class);
        for (Map.Entry<NodeEnum, Map<VehicleType, Integer>> e : passedByNodeByType.entrySet()) {
            copy.put(e.getKey(), new EnumMap<>(e.getValue()));
        }
        return copy;
    }

    /**
     * Return the average road traversal time (in seconds) per vehicle type.
     *
     * @return map of vehicle type to average road time in seconds
     */
    public synchronized Map<VehicleType, Double> getAvgRoadByTypeSeconds() {
        Map<VehicleType, Double> out = new EnumMap<>(VehicleType.class);
        for (VehicleType vt : VehicleType.values()) {
            long total = totalRoadByType.getOrDefault(vt, 0L);
            int cnt = roadCountByType.getOrDefault(vt, 0);
            double avg = (cnt == 0) ? 0.0 : (total / 1000.0 / cnt);
            out.put(vt, avg);
        }
        return out;
    }

    /**
     * Return trip statistics (min, avg, max) in milliseconds per vehicle type.
     *
     * @return map of vehicle type to long[] {min, avg, max}
     */
    public synchronized Map<VehicleType, long[]> getTripStatsMillis() {
        Map<VehicleType, long[]> out = new EnumMap<>(VehicleType.class);
        for (VehicleType vt : VehicleType.values()) {
            long total = this.totalTripByType.getOrDefault(vt, 0L);
            int cnt = this.tripCountByType.getOrDefault(vt, 0);
            long avg = (cnt == 0) ? 0L : total / cnt;
            long min = this.minTripByType.getOrDefault(vt, 0L);
            long max = this.maxTripByType.getOrDefault(vt, 0L);
            out.put(vt, new long[] { min, avg, max });
        }
        return out;
    }

    /**
     * Return the total accumulated trip time (milliseconds) across all types.
     */
    public synchronized long getTotalTripTimeMs() {
        long sum = 0L;
        for (Long v : this.totalTripByType.values()) {
            if (v != null)
                sum += v;
        }
        return sum;
    }

    /**
     * Return the total number of trips recorded across all vehicle types.
     */
    public synchronized int getTotalTripCount() {
        int sum = 0;
        for (Integer c : this.tripCountByType.values()) {
            if (c != null)
                sum += c;
        }
        return sum;
    }

    /**
     * Return overall trip statistics (min, avg, max) in milliseconds aggregated
     * across all types.
     *
     * @return long[] {min, avg, max} in milliseconds
     */
    public synchronized long[] getOverallTripStatsMillis() {
        long total = 0L;
        int count = 0;
        long min = Long.MAX_VALUE;
        long max = 0L;
        for (VehicleType vt : VehicleType.values()) {
            long t = this.totalTripByType.getOrDefault(vt, 0L);
            int c = this.tripCountByType.getOrDefault(vt, 0);
            if (c > 0) {
                total += t;
                count += c;
            }
            long m = this.minTripByType.getOrDefault(vt, 0L);
            long M = this.maxTripByType.getOrDefault(vt, 0L);
            if (m > 0 && m < min)
                min = m;
            if (M > max)
                max = M;
        }
        if (count == 0)
            return new long[] { 0L, 0L, 0L };
        long avg = total / count;
        if (min == Long.MAX_VALUE)
            min = 0L;
        return new long[] { min, avg, max };
    }
}
