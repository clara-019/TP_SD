package Event;

import Node.NodeEnum;
import Vehicle.Vehicle;

public class VehicleEvent extends Event {
    private final Vehicle vehicle;

    public VehicleEvent(EventType type, Vehicle vehicle, NodeEnum node, long time) {
        super(type, node, time);
        this.vehicle = vehicle;
    }

    public Vehicle getVehicle() {
        return vehicle;
    }

    @Override
    public String toString() {
        return String.format("VehicleEvent[%s, vehicle=%s]", super.toString(), vehicle.getId());
    }

}
