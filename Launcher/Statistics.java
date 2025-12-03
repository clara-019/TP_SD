package Launcher;

import Node.NodeEnum;
import Vehicle.Vehicle;
import Vehicle.VehicleType;
import java.util.*;

public class Statistics {
    private int totalCreated = 0;
    private int totalExited = 0;
    private long totalTravelTimeMs = 0L;
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

    public synchronized void recordCreatedVehicle(Vehicle v) {
        if (v == null) return;
        this.totalCreated++;
        VehicleType vt = v.getType();
        if (vt != null) this.createdByType.put(vt, this.createdByType.getOrDefault(vt, 0) + 1);
    }

    public synchronized void recordExitedVehicle(Vehicle v) {
        if (v == null) return;
        this.totalExited++;
        VehicleType vt = v.getType();
        if (vt != null) this.exitedByType.put(vt, this.exitedByType.getOrDefault(vt, 0) + 1);
    }

    public synchronized void recordPassedAtNode(NodeEnum node, Vehicle v) {
        if (node == null || v == null) return;
        Map<VehicleType, Integer> m = this.passedByNodeByType.get(node);
        if (m == null) {
            m = new EnumMap<>(VehicleType.class);
            this.passedByNodeByType.put(node, m);
        }
        VehicleType vt = v.getType();
        if (vt != null) m.put(vt, m.getOrDefault(vt, 0) + 1);
    }

    public synchronized void recordDepartureTimestamp(String id) {
        if (id == null) return;
        this.departTimestamps.put(id, System.currentTimeMillis());
    }

    public synchronized Long removeDepartureTimestamp(String id) {
        if (id == null) return null;
        return this.departTimestamps.remove(id);
    }

    public synchronized void recordEntranceTimestamp(String id, long timestamp) {
        if (id == null) return;
        this.entranceTimestamps.put(id, timestamp);
    }

    public synchronized void recordSignalArrival(String id) {
        if (id == null) return;
        this.signalArrivalTimestamps.put(id, System.currentTimeMillis());
    }

    public synchronized Long removeSignalArrival(String id) {
        if (id == null) return null;
        return this.signalArrivalTimestamps.remove(id);
    }

    public synchronized void recordTravelTime(Vehicle v, long ms) {
        if (v == null) return;
        this.totalTravelTimeMs += ms;
        this.completedTrips++;
        VehicleType vt = v.getType();
        if (vt != null) {
            this.totalRoadByType.put(vt, this.totalRoadByType.getOrDefault(vt, 0L) + ms);
            this.roadCountByType.put(vt, this.roadCountByType.getOrDefault(vt, 0) + 1);
        }
    }

    public synchronized void recordTripTimeByType(Vehicle v) {
        if (v == null) return;
        String id = v.getId();
        Long entrance = this.entranceTimestamps.remove(id);
        if (entrance == null || entrance <= 0) entrance = v.getEntranceTime();
        long exit = v.getExitTime();
        if (exit <= 0) exit = System.currentTimeMillis();
        if (entrance == null || entrance <= 0 || exit < entrance) return;
        long travelMs = exit - entrance;
        VehicleType vt = v.getType();
        if (vt == null) return;
        this.totalTripByType.put(vt, this.totalTripByType.getOrDefault(vt, 0L) + travelMs);
        this.tripCountByType.put(vt, this.tripCountByType.getOrDefault(vt, 0) + 1);
        Long prevMin = this.minTripByType.get(vt);
        if (prevMin == null || travelMs < prevMin) this.minTripByType.put(vt, travelMs);
        Long prevMax = this.maxTripByType.get(vt);
        if (prevMax == null || travelMs > prevMax) this.maxTripByType.put(vt, travelMs);
    }

    public synchronized void recordWaitForType(VehicleType vt, long waitMs) {
        if (vt == null) return;
        this.totalWaitByType.put(vt, this.totalWaitByType.getOrDefault(vt, 0L) + waitMs);
        this.waitCountByType.put(vt, this.waitCountByType.getOrDefault(vt, 0) + 1);
    }

    public synchronized int getTotalCreated() { return totalCreated; }
    public synchronized int getTotalExited() { return totalExited; }
    public synchronized long getTotalTravelTimeMs() { return totalTravelTimeMs; }
    public synchronized int getCompletedTrips() { return completedTrips; }

    public synchronized Map<VehicleType, Integer> getCreatedByType() { return new EnumMap<>(createdByType); }
    public synchronized Map<VehicleType, Integer> getExitedByType() { return new EnumMap<>(exitedByType); }
    public synchronized Map<VehicleType, Long> getAvgWaitByType() {
        Map<VehicleType, Long> out = new EnumMap<>(VehicleType.class);
        for (VehicleType vt : VehicleType.values()) {
            long total = totalWaitByType.getOrDefault(vt, 0L);
            int cnt = waitCountByType.getOrDefault(vt, 0);
            out.put(vt, cnt == 0 ? 0L : total / cnt);
        }
        return out;
    }

    public synchronized Map<NodeEnum, Map<VehicleType, Integer>> getPassedByNodeByType() {
        Map<NodeEnum, Map<VehicleType, Integer>> copy = new EnumMap<>(NodeEnum.class);
        for (Map.Entry<NodeEnum, Map<VehicleType, Integer>> e : passedByNodeByType.entrySet()) {
            copy.put(e.getKey(), new EnumMap<>(e.getValue()));
        }
        return copy;
    }

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

    public synchronized Map<VehicleType, long[]> getTripStatsMillis() {
        Map<VehicleType, long[]> out = new EnumMap<>(VehicleType.class);
        for (VehicleType vt : VehicleType.values()) {
            long total = this.totalTripByType.getOrDefault(vt, 0L);
            int cnt = this.tripCountByType.getOrDefault(vt, 0);
            long avg = (cnt == 0) ? 0L : total / cnt;
            long min = this.minTripByType.getOrDefault(vt, 0L);
            long max = this.maxTripByType.getOrDefault(vt, 0L);
            out.put(vt, new long[]{min, avg, max});
        }
        return out;
    }
}
