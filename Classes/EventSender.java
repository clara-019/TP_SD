package Classes;

import java.util.*;

import Enums.*;

public class VehicleReceiver extends Thread {

    private CrossroadEnum crossroad;
    private RoadEnum road;
    private List<SynchronizedQueue<Vehicle>> queues;

    public VehicleReceiver(List<SynchronizedQueue<Vehicle>> queues, CrossroadEnum crossroad) {
        this.queues = queues;
        this.crossroad = crossroad;
        this.road = null;
    }

    public VehicleReceiver(List<SynchronizedQueue<Vehicle>> queues, RoadEnum road) {
        this.queues = queues;
        this.road = road;
        this.crossroad = null;
    }

    @Override
    public void run() {
        while (true) {


            for (SynchronizedQueue<Vehicle> queue : queues) {
                Vehicle vehicle = queue.remove();
                if (vehicle != null) {
                    System.out.println("Vehicle " + vehicle.getId() + " has crossed the " + crossroad);
                }
            }
        }
    }

}