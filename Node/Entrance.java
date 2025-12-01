package Node;

import java.util.List;
import java.util.Random;

import Comunication.*;
import Event.*;
import Traffic.RoadEnum;
import Utils.*;
import Vehicle.*;

public class Entrance {
    /**
     * Entrance node responsible for periodically generating vehicles and
     * sending creation/departure events to the system.
     */
    private static final double LAMBDA = 0.3;
    private static final Random rnd = new Random();
    private final NodeEnum entrance;
    private final LogicalClock clock = new LogicalClock();
    private final int destPort;
    private final List<PathEnum> possiblePaths;
    private final int probabilitySum;
    private int counter = 0;

    /**
     * Entrance constructor.
     *
     * @param entrance entrance node (E1, E2, ...)
     */
    private Entrance(NodeEnum entrance) {
        this.entrance = entrance;
        this.possiblePaths = PathEnum.getPathsFromEntrance(entrance);
        this.probabilitySum = getProbabilitySum(possiblePaths);
        this.destPort = RoadEnum.getRoadsFromCrossroad(entrance).get(0).getDestination().getPort();
    }

    /**
     * Starts the vehicle generation loop: creates vehicles, sends events and
     * waits the exponential interval between generations.
     */
    private void start() {
        while (true) {
            Vehicle v = generateVehicle();
            System.out.println("[Entrance] Vehicle created: " + v.getId() +
                    " Type: " + v.getType() + " Path: " + v.getPath());

            Sender.sendToEventHandler(new VehicleEvent(EventType.NEW_VEHICLE, entrance, clock.tick(), v));
            Sender.sendVehicleDeparture(v, destPort, entrance, clock);
            ;
            try {
                Thread.sleep(getExponentialInterval(rnd));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Sums the selection weights of the possible paths for this entrance.
     *
     * @param possiblePaths list of possible paths
     * @return sum of probability/weight values
     */
    private int getProbabilitySum(List<PathEnum> possiblePaths) {
        int sum = 0;
        for (PathEnum path : possiblePaths) {
            sum += path.getProbToBeSelected();
        }
        return sum;
    }

    /**
     * Selects a path randomly based on weights/probabilities.
     *
     * @param possiblePaths list of possible paths
     * @param probabilitySum sum of probabilities
     * @param rnd            random number generator
     * @return selected path
     */
    private PathEnum selectPath(List<PathEnum> possiblePaths, int probabilitySum, Random rnd) {
        int value = rnd.nextInt(probabilitySum);
        int cummulativeProb = 0;
        for (PathEnum path : possiblePaths) {
            cummulativeProb += path.getProbToBeSelected();
            if (value < cummulativeProb) {
                return path;
            }
        }
        return possiblePaths.get(0);
    }

    /**
     * Generates a new vehicle with randomly selected type and path.
     *
     * @return new {@link Vehicle}
     */
    private Vehicle generateVehicle() {
        VehicleTypes type = VehicleTypes.values()[rnd.nextInt(VehicleTypes.values().length)];
        PathEnum selectedPath = selectPath(possiblePaths, probabilitySum, rnd);
        return new Vehicle(entrance + "-V" + this.counter++, type, selectedPath);
    }

    /**
     * Computes an exponential interval (in ms) used to space vehicle
     * generation.
     *
     * @param rnd random number generator
     * @return interval in milliseconds
     */
    private long getExponentialInterval(Random rnd) {
        double u = rnd.nextDouble();
        double interval = -Math.log(1 - u) / LAMBDA;
        return (long) (interval * 1000);
    }

    /**
     * Entry point to run an instance of the entrance as an application.
     *
     * @param args argument with the entrance node identifier (e.g. "E1")
     */
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

        Entrance entranceObj = new Entrance(entrance);
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        entranceObj.start();
    }
}
