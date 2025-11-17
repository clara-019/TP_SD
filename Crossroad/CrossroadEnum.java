package Crossroad;

/**
 * Enumeração que representa todos os cruzamentos disponíveis no sistema
 * Cada cruzamento tem um nome e uma porta de comunicação associada
 */
public enum CrossroadEnum {
    E3, Cr3, S; // Cruzamentos disponíveis

    /**
     * Retorna a representação em string do cruzamento
     */
    public String toString() {
        return this.name(); // Usa o nome do enum diretamente
    }

    /**
     * Converte uma string para o enum correspondente
     * @param crossString String a converter
     * @return CrossroadEnum correspondente ou null se não existir
     */
    public static CrossroadEnum toCrossroadEnum(String crossString) {
        for (CrossroadEnum crossroad : values()) {
            if (crossroad.toString().equals(crossString)) {
                return crossroad;
            }
        }
        return null;
    }

    /**
     * Retorna a porta de comunicação associada ao cruzamento
     * @return Número da porta
     */
    public int getPort() {
        switch (this) {
            case E3: return 5003;
            case Cr3: return 6003;
            case S: return 7001;
            default: return -1;
        }
    }
}