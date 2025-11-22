package Comunication;

import java.io.ObjectOutputStream;
import java.net.Socket;

import Event.*;
import Vehicle.Vehicle;
import Node.NodeEnum;
import Utils.LogicalClock;

public class Sender{

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

    public static void sendVehicleDeparture(Vehicle v, int destPort, NodeEnum node, LogicalClock clock) {
        Event event = new VehicleEvent(EventType.VEHICLE_DEPARTURE, v, node, clock.tick());
        sendToEventHandler(event);
        sendVehicle(event, destPort);
    }
}
