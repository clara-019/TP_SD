package Node;

import java.util.ArrayList;
import java.util.List;

/**
 * Enumeration that represents all available nodes in the system.
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

    NodeEnum(NodeType type, int port) {
        this.type = type;
        this.port = port;
    }

    @Override
    public String toString() {
        return this.name();
    }

    public static NodeEnum toNodeEnum(String nodeString) {
        for (NodeEnum node : values()) {
            if (node.name().equals(nodeString)) {
                return node;
            }
        }
        return null;
    }

    public int getPort() {
        return port;
    }

    public NodeType getType() {
        return type;
    }

    public static List<NodeEnum> getEntrances() {
        List<NodeEnum> entrances = new ArrayList<>();
        for (NodeEnum node : values()) {
            if (node.getType() == NodeType.ENTRANCE) {
                entrances.add(node);
            }
        }
        return entrances;
    }
}