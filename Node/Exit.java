package Node;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import Comunication.*;
import Event.*;
import Traffic.PassRoad;
import Traffic.RoadEnum;
import Traffic.TrafficSorter;
import Utils.*;
import Vehicle.Vehicle;

/**
 * Exit node runner.
 * <p>
 * The exit node collects vehicles that have completed their path and
 * forwards {@link VehicleEvent}s of type {@link EventType#VEHICLE_EXIT} to the
 * central event handler. It composes per-road queues and pass-through handlers
 * to receive vehicles delivered by upstream crossroads.
 */
public class Exit {
    private final NodeEnum exit;
    private final LogicalClock clock = new LogicalClock();
    private final List<RoadEnum> roadsToExit;
    private final Map<RoadEnum, SynchronizedQueue<Vehicle>> trafficQueues = new HashMap<>();
    private final SynchronizedQueue<Vehicle> incommingQueue = new SynchronizedQueue<>();
    private final SynchronizedQueue<Vehicle> passedQueue = new SynchronizedQueue<>();

    /**
     * Constructs and starts the exit node runner for the provided node
     * identifier.
     *
     * @param exit the {@link NodeEnum} identifying this exit node
     */
    private Exit(NodeEnum exit) {
        this.exit = exit;
        this.roadsToExit = RoadEnum.getRoadsToCrossroad(exit);
        start();
    }

    /**
     * Initializes per-road pass-through handlers, the receiver and the
     * traffic sorter and then continuously consumes vehicles that finished
     * their path, sending a {@link VehicleEvent} to the event handler for
     * each one.
     */
    private void start() {
        for (RoadEnum road : this.roadsToExit) {
            SynchronizedQueue<Vehicle> trafficQueue = new SynchronizedQueue<>();
            PassRoad passRoad = new PassRoad(trafficQueue, this.passedQueue, road, this.clock);
            this.trafficQueues.put(road, trafficQueue);
            passRoad.start();
        }

        new Receiver(this.incommingQueue, this.exit, this.clock).start();
        new TrafficSorter(this.trafficQueues, this.incommingQueue, this.exit).start();

        while (true) {
            Vehicle vehicle = this.passedQueue.remove();
            Sender.sendToEventHandler(new VehicleEvent(EventType.VEHICLE_EXIT, this.exit, this.clock.tick(), vehicle));
        }
    }

    /**
     * Entry point to run an instance of an exit node.
     *
     * @param args argument with the exit identifier (Ex. "EX1")
     */
    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("Please provide an exit string as an argument.");
            return;
        }
        String exitId = args[0];
        NodeEnum exit = NodeEnum.toNodeEnum(exitId);

        if (exit == null || exit.getType() != NodeType.EXIT) {
            System.out.println("Invalid exit node: " + exitId);
            return;
        }

        new Exit(exit);
    }
}
