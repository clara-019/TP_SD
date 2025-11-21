package Node.Crossroad;

import java.util.*;

import Comunication.*;
import Road.*;
import Utils.SynchronizedQueue;
import Vehicle.Vehicle;
import Node.*;
import Event.*;

public class Crossroad {
    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("Please provide a crossroad string as an argument.");
            return;
        }
        String crossId = args[0];
        NodeEnum crossroad = NodeEnum.toNodeEnum(crossId);
        List<RoadEnum> roadsToCrossroad = RoadEnum.getRoadsToCrossroad(crossroad);
        List<RoadEnum> roadsFromCrossroad = RoadEnum.getRoadsFromCrossroad(crossroad);

        Map<RoadEnum, SynchronizedQueue<Vehicle>> trafficQueues = new HashMap<>();
        Map<RoadEnum, SynchronizedQueue<Vehicle>> exitQueues = new HashMap<>();

        SynchronizedQueue<Vehicle> vehiclesToSort = new SynchronizedQueue<>();
        SynchronizedQueue<Vehicle> vehicleToExitQueue = new SynchronizedQueue<>();

        for (RoadEnum road : roadsToCrossroad) {
            SynchronizedQueue<Vehicle> vehicleQueue = new SynchronizedQueue<>();
            TrafficLight trafficLight = new TrafficLight(vehicleToExitQueue, vehicleQueue, road);
            trafficQueues.put(road, vehicleQueue);
            trafficLight.start();
        }

        for (RoadEnum road : roadsFromCrossroad) {
            SynchronizedQueue<Vehicle> exitQueue = new SynchronizedQueue<>();
            exitQueues.put(road, exitQueue);
            int destPort = road.getDestination().getPort();
            new Sender(exitQueue, destPort, EventType.VEHICLE_DEPARTURE, crossroad).start();
        }

        new Receiver(vehiclesToSort, crossroad.toString(), crossroad.getPort()).start();
        new TrafficSorter(trafficQueues, vehiclesToSort, crossroad).start();
        new ExitSorter(exitQueues, vehicleToExitQueue, crossroad).start();
    }
}