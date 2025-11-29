package Traffic;

import java.util.AbstractMap.SimpleEntry;

import Comunication.Sender;
import Event.*;
import Utils.*;
import Vehicle.*;

public class PassRoad extends Thread {

    private static final long DELAY_BETWEEN_PASSES_MS = 200;

    private final SynchronizedQueue<Vehicle> arrivingQueue;
    private final SynchronizedQueue<Vehicle> passedQueue;
    private final RoadEnum road;
    private final LogicalClock clock;

    public PassRoad(SynchronizedQueue<Vehicle> arrivingQueue,
            SynchronizedQueue<Vehicle> passedQueue,
            RoadEnum road,
            LogicalClock clock) {
        this.arrivingQueue = arrivingQueue;
        this.passedQueue = passedQueue;
        this.road = road;
        this.clock = clock;
    }

    @Override
    public void run() {

        SynchronizedQueue<SimpleEntry<Long, Vehicle>> vehicleQueue = new SynchronizedQueue<>();

        while (true) {
            try {
                Vehicle vehicle = arrivingQueue.poll();

                if (vehicle != null) {
                    SimpleEntry<Long, Vehicle> last = vehicleQueue.peekLast();
                    long travelTime = System.currentTimeMillis()
                            + vehicle.getType().getTimeToPass(road.getTime());

                    if (last != null && travelTime < last.getKey()) {
                        long corrected = last.getKey() + DELAY_BETWEEN_PASSES_MS;
                        vehicleQueue.add(new SimpleEntry<>(corrected, vehicle));
                    } else {
                        vehicleQueue.add(new SimpleEntry<>(travelTime, vehicle));
                    }
                }

                SimpleEntry<Long, Vehicle> entry = vehicleQueue.peek();

                if (entry != null && System.currentTimeMillis() >= entry.getKey()) {
                    vehicleQueue.remove();
                    Vehicle v = entry.getValue();
                    Sender.sendToEventHandler(
                            new VehicleEvent(EventType.VEHICLE_SIGNAL_ARRIVAL, v, road.getDestination(), clock.tick()));
                    passedQueue.add(v);
                } else {
                    Thread.sleep(50);
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
