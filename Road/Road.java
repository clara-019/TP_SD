package Road;

import Utils.SynchronizedQueue;
import Vehicle.Vehicle;
import Comunication.*;
import Crossroad.*;

public class Road {

    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("Please provide a road string as an argument.");
            return;
        }
        RoadEnum road = RoadEnum.toRoadEnum(args[0]);
        SynchronizedQueue<Vehicle> vehicleToSendQueue = new SynchronizedQueue<>(road);

        Receiver receiver = new Receiver(vehicleToSendQueue, road.toString(), road.getPort());
        receiver.start();
        Sender sender = new Sender(vehicleToSendQueue, road.getDestination().getPort());
        sender.start();
    }
}
