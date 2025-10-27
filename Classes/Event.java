package Classes;

public class Event {
	private Vehicle vehicle;
	private Crossroad origin;
	private Crossroad destination;
	private long time;

	public Event(Vehicle vehicle, Crossroad origin, Crossroad destination, long time) {
		this.vehicle = vehicle;
		this.origin = origin;
		this.destination = destination;
		this.time = time;
	}

	public Vehicle getVehicle() {
		return vehicle;
	}

	public Crossroad getOrigin() {
		return origin;
	}

	public Crossroad getDestination() {
		return destination;
	}

	public long getTime() {
		return time;
	}
}
