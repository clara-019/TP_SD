package Vehicle;

import java.util.*;

import Node.*;

/**
 * Enumeration that defines the possible paths for vehicles.
 * Each path is a sequence of nodes/crossroads that the vehicle will follow.
 */
public enum PathEnum {
    E1_CR1_CR4_CR5_S(100), // Percurso: E1 → CR1 → CR4 → CR5 → S
    E1_CR1_CR2_CR5_S(1), // Percurso: E1 → CR1 → CR2 → CR5 → S
    E1_CR1_CR2_CR3_S(1), // Percurso: E1 → CR1 → CR2 → CR3 → S
    E2_CR2_CR5_S(34), // Percurso: E2 → CR2 → CR5 → S
    E2_CR2_CR3_S(33), // Percurso: E2 → CR2 → CR3 → S
    E2_CR2_CR1_CR4_CR5_S(33), // Percurso: E2 → CR2 → CR1 → CR4 → CR5 → S
    E3_CR3_S(34), // Percurso: E3 → CR3 → S
    E3_CR3_CR2_CR5_S(33), // Percurso: E3 → CR3 → CR2 → CR5 → S
    E3_CR3_CR2_CR1_CR4_CR5_S(33); // Percurso: E3 → CR3 → CR2 → CR1 → CR4 → CR5 → S

    private final int probToBeSelected;

    PathEnum(int probToBeSelected) {
        this.probToBeSelected = probToBeSelected;
    }

    /**
     * Returns the sequence of nodes corresponding to this path.
     *
     * @return ordered list of {@link NodeEnum} representing the path
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

    /**
     * Returns all paths that have the given entrance as their first node.
     *
     * @param entrance entrance node (e.g. E1, E2, E3)
     * @return list of {@link PathEnum} that start at the given entrance
     */
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
     * Returns a human-readable representation of the path.
     *
     * @return string with the sequence of nodes separated by " -> "
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

    /**
     * Probability (weight) that this path will be selected when generating
     * vehicles.
     *
     * @return integer value representing the relative probability
     */
    public int getProbToBeSelected() {
        return probToBeSelected;
    }
}