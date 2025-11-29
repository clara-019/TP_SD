package Event;

import Node.NodeEnum;

public abstract class Event implements java.io.Serializable {
    private static final long serialVersionUID = 1L;
    private final NodeEnum node;
    private final EventType type;
    private final long logicalClock;

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

    @Override
    public String toString() {
        return String.format("Event[type=%s, node=%s, logicalClock=%d]", type, node, logicalClock);
    }
}
