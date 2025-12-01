package Traffic;

import java.util.AbstractMap.SimpleEntry;

import Comunication.Sender;
import Event.*;
import Utils.*;
import Vehicle.*;

/**
 * Thread responsible for managing vehicles passing through a road segment.
 * <p>
 * The PassRoad thread consumes arriving vehicles from an incoming
 * {@link SynchronizedQueue}, schedules their simulated passage time
 * based on vehicle type and road travel time, and when the scheduled time
 * is reached it emits a {@link VehicleEvent} of type
 * {@link EventType#VEHICLE_SIGNAL_ARRIVAL} to the central event
 * handler. Vehicles that have finished crossing are added to the
 * {@code passedQueue} for downstream processing.
 */
public class PassRoad extends Thread {
    private static final long DELAY_BETWEEN_PASSES_MS = 200;

    private final SynchronizedQueue<Vehicle> arrivingQueue;
    private final SynchronizedQueue<Vehicle> passedQueue;
    private final RoadEnum road;
    private final LogicalClock clock;
    private final SynchronizedQueue<SimpleEntry<Long, Vehicle>> passingQueue = new SynchronizedQueue<>();

    /**
     * Create a PassRoad runner.
     *
     * @param arrivingQueue queue where arriving vehicles are enqueued
     * @param passedQueue   queue where vehicles that finished passing are added
     * @param road          the {@link RoadEnum} this PassRoad simulates
     * @param clock         logical clock used for event timestamps
     */
    public PassRoad(SynchronizedQueue<Vehicle> arrivingQueue,
            SynchronizedQueue<Vehicle> passedQueue,
            RoadEnum road,
            LogicalClock clock) {
        this.arrivingQueue = arrivingQueue;
        this.passedQueue = passedQueue;
        this.road = road;
        this.clock = clock;
    }

    /**
     * Main loop: polls for newly arrived vehicles, schedules their pass
     * times and triggers processing when the scheduled time is reached.
     */
    @Override
    public void run() {
        while (true) {
            try {
                Vehicle vehicle = this.arrivingQueue.poll();
                if (vehicle != null) {
                    this.processNewArrival(vehicle);
                }

                SimpleEntry<Long, Vehicle> entry = this.passingQueue.peek();
                if (entry != null && System.currentTimeMillis() >= entry.getKey()) {
                    this.processPassedRoad();
                } else {
                    Thread.sleep(50);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Schedule a newly arrived vehicle by computing its expected passing
     * timestamp and inserting it into the internal timed queue. If the
     * computed timestamp would overtake the last scheduled vehicle, a
     * small delay is applied to keep vehicle order.
     *
     * @param vehicle vehicle that just arrived
     */
    private void processNewArrival(Vehicle vehicle) {
        SimpleEntry<Long, Vehicle> last = this.passingQueue.peekLast();
        long travelTime = System.currentTimeMillis()
                + vehicle.getType().getTimeToPass(road.getTime());

        if (last != null && travelTime < last.getKey()) {
            long corrected = last.getKey() + DELAY_BETWEEN_PASSES_MS;
            this.passingQueue.add(new SimpleEntry<>(corrected, vehicle));
        } else {
            this.passingQueue.add(new SimpleEntry<>(travelTime, vehicle));
        }
    }

    /**
     * Process the next vehicle whose scheduled pass time has arrived.
     * Sends a {@link VehicleEvent} to the event handler indicating the
     * vehicle's arrival at the next signal/destination and enqueues the
     * vehicle into {@code passedQueue} for downstream components.
     */
    private void processPassedRoad() {
        SimpleEntry<Long, Vehicle> entry = this.passingQueue.remove();
        Vehicle v = entry.getValue();
        Sender.sendToEventHandler(
                new VehicleEvent(EventType.VEHICLE_SIGNAL_ARRIVAL, road.getDestination(), clock.tick(), v));
        passedQueue.add(v);
    }
}
