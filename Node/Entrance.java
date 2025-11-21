package Node.Entrance;

import Comunication.*;
import Event.EventType;
import Road.RoadEnum;
import Utils.SynchronizedQueue;
import Vehicle.PathEnum;
import Vehicle.Vehicle;
import Vehicle.VehicleTypes;
import Node.NodeEnum;

public class Entrance {
    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("Please provide a crossroad string as an argument.");
            return;
        }
        String entranceId = args[0];
        NodeEnum entrance = NodeEnum.toNodeEnum(entranceId);
        java.util.Random rnd = new java.util.Random();
        int counter = 0;

        SynchronizedQueue<Vehicle> outgoingQueue = new SynchronizedQueue<>();
        RoadEnum road = RoadEnum.getRoadsFromCrossroad(entrance).get(0);
        int destPort = road.getDestination().getPort();
        new Sender(outgoingQueue, destPort, EventType.NEW_VEHICLE, entrance).start();
        while (true) {
            VehicleTypes type = VehicleTypes.values()[rnd.nextInt(VehicleTypes.values().length)];
            Vehicle v = new Vehicle("V" + counter++, type, PathEnum.E3_CR3_S);
            v.setEntranceTime((int) System.currentTimeMillis());
            outgoingQueue.add(v);
            System.out.println("[Entrance] Vehicle created: " + v.getId() +
                    " Type: " + v.getType() + " Path: " + v.getPath());

            
            try {
                Thread.sleep(3500 + rnd.nextInt(1500));
            } catch (Exception e) {
                e.printStackTrace();
            }

        }
    }
}
