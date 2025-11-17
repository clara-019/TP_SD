package Vehicle;

import java.util.*;
import Crossroad.*;

/**
 * Enumeração que define os percursos possíveis para os veículos
 * Cada percurso é uma sequência de cruzamentos
 */
public enum PathEnum {
    E3_CR3_S; // Percurso: E3 → Cr3 → S

    /**
     * Retorna a sequência de cruzamentos do percurso
     * @return Lista ordenada de cruzamentos
     */
    public List<CrossroadEnum> getPath() {
        switch (this) {
            case E3_CR3_S:
                return Arrays.asList(CrossroadEnum.E3, CrossroadEnum.Cr3, CrossroadEnum.S);
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
            case E3_CR3_S: return "E3 → Cr3 → S";
            default: return "Percurso Desconhecido";
        }
    }
}