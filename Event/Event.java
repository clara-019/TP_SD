package Event;

import java.io.Serializable;

import Vehicle.Vehicle;

public class Event implements Serializable {
	private static final long serialVersionUID = 1L;
	private Vehicle vehicle;
	private long time;

	public Event(Vehicle vehicle, long time) {
		this.vehicle = vehicle;
		this.time = time;
	}

	public Vehicle getVehicle() {
		return vehicle;
	}

	public long getTime() {
		return time;
	}

	@Override
	public String toString() {
		return "Evento[Veículo:" + vehicle.getId() + ", Tempo: " + time + "]";
	}
}
