package Road;

import Utils.SynchronizedQueue;
import Vehicle.Vehicle;
import Comunication.*;

public class Road {
    private RoadEnum road;

    public Road(RoadEnum road) {
        this.road = road;
    }

    public void start() {
        SynchronizedQueue<Vehicle> vehicleToSendQueue = new SynchronizedQueue<>();
        new Receiver(vehicleToSendQueue, road.toString(), road.getPort()).start();
        new Sender(vehicleToSendQueue, road.getDestination().getPort()).start();
    }

    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("Please provide a road string as an argument.");
            return;
        }
        new Road(RoadEnum.toRoadEnum(args[0])).start();
    }
}
