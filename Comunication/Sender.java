package Comunication;

import Event.*;
import Node.NodeEnum;
import Utils.LogicalClock;
import Vehicle.Vehicle;
import java.io.ObjectOutputStream;
import java.net.Socket;

/**
 * The Sender class is responsible for sending events over TCP sockets.
 * It can send:
 *  - events to the local EventHandler (port 8000)
 *  - events to other nodes specified by port
 *
 * It is used by the Receiver and other components to communicate
 * across the distributed system.
 */

/**
 * Helper utility that sends serialized {@link Event} objects over TCP to
 * other components of the simulator.
 */
public class Sender {

/**
    * Sends an event to the EventHandler running on the local machine
    * at port 8000.
    *
    * @param event event to send
    */

    /**
     * Sends an event directly to the central EventHandler (port 8001).
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
    * Sends a vehicle departure event.
    *
    * - Creates the departure event
    * - Updates the logical clock (tick())
    * - Sends the event to the EventHandler
    * - Then sends the event to the destination node (port destPort)
    *
    * @param v vehicle that is departing
    * @param destPort port of the node to send the vehicle to
    * @param node current node identifier
    * @param clock logical clock used for distributed event ordering
    */

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
