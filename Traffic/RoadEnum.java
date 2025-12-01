package Traffic;

import java.util.*;

import Node.NodeEnum;

/**
 * Enumeration representing all roads available in the system.
 * Each road connects two nodes.
 */
public enum RoadEnum {
    E1_CR1(NodeEnum.E1, NodeEnum.CR1, 1000, 5000),
    E2_CR2(NodeEnum.E2, NodeEnum.CR2, 1000, 5000),
    E3_CR3(NodeEnum.E3, NodeEnum.CR3, 1000, 5000),
    CR1_CR2(NodeEnum.CR1, NodeEnum.CR2, 1500, 5000),
    CR1_CR4(NodeEnum.CR1, NodeEnum.CR4, 1500, 5000),
    CR2_CR1(NodeEnum.CR2, NodeEnum.CR1, 1500, 5000),
    CR2_CR3(NodeEnum.CR2, NodeEnum.CR3, 1500, 5000),
    CR2_CR5(NodeEnum.CR2, NodeEnum.CR5, 1500, 5000),
    CR3_CR2(NodeEnum.CR3, NodeEnum.CR2, 1500, 5000),
    CR4_CR5(NodeEnum.CR4, NodeEnum.CR5, 1500, 5000),
    CR3_S(NodeEnum.CR3, NodeEnum.S, 2000),
    CR5_S(NodeEnum.CR5, NodeEnum.S, 2000);

    private final NodeEnum origin;
    private final NodeEnum destination;
    private final int timeToTravel;
    private final int greenLightDuration;

    RoadEnum(NodeEnum origin, NodeEnum destination, int timeToTravel) {
        this.origin = origin;
        this.destination = destination;
        this.timeToTravel = timeToTravel;
        this.greenLightDuration = 5000;
    }

    RoadEnum(NodeEnum origin, NodeEnum destination, int timteToTravel, int greenLightDuration) {
        this.origin = origin;
        this.destination = destination;
        this.timeToTravel = timteToTravel;
        this.greenLightDuration = greenLightDuration;
    }

    @Override
    public String toString() {
        return this.name();
    }

    public static RoadEnum toRoadEnum(String roadStr) {
        for (RoadEnum road : values()) {
            if (road.name().equals(roadStr)) {
                return road;
            }
        }
        return null;
    }
    /**
     * Origin of the road.
     *
     * @return {@link NodeEnum} origin
     */
    public NodeEnum getOrigin() {
        return origin;
    }

    /**
     * Destination of the road.
     *
     * @return {@link NodeEnum} destination
     */
    public NodeEnum getDestination() {
        return destination;
    }

    /**
     * Base time (ms) to traverse this road.
     *
     * @return time in milliseconds
     */
    public int getTime() {
        return timeToTravel;
    }

    /**
     * Green light duration for this road (ms).
     *
     * @return duration in ms
     */
    public int getGreenLightDuration() {
        return greenLightDuration;
    }

    /**
     * Returns the roads that end at the provided crossroad/node.
     *
     * @param node destination node
     * @return list of {@link RoadEnum}
     */
    public static List<RoadEnum> getRoadsToCrossroad(NodeEnum node) {
        List<RoadEnum> roads = new ArrayList<>();
        for (RoadEnum road : values()) {
            if (road.destination == node)
                roads.add(road);
        }
        return roads;
    }

    /**
     * Returns the roads that originate at the provided crossroad/node.
     *
     * @param node origin node
     * @return list of {@link RoadEnum}
     */
    public static List<RoadEnum> getRoadsFromCrossroad(NodeEnum node) {
        List<RoadEnum> roads = new ArrayList<>();
        for (RoadEnum road : values()) {
            if (road.origin == node)
                roads.add(road);
        }
        return roads;
    }
}