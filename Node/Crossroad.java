package Node;

import java.util.*;

import Comunication.*;
import Road.*;
import Utils.LogicalClock;
import Utils.SynchronizedQueue;
import Vehicle.Vehicle;

public class Crossroad {
    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("Please provide a crossroad string as an argument.");
            return;
        }
        String crossId = args[0];
        NodeEnum crossroad = NodeEnum.toNodeEnum(crossId);

        if (crossroad == null || crossroad.getType() != NodeType.CROSSROAD) {
            System.out.println("Invalid crossroad node: " + crossId);
            return;
        }

        LogicalClock clock = new LogicalClock();

        List<RoadEnum> roadsToCrossroad = RoadEnum.getRoadsToCrossroad(crossroad);

        Map<RoadEnum, SynchronizedQueue<Vehicle>> trafficQueues = new HashMap<>();

        SynchronizedQueue<Vehicle> vehiclesToSort = new SynchronizedQueue<>();

        for (RoadEnum road : roadsToCrossroad) {
            SynchronizedQueue<Vehicle> vehicleQueue = new SynchronizedQueue<>();
            TrafficLight trafficLight = new TrafficLight(vehicleQueue, road, clock);
            trafficQueues.put(road, vehicleQueue);
            trafficLight.start();
        }

        new Receiver(vehiclesToSort, crossroad.getPort(), crossroad, clock).start();
        new TrafficSorter(trafficQueues, vehiclesToSort, crossroad).start();
    }
}