package Node;

import java.util.*;

/**
 * Enumeração que representa todas as estradas disponíveis no sistema
 * Cada estrada conecta dois nós
 */
public enum RoadEnum {
    E3_CR3(NodeEnum.E3, NodeEnum.CR3, 1000),
    CR3_S(NodeEnum.CR3, NodeEnum.S, 1000);

    private final NodeEnum origin;
    private final NodeEnum destination;
    private final int timeToTravel;

    RoadEnum(NodeEnum origin, NodeEnum destination, int timeToTravel) {
        this.origin = origin;
        this.destination = destination;
        this.timeToTravel = timeToTravel;
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

    public static List<RoadEnum> getRoadsToCrossroad(NodeEnum node) {
        List<RoadEnum> roads = new ArrayList<>();
        for (RoadEnum road : values()) {
            if (road.destination == node) roads.add(road);
        }
        return roads;
    }

    public static List<RoadEnum> getRoadsFromCrossroad(NodeEnum node) {
        List<RoadEnum> roads = new ArrayList<>();
        for (RoadEnum road : values()) {
            if (road.origin == node) roads.add(road);
        }
        return roads;
    }
}