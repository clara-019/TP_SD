package Event;

import Node.NodeEnum;

/**
 * Base event for the simulator event system.
 * <p>
 * Each event has a type, an associated node, and a logical clock timestamp.
 */
public abstract class Event implements java.io.Serializable {
    private static final long serialVersionUID = 1L;
    private final NodeEnum node;
    private final EventType type;
    private final long logicalClock;

    /**
     * Base constructor for an event.
     *
     * @param type         event type
     * @param node         node associated with the event
     * @param logicalClock logical clock timestamp of the event
     */
    public Event(EventType type, Node.NodeEnum node, long logicalClock) {
        this.type = type;
        this.node = node;
        this.logicalClock = logicalClock;
    }

    /**
     * Returns the event type.
     *
     * @return {@link EventType} of the event
     */
    public EventType getType() {
        return type;
    }

    /**
     * Returns the node associated with the event.
     *
     * @return {@link NodeEnum} of the node
     */
    public NodeEnum getNode() {
        return node;
    }

    /**
     * Returns the logical clock timestamp of the event.
     *
     * @return logical clock timestamp (long)
     */
    public long getLogicalClock() {
        return logicalClock;
    }

    @Override
    public String toString() {
        return String.format("Event[type=%s, node=%s, logicalClock=%d]", type, node, logicalClock);
    }
}
