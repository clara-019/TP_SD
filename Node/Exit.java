package Node;

import Comunication.*;
import Event.*;
import Utils.*;
import Vehicle.Vehicle;

public class Exit {
    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("Please provide a crossroad string as an argument.");
            return;
        }
        String exitId = args[0];
        NodeEnum exit = NodeEnum.toNodeEnum(exitId);

        if (exit == null || exit.getType() != NodeType.EXIT) {
            System.out.println("Invalid exit node: " + exitId);
            return;
        }

        LogicalClock clock = new LogicalClock();

        SynchronizedQueue<Vehicle> incommingQueue = new SynchronizedQueue<>();

        new Receiver(incommingQueue, exit.getPort(), exit, clock).start();

        while (true) {
            Vehicle vehicle = incommingQueue.remove();
            if (vehicle == null)
                continue;
            Sender.sendToEventHandler(new VehicleEvent(EventType.VEHICLE_EXIT, vehicle, exit, clock.tick()));
        }

    }
}
