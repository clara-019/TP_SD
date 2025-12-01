package Node;

import java.util.ArrayList;
import java.util.List;

/**
 * Enumeration representing all nodes available in the system.
 * Each node has a name and an associated communication port.
 */
public enum NodeEnum {
    E1(NodeType.ENTRANCE, 5001),
    E2(NodeType.ENTRANCE, 5002),
    E3(NodeType.ENTRANCE, 5003),
    CR1(NodeType.CROSSROAD, 6001),
    CR2(NodeType.CROSSROAD, 6002),
    CR3(NodeType.CROSSROAD, 6003),
    CR4(NodeType.CROSSROAD, 6004),
    CR5(NodeType.CROSSROAD, 6005),
    S(NodeType.EXIT, 7001);

    private final NodeType type;
    private final int port;

    /**
     * Create a node enumeration value.
     *
     * @param type the {@link NodeType} for this node
     * @param port the TCP port used by the node for inter-process communication
     */
    private NodeEnum(NodeType type, int port) {
        this.type = type;
        this.port = port;
    }

    /**
     * Converts a string identifier to the corresponding {@link NodeEnum}.
     *
     * @param nodeString the enum name (for example "E1", "CR1", "S")
     * @return the matching {@link NodeEnum} or {@code null} if not found
     */
    public static NodeEnum toNodeEnum(String nodeString) {
        for (NodeEnum node : values()) {
            if (node.name().equals(nodeString)) {
                return node;
            }
        }
        return null;
    }

    /**
     * Returns the communication port associated with this node.
     *
     * @return TCP port used by this node
     */
    public int getPort() {
        return port;
    }

    /**
     * Returns the node type (entrance, exit, crossroad).
     *
     * @return {@link NodeType} of the node
     */
    public NodeType getType() {
        return type;
    }

    /**
     * Returns the list of nodes that are entrances in the system.
     *
     * @return list of {@link NodeEnum} of type ENTRANCE
     */
    public static List<NodeEnum> getEntrances() {
        List<NodeEnum> entrances = new ArrayList<>();
        for (NodeEnum node : values()) {
            if (node.getType() == NodeType.ENTRANCE) {
                entrances.add(node);
            }
        }
        return entrances;
    }

    @Override
    public String toString() {
        return this.name();
    }
}