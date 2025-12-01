package Node;

import java.util.*;

import Comunication.*;
import Traffic.*;
import Utils.*;
import Vehicle.Vehicle;

/**
 * Class to run a crossroad node. Initializes traffic lights, pass-through
 * handlers and coordinators (round-robin) as needed.
 */
public class Crossroad {
    private final NodeEnum crossroad;
    private final LogicalClock clock = new LogicalClock();
    private final List<RoadEnum> roadsToCrossroad;

    /**
     * Constructs and starts the crossroad node runner for the provided node
     * identifier.
     *
     * @param crossroad the {@link Node.NodeEnum} identifying this crossroad
     */
    private Crossroad(NodeEnum crossroad) {
        this.crossroad = crossroad;
        this.roadsToCrossroad = RoadEnum.getRoadsToCrossroad(crossroad);
        start();
    }

    /**
     * Initializes and starts the crossroad components.
     * <p>
     * If multiple incoming roads exist the crossroad starts a set of
     * traffic lights, pass-through handlers and a sorter. If only a single
     * incoming road exists a simpler single-signal configuration is used.
     */
    private void start() {
        if (roadsToCrossroad.size() >= 2) {
            startMultipleSignals();
        } else {
            startSingleSignal();
        }
    }

    /**
     * Starts the crossroad in multi-signal mode.
     * <p>
     * This method creates a {@link RoundRobin} coordinator, per-road
     * synchronized queues for arriving and passed vehicles, and spawns
     * {@link PassRoad} and {@link TrafficLight} threads for
     * each incoming road. A single {@link Receiver} and
     * {@link TrafficSorter} are also started to distribute arriving
     * vehicles into the appropriate road queues.
     */
    private void startMultipleSignals() {
        RoundRobin roundRobin = new RoundRobin(roadsToCrossroad.size());
        Map<RoadEnum, SynchronizedQueue<Vehicle>> trafficQueues = new HashMap<>();
        Map<RoadEnum, SynchronizedQueue<Vehicle>> passedQueues = new HashMap<>();

        SynchronizedQueue<Vehicle> vehiclesToSort = new SynchronizedQueue<>();

        for (RoadEnum road : roadsToCrossroad) {
            SynchronizedQueue<Vehicle> vehicleQueue = new SynchronizedQueue<>();
            SynchronizedQueue<Vehicle> passedQueue = new SynchronizedQueue<>();

            trafficQueues.put(road, vehicleQueue);
            passedQueues.put(road, passedQueue);

            PassRoad passRoad = new PassRoad(vehicleQueue, passedQueue, road, clock);
            TrafficLight trafficLight = new TrafficLight(passedQueue, road, clock, roundRobin);
            passRoad.start();
            trafficLight.start();
        }

        new Receiver(vehiclesToSort, crossroad, clock).start();
        new TrafficSorter(trafficQueues, vehiclesToSort, crossroad).start();
    }

    /**
     * Starts the crossroad in single-signal mode.
     * <p>
     * This sets up a minimal configuration for a crossroad with only one
     * incoming road: a pass-through handler, a single traffic light, a
     * pedestrian light and a receiver for arriving vehicles.
     */
    private void startSingleSignal() {
        RoundRobin roundRobin = new RoundRobin(2);
        SynchronizedQueue<Vehicle> arrivingQueue = new SynchronizedQueue<>();
        SynchronizedQueue<Vehicle> passedQueue = new SynchronizedQueue<>();

        new PassRoad(arrivingQueue, passedQueue, roadsToCrossroad.get(0), clock).start();
        new TrafficLight(passedQueue, roadsToCrossroad.get(0), clock, roundRobin).start();
        new PedestrianLight(roundRobin, 1).start();
        new Receiver(arrivingQueue, crossroad, clock).start();
    }

    /**
     * Entry point to run an instance of a crossroad.
     *
     * @param args argument with the crossroad identifier (Ex. "CR1")
     */
    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("Please provide a crossroad string as an argument.");
            return;
        }
        String crossId = args[0];
        NodeEnum crossroad = NodeEnum.toNodeEnum(crossId);

        if (crossroad == null || crossroad.getType() != NodeType.CROSSROAD) {
            System.out.println("Invalid crossroad node: " + crossId);
            return;
        }

        new Crossroad(crossroad);
    }
}
