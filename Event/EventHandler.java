package Event;

import java.io.*;
import java.net.*;
import java.util.concurrent.PriorityBlockingQueue;

/**
 * Central TCP server that receives serialized {@link Event} objects from
 * simulator components and enqueues them into a priority queue.
 * <p>
 * Events received on port {@link #PORT} are placed in the
 * {@code PriorityBlockingQueue<Event>} for consumption by the UI or other
 * components. This thread is intended to run as a single central service
 * within the simulation host.
 */

public class EventHandler extends Thread {
    public static final int PORT = 8000;
    private PriorityBlockingQueue<Event> eventQueue;
    private volatile boolean running = true;

    /**
     * Creates an event handler that listens on the defined port and inserts
     * received events into the priority queue.
     *
     * @param eventQueue event queue (PriorityBlockingQueue)
     * @param running    initial running state of the handler
     */
    public EventHandler(PriorityBlockingQueue<Event> eventQueue, boolean running) {
        this.running = running;
        this.eventQueue = eventQueue;
    }

    /**
     * Stops the handler (causes the main loop to exit).
     */
    public void stopHandler() {
        this.running = false;
    }

    /**
     * Main thread loop: accepts connections on {@link #PORT}, reads an
     * {@link Event} object from the socket stream and inserts it into the
     * event queue.
     * <p>
     * The method blocks on {@code serverSocket.accept()} and will continue
     * until {@link #stopHandler()} is called which flips the {@code running}
     * flag.
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

}