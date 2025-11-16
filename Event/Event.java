package Event;

import Crossroad.*;
import Vehicle.Vehicle;

public class Event {
	private Vehicle vehicle;          // Veículo envolvido no evento
	private CrossroadEnum location;   // Local onde acontece o evento
	private long time;                // Tempo em que acontece
	private String type;              // Tipo do evento

	// Construtor
	public Event(Vehicle vehicle, CrossroadEnum location, long time, String type) {
		this.vehicle = vehicle;
		this.location = location;
		this.time = time;
		this.type = type;
	}

	// GETTERS - métodos para obter os valores
	public Vehicle getVehicle() {
		return vehicle;
	}
	public CrossroadEnum getLocation() {
		return location;
	}
	public long getTime() {
		return time;
	}
	public String getType() {
		return type;
	}

	// Mostra informações do evento
	@Override
	public String toString() {
		return "Evento[Veículo: " + vehicle.getId() + ", Local: " + location +
				", Tempo: " + time + ", Tipo: " + type + "]";
	}
}
