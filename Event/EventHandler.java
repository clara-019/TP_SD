package Event;

import java.net.*;
import java.util.concurrent.PriorityBlockingQueue;

import java.io.*;

public class EventHandler extends Thread {
    public static final int PORT = 8000;
    private PriorityBlockingQueue<Event> eventQueue;

    public EventHandler(PriorityBlockingQueue<Event> p) {
        eventQueue = p;
    }

    @Override
    public void run() {
        ServerSocket serverSocket = null;
        try {
            serverSocket = new ServerSocket(PORT);
            while (true) {
                try {
                    Socket socket = serverSocket.accept();
                    ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
                    Event event = (Event) in.readObject();
                    processEvent(event);
                    eventQueue.put(event);
                    in.close();
                    socket.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (serverSocket != null && !serverSocket.isClosed()) {
                try {
                    serverSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void processEvent(Event event) {
        if (event.getType() == EventType.NEW_VEHICLE){
            VehicleEvent ve = (VehicleEvent) event;
            ve.getVehicle().setEntranceTime(System.currentTimeMillis());
        }else if (event.getType() == EventType.VEHICLE_EXIT){
            VehicleEvent ve = (VehicleEvent) event;
            ve.getVehicle().setExitTime(System.currentTimeMillis());
        }
    }
}