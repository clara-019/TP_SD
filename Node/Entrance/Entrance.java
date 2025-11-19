package Node.Entrance;

import Comunication.*;
import Road.RoadEnum;
import Utils.SynchronizedQueue;
import Vehicle.Vehicle;
import Node.NodeEnum;

public class Entrance {
    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("Please provide a crossroad string as an argument.");
            return;
        }
        String entranceId = args[0];
        // install remote log appender so this process sends logs to central LogServer
        try { Comunication.RemoteLogAppender.install("Entrance_" + entranceId, "localhost", Comunication.LogServer.DEFAULT_PORT); } catch (Exception ignored) {}
        NodeEnum entrance = NodeEnum.toNodeEnum(entranceId);

        SynchronizedQueue<Vehicle> inconmingQueue = new SynchronizedQueue<>();
        SynchronizedQueue<Vehicle> outgoingQueue = new SynchronizedQueue<>();

        new Receiver(inconmingQueue, entrance.toString(), entrance.getPort()).start();
        new EntranceTimeStamper(inconmingQueue, outgoingQueue).start();
        new Sender(outgoingQueue, RoadEnum.getRoadsFromCrossroad(entrance).get(0).getPort()).start();
    }
}
