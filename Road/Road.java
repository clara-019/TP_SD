package Road;

import java.util.*;

import Comunication.VehicleReceiver;
import Comunication.VehicleSender;
import Enums.*;
import Utils.SynchronizedQueue;
import Vehicle.Vehicle;
import Crossroad.*;

public class Road {

    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("Please provide a road string as an argument.");
            return;
        }
        RoadEnum road = RoadEnum.toRoadEnum(args[0]);
        CrossroadEnum crossroad = road.getDestination();
        List<SynchronizedQueue<Vehicle>> vehicleQueues = new ArrayList<>();
        SynchronizedQueue<Vehicle> vehicleToSendQueue = new SynchronizedQueue<>();
        vehicleQueues.add(vehicleToSendQueue);

        VehicleReceiver vehicleReceiver = new VehicleReceiver(vehicleQueues, crossroad);
        vehicleReceiver.start();
        VehicleSender vehicleSender = new VehicleSender(vehicleToSendQueue, crossroad);
        vehicleSender.start();
    }
}
