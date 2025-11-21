package Event;

import Node.NodeEnum;

public abstract class Event implements java.io.Serializable {
    private static final long serialVersionUID = 1L;
    private final NodeEnum node;
    private final EventType type;
    private final long time; 

    public Event(EventType type, NodeEnum node, long time) {
        this.type = type;
        this.node = node;
        this.time = time;
    }

    public EventType getType() { return type; }
    public NodeEnum getNode() { return node; }
    public long getTime() { return time; }

    @Override
    public String toString() {
        return String.format("Event[type=%s, node=%s, time=%d]", type, node, time);
    }
}
