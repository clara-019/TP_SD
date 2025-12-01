package Event;

import Node.NodeEnum;
import Vehicle.Vehicle;

/**
 * An event related to a {@link Vehicle} in the simulation.
 * <p>
 * This event represents actions or state changes associated with a
 * vehicle (for example: creation, arrival at a traffic light or road,
 * departure, or exit). It records the {@link EventType type} of the event, the
 * {@link Node.NodeEnum node} where it occurs and the logical timestamp
 * inherited from {@link Event}.
 */
public class VehicleEvent extends Event {
    private final Vehicle vehicle;

    /**
     * Event associated with a vehicle (creation, arrival at road/signal,
     * exit, etc.).
     *
     * @param type    event type
     * @param node    node where the event occurs
     * @param time    logical clock / event timestamp
     * @param vehicle associated vehicle
     */
    public VehicleEvent(EventType type, NodeEnum node, long time, Vehicle vehicle) {
        super(type, node, time);
        this.vehicle = vehicle;
    }

    /**
     * Returns the vehicle associated with the event.
     *
     * @return {@link Vehicle}
     */
    public Vehicle getVehicle() {
        return vehicle;
    }

    @Override
    public String toString() {
        return String.format("VehicleEvent[%s, vehicle=%s]", super.toString(), vehicle.getId());
    }

}
