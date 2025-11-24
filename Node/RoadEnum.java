package Node;

import java.util.*;

/**
 * Enumeração que representa todas as estradas disponíveis no sistema
 * Cada estrada conecta dois nós
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

    public NodeEnum getOrigin() {
        return origin;
    }

    public NodeEnum getDestination() {
        return destination;
    }

    public int getTime() {
        return timeToTravel;
    }

    public int getGreenLightDuration() {
        return greenLightDuration;
    }

    public static List<RoadEnum> getRoadsToCrossroad(NodeEnum node) {
        List<RoadEnum> roads = new ArrayList<>();
        for (RoadEnum road : values()) {
            if (road.destination == node)
                roads.add(road);
        }
        return roads;
    }

    public static List<RoadEnum> getRoadsFromCrossroad(NodeEnum node) {
        List<RoadEnum> roads = new ArrayList<>();
        for (RoadEnum road : values()) {
            if (road.origin == node)
                roads.add(road);
        }
        return roads;
    }
}