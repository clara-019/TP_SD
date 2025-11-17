package Road;

import java.util.*;

import Node.*;

/**
 * Enumeração que representa todas as estradas disponíveis no sistema
 * Cada estrada conecta dois nós
 */
public enum RoadEnum {
    E3_Cr3, Cr3_S;

    /**
     * Retorna a representação em string da estrada
     */
    public String toString() {
        return this.name();
    }

    /**
     * Converte uma string para o enum correspondente
     * @param roadStr String a converter
     * @return RoadEnum correspondente ou null se não existir
     */
    public static RoadEnum toRoadEnum(String roadStr) {
        for (RoadEnum road : values()) {
            if (road.toString().equals(roadStr)) {
                return road;
            }
        }
        return null;
    }

    /**
     * Retorna a porta de comunicação associada à estrada
     * @return Número da porta
     */
    public int getPort() {
        switch (this) {
            case E3_Cr3: return 5363;
            case Cr3_S: return 6370;
            default: return -1;
        }
    }

    /**
     * Retorna o nó de origem da estrada
     * @return Nó de origem
     */
    public NodeEnum getOrigin() {
        String[] parts = this.name().split("_");
        return NodeEnum.toNodeEnum(parts[0]);
    }

    /**
     * Retorna o nó de destino da estrada
     * @return Nó de destino
     */
    public NodeEnum getDestination() {
        String[] parts = this.name().split("_");
        return NodeEnum.toNodeEnum(parts[1]);
    }

    /**
     * Retorna todas as estradas que chegam a um determinado nó
     * @param node Nó de destino
     * @return Lista de estradas que chegam ao nó
     */
    public static List<RoadEnum> getRoadsToCrossroad(NodeEnum node) {
        List<RoadEnum> roads = new ArrayList<>();
        for (RoadEnum road : values()) {
            if (road.getDestination() == node) {
                roads.add(road);
            }
        }
        return roads;
    }

    /**
     * Retorna todas as estradas que saem de um determinado nó
     * @param node Nó de origem
     * @return Lista de estradas que saem do nó
     */
    public static List<RoadEnum> getRoadsFromCrossroad(NodeEnum node) {
        List<RoadEnum> roads = new ArrayList<>();
        for (RoadEnum road : values()) {
            if (road.getOrigin() == node) {
                roads.add(road);
            }
        }
        return roads;
    }
}