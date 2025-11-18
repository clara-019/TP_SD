package Node;

/**
 * Enumeração que representa todos os nós disponíveis no sistema
 * Cada nó tem um nome e uma porta de comunicação associada
 */
public enum NodeEnum {
    E3("E", 5003),
    CR3("Cr", 6003),
    S("S", 7001);

    private final String type;
    private final int port;

    NodeEnum(String type, int port) {
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

    public String getType() {
        return type;
    }
}