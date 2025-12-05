package Event;

import java.io.*;
import java.net.*;
import java.util.concurrent.PriorityBlockingQueue;

/**
 * The EventHandler is a thread responsible for:
 *  - Receiving events sent by various components (Sender, Receiver, etc.)
 *  - Processing certain event types (e.g., NEW_VEHICLE, VEHICLE_EXIT)
 *  - Placing all received events into a PriorityBlockingQueue
 *
 * Acts as a central event "server", always listening on port 8000.
 */

public class EventHandler extends Thread {
    public static final int PORT = 8000;
    // Queue where all received events are stored
    // PriorityBlockingQueue orders events by priority
    private PriorityBlockingQueue<Event> eventQueue;
    private volatile boolean running = true;

    /**
     * Constructor.
     *
     * @param p queue where events will be stored
     * @param running initial state of the EventHandler (true = active)
     */
    public EventHandler(PriorityBlockingQueue<Event> p, boolean running) {
        this.running = running;
        this.eventQueue = p;
    }

    public void stopHandler() {
        this.running = false;
    }

    /**
    * Main method of the thread.
    * Creates a TCP server and continuously receives events while the handler is active.
    */

    @Override
    public void run() {
        ServerSocket serverSocket = null;
        try {
            serverSocket = new ServerSocket(PORT);
            while (running) {
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

     /**
     * Basic processing for certain types of events.
     * Entry and exit times are assigned here.
     *
     * @param event received event that will be checked and possibly modified
     */
    private void processEvent(Event event) {
        // Event for a new vehicle entering the network
        if (event.getType() == EventType.NEW_VEHICLE) {
            VehicleEvent ve = (VehicleEvent) event;
            ve.getVehicle().setEntranceTime(System.currentTimeMillis());
        // Event for a vehicle exiting the network
        } else if (event.getType() == EventType.VEHICLE_EXIT) {
            VehicleEvent ve = (VehicleEvent) event;
            ve.getVehicle().setExitTime(System.currentTimeMillis());
        }
    }
}