package Node;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import Comunication.*;
import Event.*;
import Utils.*;
import Vehicle.Vehicle;

public class Exit {
    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("Please provide a crossroad string as an argument.");
            return;
        }
        String exitId = args[0];
        NodeEnum exit = NodeEnum.toNodeEnum(exitId);

        if (exit == null || exit.getType() != NodeType.EXIT) {
            System.out.println("Invalid exit node: " + exitId);
            return;
        }

        LogicalClock clock = new LogicalClock();
        List<RoadEnum> roadsToExit = RoadEnum.getRoadsToCrossroad(exit);
        Map<RoadEnum, SynchronizedQueue<Vehicle>> trafficQueues = new HashMap<>();
        SynchronizedQueue<Vehicle> incommingQueue = new SynchronizedQueue<>();
        SynchronizedQueue<Vehicle> passedQueue = new SynchronizedQueue<>();

        for (RoadEnum road : roadsToExit) {
            SynchronizedQueue<Vehicle> trafficQueue = new SynchronizedQueue<>();

            PassRoad passRoad = new PassRoad(trafficQueue, passedQueue, road);

            trafficQueues.put(road, trafficQueue);

            passRoad.start();
        }

        new Receiver(incommingQueue, exit.getPort(), exit, clock).start();
        new TrafficSorter(trafficQueues, incommingQueue, exit).start();

        while (true) {
            Vehicle vehicle = passedQueue.remove();
            Sender.sendToEventHandler(new VehicleEvent(EventType.VEHICLE_EXIT, vehicle, exit, clock.tick()));
        }

    }
}
