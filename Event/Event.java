package Event;

import Node.NodeEnum;
import Vehicle.Vehicle;

public class Event implements java.io.Serializable {
    private static final long serialVersionUID = 1L;
    private final EventType type;
    private final Vehicle vehicle;
    private final NodeEnum node;
    private final long time; 

    public Event(EventType type, Vehicle vehicle, NodeEnum node, long time) {
        this.type = type;
        this.vehicle = vehicle;
        this.node = node;
        this.time = time;
    }

    public EventType getType() { return type; }
    public Vehicle getVehicle() { return vehicle; }
    public NodeEnum getNode() { return node; }
    public long getTime() { return time; }

    @Override
    public String toString() {
        return String.format("Event[type=%s, veh=%s, node=%s, time=%d]", type, vehicle==null? "null":vehicle.getId(), node, time);
    }
}
