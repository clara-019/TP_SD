package Node;

/**
 * Enumeração que representa todos os nós disponíveis no sistema
 * Cada nó tem um nome e uma porta de comunicação associada
 */
public enum NodeEnum {
    E3, Cr3, S; // nós disponíveis

    /**
     * Retorna a representação em string do node
     */
    @Override
    public String toString() {
        return this.name();
    }

    /**
     * Converte uma string para o enum correspondente
     * 
     * @param nodeString String a converter
     * @return NodeEnum correspondente ou null se não existir
     */
    public static NodeEnum toNodeEnum(String nodeString) {
        for (NodeEnum node : values()) {
            if (node.toString().equals(nodeString)) {
                return node;
            }
        }
        return null;
    }

    /**
     * Retorna a porta de comunicação associada ao nó
     * 
     * @return Número da porta
     */
    public int getPort() {
        switch (this) {
            case E3:
                return 5003;
            case Cr3:
                return 6003;
            case S:
                return 7001;
            default:
                return -1;
        }
    }

    public String getType() {
        switch (this) {
            case E3:
                return "E";
            case Cr3:
                return "Cr";
            case S:
                return "S";
            default:
                return "";
        }
    }
}