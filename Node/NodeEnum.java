package Node;

import java.util.ArrayList;
import java.util.List;

/**
 * Enumeração que representa todos os nós disponíveis no sistema
 * Cada nó tem um nome e uma porta de comunicação associada
 */
public enum NodeEnum {
    E3(NodeType.ENTRANCE, 5003),
    CR3(NodeType.CROSSROAD, 6003),
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