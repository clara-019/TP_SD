package Node;

import java.util.List;
import java.util.Random;

import Comunication.*;
import Event.*;
import Utils.*;
import Vehicle.*;

public class Entrance {

    private static final double lambda = 0.2;

    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("Please provide a crossroad string as an argument.");
            return;
        }
        String entranceId = args[0];
        NodeEnum entrance = NodeEnum.toNodeEnum(entranceId);

        if (entrance == null || entrance.getType() != NodeType.ENTRANCE) {
            System.out.println("Invalid entrance node: " + entranceId);
            return;
        }

        Random rnd = new Random();
        int counter = 0;
        LogicalClock clock = new LogicalClock();

        SynchronizedQueue<Vehicle> outgoingQueue = new SynchronizedQueue<>();

        RoadEnum road = RoadEnum.getRoadsFromCrossroad(entrance).get(0);
        int destPort = road.getDestination().getPort();

        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        while (true) {
            VehicleTypes type = VehicleTypes.values()[rnd.nextInt(VehicleTypes.values().length)];
            List<PathEnum> possiblePaths = PathEnum.getPathsFromEntrance(entrance);
            PathEnum path = possiblePaths.get(rnd.nextInt(possiblePaths.size()));
            Vehicle v = new Vehicle(entrance + "-V" + counter++, type, path);
            outgoingQueue.add(v);

            System.out.println("[Entrance] Vehicle created: " + v.getId() +
                    " Type: " + v.getType() + " Path: " + v.getPath());

            Sender.sendToEventHandler(new VehicleEvent(EventType.NEW_VEHICLE, v, entrance, clock.tick()));
            Sender.sendVehicleDeparture(v, destPort, entrance, clock);

            long sleepTime = getExponentialInterval(lambda, rnd);

            try {
                Thread.sleep(sleepTime);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        }
    }

    private static long getExponentialInterval(double lambda, Random rnd) {
        double u = rnd.nextDouble();
        double interval = -Math.log(1 - u) / lambda;
        return (long) (interval * 1000);
    }
}
