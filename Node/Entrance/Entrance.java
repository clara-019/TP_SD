package Node.Entrance;

import Comunication.*;
import Road.RoadEnum;
import Utils.SynchronizedQueue;
import Vehicle.Vehicle;
import Vehicle.VehicleSpawner;
import Node.NodeEnum;

public class Entrance {
    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("Please provide a crossroad string as an argument.");
            return;
        }
        String entranceId = args[0];
        NodeEnum entrance = NodeEnum.toNodeEnum(entranceId);

        SynchronizedQueue<Vehicle> outgoingQueue = new SynchronizedQueue<>();
        RoadEnum road = RoadEnum.getRoadsFromCrossroad(entrance).get(0);
        int destPort = road.getDestination().getPort();
        new Sender(outgoingQueue, destPort).start();
        VehicleSpawner spawner = new VehicleSpawner(outgoingQueue, true, 5000);
        spawner.start();
    }
}
