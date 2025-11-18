package Vehicle;

import java.util.*;

import Node.*;

/**
 * Enumeração que define os percursos possíveis para os veículos
 * Cada percurso é uma sequência de cruzamentos
 */
public enum PathEnum {
    E3_CR3_S; // Percurso: E3 → CR3 → S

    /**
     * Retorna a sequência de cruzamentos do percurso
     * @return Lista ordenada de cruzamentos
     */
    public List<NodeEnum> getPath() {
        switch (this) {
            case E3_CR3_S:
                return Arrays.asList(NodeEnum.E3, NodeEnum.CR3, NodeEnum.S);
            default:
                return Collections.emptyList();
        }
    }

    /**
     * Retorna a representação em string do percurso
     */
    @Override
    public String toString() {
        switch (this) {
            case E3_CR3_S: return "E3 → CR3 → S";
            default: return "Percurso Desconhecido";
        }
    }
}