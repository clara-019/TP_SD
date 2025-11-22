package Node;

import java.util.AbstractMap.SimpleEntry;

import Utils.*;
import Vehicle.*;

public class PassRoad extends Thread {
    private static final long DELAY_BETWEEN_PASSES_MS = 100;
    private SynchronizedQueue<Vehicle> arrivingQueue;
    private SynchronizedQueue<Vehicle> passedQueue;
    private RoadEnum road;

    public PassRoad(SynchronizedQueue<Vehicle> arrivingQueue, SynchronizedQueue<Vehicle> passedQueue, RoadEnum road) {
        this.arrivingQueue = arrivingQueue;
        this.passedQueue = passedQueue;
        this.road = road;
    }

    @Override
    public void run() {
        SynchronizedQueue<SimpleEntry<Long, Vehicle>> vehicleQueue = new SynchronizedQueue<>();
        while (true) {
            try {
                Vehicle vehicle = arrivingQueue.remove();
                if (vehicle != null) {
                    SimpleEntry<Long, Vehicle> entry = vehicleQueue.peekLast();
                    long timeToTravel = System.currentTimeMillis() + vehicle.getType().getTimeToPass(road.getTime());
                    if (entry != null && timeToTravel < entry.getKey()) {
                        long lastVehicleTime = entry.getKey();
                        vehicleQueue.add(new SimpleEntry<>(lastVehicleTime + DELAY_BETWEEN_PASSES_MS, vehicle));
                    } else {
                        vehicleQueue.add(new SimpleEntry<>(timeToTravel, vehicle));
                    }
                }
                SimpleEntry<Long, Vehicle> entry = vehicleQueue.peek();
                if (entry != null && System.currentTimeMillis() >= entry.getKey()) {
                    vehicleQueue.remove();
                    Vehicle passedVehicle = entry.getValue();
                    System.out.println(
                            "Vehicle " + passedVehicle.getId() + " has passed through Road: " + road.toString());
                    passedQueue.add(passedVehicle);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
