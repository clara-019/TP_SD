package Node;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import Comunication.*;
import Road.RoadEnum;
import Utils.SynchronizedQueue;
import Vehicle.Vehicle;

public class Exit {
    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("Please provide a crossroad string as an argument.");
            return;
        }
        String exitId = args[0];
        NodeEnum exit = NodeEnum.toNodeEnum(exitId);
        List<RoadEnum> roadsToCrossroad = RoadEnum.getRoadsToCrossroad(exit);

        Map<RoadEnum, SynchronizedQueue<Vehicle>> trafficQueues = new HashMap<>();

        SynchronizedQueue<Vehicle> vehiclesToSort = new SynchronizedQueue<>();
        SynchronizedQueue<Vehicle> vehicleToExitQueue = new SynchronizedQueue<>();
        SynchronizedQueue<Vehicle> sampedVehiclesQueue = new SynchronizedQueue<>();

        for (RoadEnum road : roadsToCrossroad) {
            SynchronizedQueue<Vehicle> vehicleQueue = new SynchronizedQueue<>();
            TrafficLight trafficLight = new TrafficLight(vehicleToExitQueue, vehicleQueue, road);
            trafficQueues.put(road, vehicleQueue);
            trafficLight.start();
        }

        new Receiver(vehiclesToSort, exit.toString(), exit.getPort()).start();
        new TrafficSorter(trafficQueues, vehiclesToSort, exit).start();
        new ExitTimeStamper(vehicleToExitQueue, sampedVehiclesQueue).start();

        while(true){
            Vehicle vehicle = sampedVehiclesQueue.remove();
            if(vehicle == null) continue;
            System.out.println("Vehicle " + vehicle.getId() + " exited at " + vehicle.getExitTime());
        }
        

    }
}
