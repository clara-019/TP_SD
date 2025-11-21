package Comunication;

import java.io.ObjectOutputStream;
import java.net.Socket;

import Event.*;
import Vehicle.Vehicle;
import Node.NodeEnum;

public class Sender {

    public static void sendEvent(Event event) {
        try {
            Socket socket = new Socket("localhost", EventHandler.PORT);
            ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
            out.writeObject(event);
            out.flush();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void sendVehicle(Vehicle v, int destPort) {
        try {
            Socket socket = new Socket("localhost", destPort);
            ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
            out.writeObject(v);
            out.flush();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void sendVehicleDeparture(Vehicle v, int destPort, NodeEnum node) {
        Event event = new VehicleEvent(EventType.VEHICLE_DEPARTURE, v, node, System.currentTimeMillis());
        sendEvent(event);
        sendVehicle(v, destPort);
    }
}
