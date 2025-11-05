package Classes;

import java.util.*;
import Enums.*;

public class Crossroad{
    public static void main(String[] args) {
        if(args.length == 0){
            System.out.println("Please provide a crossroad string as an argument.");
            return;
        }
        CrossroadEnum crossroad = CrossroadEnum.toCrossroadEnum(args[0]);
        List<RoadEnum> roads = RoadEnum.getRoadsToCrossroad(crossroad);
        List<SynchronizedQueue<Vehicle>> vehicleQueues = new ArrayList<>();
        SynchronizedQueue<Vehicle> vehicleToSendQueue = new SynchronizedQueue<>();

        for(RoadEnum road : roads){
            SynchronizedQueue<Vehicle> vehicleQueue = new SynchronizedQueue<>(road);
            TrafficLight trafficLight = new TrafficLight(vehicleToSendQueue, vehicleQueue,  road);
            vehicleQueues.add(vehicleQueue);
            trafficLight.start();
        }

        VehicleReceiver vehicleReceiver = new VehicleReceiver(vehicleQueues, crossroad);
        vehicleReceiver.start();
        VehicleSender vehicleSender = new VehicleSender(vehicleToSendQueue, crossroad);
        vehicleSender.start();
    }


}