package Node;

import Utils.SynchronizedQueue;
import Vehicle.Vehicle;

import java.util.*;

public class TrafficSorter extends Thread {
    private List<SynchronizedQueue<Vehicle>> queues;
    private SynchronizedQueue<Vehicle> vehiclesToSort;
    private NodeEnum node;

    public TrafficSorter(List<SynchronizedQueue<Vehicle>> queues, SynchronizedQueue<Vehicle> vehiclesToSort,
            NodeEnum node) {
        this.queues = queues;
        this.vehiclesToSort = vehiclesToSort;
        this.node = node;
    }

    public void start() {

        while (true) {
            try {
                Vehicle vehicle = vehiclesToSort.remove();

                for (SynchronizedQueue<Vehicle> queue : queues) {
                    if (queue.getRoad() != null
                            && vehicle.getOriginRoad().toString().equals(queue.getRoad().toString())) {
                        queue.add(vehicle);
                        break;
                    }
                }

            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
