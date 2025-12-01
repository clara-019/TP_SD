package Event;

import Node.NodeEnum;

/**
 * Classe abstrata que representa um evento genérico no sistema distribuído.
 * Todos os eventos enviados entre nós (como chegada ou partida de veículos)
 * herdam desta classe.
 *
 * Implementa Serializable para permitir envio através de sockets TCP.
 */

public abstract class Event implements java.io.Serializable {
    private static final long serialVersionUID = 1L;
    private final NodeEnum node;
    private final EventType type;
    private final long logicalClock;

 /**
     * Construtor base para qualquer evento.
     *
     * @param type          tipo do evento
     * @param node          nó responsável pelo evento
     * @param logicalClock  timestamp lógico do evento (Lamport clock)
     */

    public Event(EventType type, NodeEnum node, long logicalClock) {
        this.type = type;
        this.node = node;
        this.logicalClock = logicalClock;
    }

    public EventType getType() {
        return type;
    }

    public NodeEnum getNode() {
        return node;
    }

    public long getLogicalClock() {
        return logicalClock;
    }

 /**
     * Representação em String útil para debug e logs.
     */

    @Override
    public String toString() {
        return String.format("Event[type=%s, node=%s, logicalClock=%d]", type, node, logicalClock);
    }
}
