package Comunication;

import Event.*;
import Node.NodeEnum;
import Utils.LogicalClock;
import Vehicle.Vehicle;
import java.io.ObjectOutputStream;
import java.net.Socket;

/**
 * Helper utility that sends serialized {@link Event} objects over TCP to
 * other components of the simulator.
 */
public class Sender {

    /**
     * Sends an event directly to the central EventHandler (port 8000).
     *
     * @param event serializable event to send
     */
    public static void sendToEventHandler(Event event) {
        try {
            Socket socket = new Socket("localhost", 8000);
            ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
            out.writeObject(event);
            out.flush();
            out.close();
            socket.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Sends an event to a specific node (destination port).
     *
     * @param event    event to send
     * @param destPort TCP port of the destination node
     */
    private static void sendVehicle(Event event, int destPort) {
        try {
            Socket socket = new Socket("localhost", destPort);
            ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
            out.writeObject(event);
            out.flush();
            out.close();
            socket.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Sends a vehicle departure event: notifies both the central EventHandler
     * and the destination node about the vehicle departure.
     *
     * @param v        vehicle that departs
     * @param destPort destination node port
     * @param node     origin node
     * @param clock    logical clock used to timestamp the event
     */
    public static void sendVehicleDeparture(Vehicle v, int destPort, NodeEnum node, LogicalClock clock) {
        Event event = new VehicleEvent(EventType.VEHICLE_DEPARTURE, node, clock.tick(), v);
        sendToEventHandler(event);
        sendVehicle(event, destPort);
    }
}
