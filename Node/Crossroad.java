package Node;

import java.util.*;

import Comunication.*;
import Utils.LogicalClock;
import Utils.RoundRobin;
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
        RoundRobin roundRobin = null;
        if (roadsToCrossroad.size() >= 2) {

            roundRobin = new RoundRobin(roadsToCrossroad.size());
            Map<RoadEnum, SynchronizedQueue<Vehicle>> trafficQueues = new HashMap<>();
            Map<RoadEnum, SynchronizedQueue<Vehicle>> passedQueues = new HashMap<>();

            SynchronizedQueue<Vehicle> vehiclesToSort = new SynchronizedQueue<>();

            for (RoadEnum road : roadsToCrossroad) {
                SynchronizedQueue<Vehicle> vehicleQueue = new SynchronizedQueue<>();
                SynchronizedQueue<Vehicle> passedQueue = new SynchronizedQueue<>();

                trafficQueues.put(road, vehicleQueue);
                passedQueues.put(road, passedQueue);

                PassRoad passRoad = new PassRoad(vehicleQueue, passedQueue, road);
                TrafficLight trafficLight = new TrafficLight(passedQueue, road, clock, roundRobin);
                passRoad.start();
                trafficLight.start();
            }

            new Receiver(vehiclesToSort, crossroad.getPort(), crossroad, clock).start();
            new TrafficSorter(trafficQueues, vehiclesToSort, crossroad).start();
        } else {
            roundRobin = new RoundRobin(2);
            SynchronizedQueue<Vehicle> arrivingQueue = new SynchronizedQueue<>();
            SynchronizedQueue<Vehicle> passedQueue = new SynchronizedQueue<>();

            new PassRoad(arrivingQueue, passedQueue, roadsToCrossroad.get(0)).start();
            new TrafficLight(passedQueue, roadsToCrossroad.get(0), clock, roundRobin).start();
            new PedestrianLight(roundRobin, 1).start();


            new Receiver(arrivingQueue, crossroad.getPort(), crossroad, clock).start();
        }
    }
}
