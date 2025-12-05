package Node;

import Comunication.*;
import Event.*;
import Traffic.RoadEnum;
import Utils.*;
import Vehicle.*;
import java.util.List;
import java.util.Random;

/**
 * Entrance node responsible for periodically generating vehicles and
 * sending creation/departure events to the rest of the system.
 * <p>
 * The entrance simulates vehicle arrivals using an exponential inter-arrival
 * distribution. Each generated {@link Vehicle} is assigned a random
 * {@link VehicleType} and a route selected from the {@link PathEnum} options
 * for this entrance. Created vehicles are reported to the event handler and
 * their departure is sent to the destination crossroad over the network.
 */
public class Entrance {
    private static double LAMBDA = 0.3;
    private static final Random RND = new Random();
    private final NodeEnum entrance;
    private final LogicalClock clock = new LogicalClock();
    private final int destPort;
    private final List<PathEnum> possiblePaths;
    private final int probabilitySum;
    private int counter = 0;

    /**
     * Constructs and starts the entrance node runner for the provided node
     * identifier.
     *
     * @param entrance the {@link Node.NodeEnum} identifying this entrance node
     */
    private Entrance(NodeEnum entrance) {
        this.entrance = entrance;
        this.possiblePaths = PathEnum.getPathsFromEntrance(entrance);
        this.probabilitySum = getProbabilitySum();
        this.destPort = RoadEnum.getRoadsFromCrossroad(entrance).get(0).getDestination().getPort();
        start();
    }

    static {
        try {
            String v = System.getProperty("simulation.lambda");
            if (v != null && !v.isEmpty()) {
                LAMBDA = Double.parseDouble(v);
                System.out.println("[Entrance] Using simulation.lambda=" + LAMBDA);
            }
        } catch (Exception ignored) {
        }
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
                Thread.sleep(getExponentialInterval());
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Sums the selection weights of the possible paths for this entrance.
     *
     * @return sum of probability/weight values
     */
    private int getProbabilitySum() {
        int sum = 0;
        for (PathEnum path : this.possiblePaths) {
            sum += path.getProbToBeSelected();
        }
        return sum;
    }

    /**
     * Selects a path randomly based on weights/probabilities.
     *
     * @return selected path
     */
    private PathEnum selectPath() {
        int value = RND.nextInt(this.probabilitySum);
        int cummulativeProb = 0;
        for (PathEnum path : this.possiblePaths) {
            cummulativeProb += path.getProbToBeSelected();
            if (value < cummulativeProb) {
                return path;
            }
        }
        return this.possiblePaths.get(0);
    }

    /**
     * Generates a new vehicle with randomly selected type and path.
     *
     * @return new {@link Vehicle}
     */
    private Vehicle generateVehicle() {
        VehicleType type = VehicleType.values()[RND.nextInt(VehicleType.values().length)];
        PathEnum selectedPath = selectPath();
        return new Vehicle(entrance + "-V" + this.counter++, type, selectedPath);
    }

    /**
     * Computes an exponential interval (in ms) used to space vehicle
     * generation.
     *
     * @return interval in milliseconds
     */
    private long getExponentialInterval() {
        double u = RND.nextDouble();
        double interval = -Math.log(1 - u) / LAMBDA;
        return (long) (interval * 1000);
    }

    /**
     * Entry point to run an instance of the entrance as an application.
     *
     * @param args argument with the entrance node identifier (Ex. "E1")
     */
    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("Please provide an entrance string as an argument.");
            return;
        }
        String entranceId = args[0];
        NodeEnum entrance = NodeEnum.toNodeEnum(entranceId);

        if (entrance == null || entrance.getType() != NodeType.ENTRANCE) {
            System.out.println("Invalid entrance node: " + entranceId);
            return;
        }

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        new Entrance(entrance);

    }
}
