package Road;

import java.util.*;

import Node.*;

/**
 * Enumeração que representa todas as estradas disponíveis no sistema
 * Cada estrada conecta dois cruzamentos
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
     * Retorna o cruzamento de origem da estrada
     * @return Cruzamento de origem
     */
    public NodeEnum getOrigin() {
        String[] parts = this.name().split("_");
        return NodeEnum.toNodeEnum(parts[0]);
    }

    /**
     * Retorna o cruzamento de destino da estrada
     * @return Cruzamento de destino
     */
    public NodeEnum getDestination() {
        String[] parts = this.name().split("_");
        return NodeEnum.toNodeEnum(parts[1]);
    }

    /**
     * Retorna todas as estradas que chegam a um determinado cruzamento
     * @param crossroadEnum Cruzamento de destino
     * @return Lista de estradas que chegam ao cruzamento
     */
    public static List<RoadEnum> getRoadsToCrossroad(NodeEnum crossroadEnum) {
        List<RoadEnum> roads = new ArrayList<>();
        for (RoadEnum road : values()) {
            if (road.getDestination() == crossroadEnum) {
                roads.add(road);
            }
        }
        return roads;
    }
}