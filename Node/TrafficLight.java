package Node;

import java.util.List;

import Comunication.Sender;
import Event.SignalChangeEvent;
import Utils.*;
import Vehicle.Vehicle;

public class TrafficLight extends Thread {
    private static final int GREEN_LIGHT_DURATION_MS = 5000;
    private static final int RED_LIGHT_DURATION_MS = 5000;
    private static final int TIME_TO_PASS_MS = 1000;

    private SynchronizedQueue<Vehicle> vehicleQueue;
    private RoadEnum road;
    private LogicalClock clock;

    public TrafficLight(SynchronizedQueue<Vehicle> vehicleQueue, RoadEnum road, LogicalClock clock) {
        this.vehicleQueue = vehicleQueue;
        this.road = road;
        this.clock = clock;
    }

    @Override
    public void run() {
        NodeEnum currentNode = road.getDestination();
        while (true) {
            try {
                // Green Light
                System.out.println("Traffic Light GREEN for Traffic Light: " + road.toString());
                long greenStartTime = System.currentTimeMillis();
                Sender.sendToEventHandler(new SignalChangeEvent(currentNode, greenStartTime, "Green"));
                long greenEndTime = greenStartTime + GREEN_LIGHT_DURATION_MS;
                
                while (System.currentTimeMillis() < greenEndTime) {
                    Vehicle vehicle = vehicleQueue.remove();
                    if (vehicle != null) {
                        Thread.sleep(vehicle.getType().getTimeToPass(TIME_TO_PASS_MS));

                        List<NodeEnum> path = vehicle.getPath().getPath();

                        NodeEnum nextNode = path.get(path.indexOf(currentNode) + 1);
                        Sender.sendVehicleDeparture(vehicle, nextNode.getPort(), currentNode, clock);

                        System.out.println(
                                "Vehicle " + vehicle.getId() + " is passing through Traffic Light: " + road.toString());
                        System.out.println("queue size: " + vehicleQueue.isEmpty());
                    }
                }

                // Red Light
                System.out.println("Traffic Light RED for Traffic Light: " + road.toString());
                Sender.sendToEventHandler(new SignalChangeEvent(currentNode, System.currentTimeMillis(), "Red"));
                Thread.sleep(RED_LIGHT_DURATION_MS);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}