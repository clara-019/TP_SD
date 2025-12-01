package Vehicle;

import java.util.*;

import Node.*;

/**
 * Enumeration that defines the possible routes for vehicles.
 * Each route is a sequence of crossroads (nodes).
 */
public enum PathEnum {
    E1_CR1_CR4_CR5_S(100), // Path: E1 → CR1 → CR4 → CR5 → S
    E1_CR1_CR2_CR5_S(1), // Path: E1 → CR1 → CR2 → CR5 → S
    E1_CR1_CR2_CR3_S(1), // Path: E1 → CR1 → CR2 → CR3 → S
    E2_CR2_CR5_S(34), // Path: E2 → CR2 → CR5 → S
    E2_CR2_CR3_S(33), // Path: E2 → CR2 → CR3 → S
    E2_CR2_CR1_CR4_CR5_S(33), // Path: E2 → CR2 → CR1 → CR4 → CR5 → S
    E3_CR3_S(34), // Path: E3 → CR3 → S
    E3_CR3_CR2_CR5_S(33), // Path: E3 → CR3 → CR2 → CR5 → S
    E3_CR3_CR2_CR1_CR4_CR5_S(33); // Path: E3 → CR3 → CR2 → CR1 → CR4 → CR5 → S

    private final int probToBeSelected;

    PathEnum(int probToBeSelected) {
        this.probToBeSelected = probToBeSelected;
    }

    /**
     * Returns the sequence of nodes for the path
     *
     * @return Ordered list of nodes
     */
    public List<NodeEnum> getPath() {
        switch (this) {
            case E1_CR1_CR4_CR5_S:
                return Arrays.asList(NodeEnum.E1, NodeEnum.CR1, NodeEnum.CR4, NodeEnum.CR5, NodeEnum.S);
            case E1_CR1_CR2_CR5_S:
                return Arrays.asList(NodeEnum.E1, NodeEnum.CR1, NodeEnum.CR2, NodeEnum.CR5, NodeEnum.S);
            case E1_CR1_CR2_CR3_S:
                return Arrays.asList(NodeEnum.E1, NodeEnum.CR1, NodeEnum.CR2, NodeEnum.CR3, NodeEnum.S);
            case E2_CR2_CR5_S:
                return Arrays.asList(NodeEnum.E2, NodeEnum.CR2, NodeEnum.CR5, NodeEnum.S);
            case E2_CR2_CR3_S:
                return Arrays.asList(NodeEnum.E2, NodeEnum.CR2, NodeEnum.CR3, NodeEnum.S);
            case E2_CR2_CR1_CR4_CR5_S:
                return Arrays.asList(NodeEnum.E2, NodeEnum.CR2, NodeEnum.CR1, NodeEnum.CR4, NodeEnum.CR5, NodeEnum.S);
            case E3_CR3_S:
                return Arrays.asList(NodeEnum.E3, NodeEnum.CR3, NodeEnum.S);
            case E3_CR3_CR2_CR5_S:
                return Arrays.asList(NodeEnum.E3, NodeEnum.CR3, NodeEnum.CR2, NodeEnum.CR5, NodeEnum.S);
            case E3_CR3_CR2_CR1_CR4_CR5_S:
                return Arrays.asList(NodeEnum.E3, NodeEnum.CR3, NodeEnum.CR2, NodeEnum.CR1, NodeEnum.CR4, NodeEnum.CR5,
                        NodeEnum.S);
            default:
                return Collections.emptyList();
        }
    }

    public static List<PathEnum> getPathsFromEntrance(NodeEnum entrance) {
        List<PathEnum> paths = new ArrayList<>();
        for (PathEnum path : PathEnum.values()) {
            if (path.getPath().get(0) == entrance) {
                paths.add(path);
            }
        }
        return paths;
    }

    /**
     * Returns the string representation of the path
     */
    @Override
    public String toString() {
        switch (this) {
            case E3_CR3_S:
                return "E3 -> CR3 -> S";
            default:
                return "Unknown Path";
        }
    }

    public int getProbToBeSelected() {
        return probToBeSelected;
    }
}