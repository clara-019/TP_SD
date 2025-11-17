package Node;

import java.util.*;

import Comunication.VehicleReceiver;
import Comunication.VehicleSender;
import Road.*;
import Utils.SynchronizedQueue;
import Vehicle.Vehicle;

public class Crossroad {
    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("Please provide a crossroad string as an argument.");
            return;
        }
        NodeEnum crossroad = NodeEnum.toNodeEnum(args[0]);
        List<RoadEnum> roads = RoadEnum.getRoadsToCrossroad(crossroad);
        List<SynchronizedQueue<Vehicle>> trafficQueues = new ArrayList<>();
        SynchronizedQueue<Vehicle> vehicleToSendQueue = new SynchronizedQueue<>();

        if (roads.size() > 0) {
            for (RoadEnum road : roads) {
                SynchronizedQueue<Vehicle> vehicleQueue = new SynchronizedQueue<>(road);
                TrafficLight trafficLight = new TrafficLight(vehicleToSendQueue, vehicleQueue, road);
                trafficQueues.add(vehicleQueue);
                trafficLight.start();
            }
        } else {
            trafficQueues.add(vehicleToSendQueue);
        }

        VehicleReceiver vehicleReceiver = new VehicleReceiver(trafficQueues, crossroad);
        vehicleReceiver.start();
        VehicleSender vehicleSender = new VehicleSender(vehicleToSendQueue, crossroad);
        vehicleSender.start();
    }
}