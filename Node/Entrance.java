package Node;

import Comunication.*;
import Event.*;
import Road.RoadEnum;
import Utils.SynchronizedQueue;
import Vehicle.*;

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

        try{
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        
        while (true) {
            VehicleTypes type = VehicleTypes.values()[rnd.nextInt(VehicleTypes.values().length)];
            Vehicle v = new Vehicle(entrance + "-V" + counter++, type, PathEnum.E3_CR3_S);
            v.setEntranceTime((int) System.currentTimeMillis());
            outgoingQueue.add(v);
            System.out.println("[Entrance] Vehicle created: " + v.getId() +
                    " Type: " + v.getType() + " Path: " + v.getPath());
            Sender.sendEvent(new VehicleEvent(EventType.NEW_VEHICLE, v, entrance, System.currentTimeMillis()));
            Sender.sendVehicleDeparture(v, destPort, entrance);
            try {
                Thread.sleep(3500 + rnd.nextInt(1500));
            } catch (Exception e) {
                e.printStackTrace();
            }

        }
    }
}
