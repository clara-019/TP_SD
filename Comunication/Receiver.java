package Comunication;

import Event.*;
import Node.NodeEnum;
import Utils.*;
import Vehicle.*;
import java.io.*;
import java.net.*;

/**
 * The Receiver class listens on a TCP port and receives events
 * sent by other nodes in the system. Every time an event related to
 * a vehicle is received, the Receiver updates the logical clock, forwards the event
 * to the EventHandler and adds the received vehicle to a synchronized queue.
 *
 * This class runs in a separate thread and keeps listening until it is
 * explicitly stopped using the `stopReceiver()` method.
 */

/**
 * Thread that receives vehicle events from other nodes using TCP socket and
 * places them into the local queue for processing. It also forwards arrival
 * events to the central {@link EventHandler}, updating the local logical clock
 * based on the received clock.
 */
public class Receiver extends Thread {
    private final SynchronizedQueue<Vehicle> queue;
    private final int port;
    private final NodeEnum node;
    private final LogicalClock clock;

    private volatile boolean running = true;
    private ServerSocket serverSocket;

    /**
     * Constructor for Receiver.
     *
     * @param queue local queue where received vehicles will be placed
     * @param node  logical node associated with this receiver
     * @param clock logical clock used to synchronize event timestamps
     */
    public Receiver(SynchronizedQueue<Vehicle> queue, NodeEnum node, LogicalClock clock) {
        this.queue = queue;
        this.node = node;
        this.port = node.getPort();
        this.clock = clock;
    }

    /**
     * Closes the receiver server socket and stops the thread.
     */
    public void stopReceiver() {
        running = false;
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (Exception ignored) {
        }
        this.interrupt();
    }

    @Override
    public void run() {
        try {
            serverSocket = new ServerSocket(port);
            while (running) {
                 // Waits for a TCP connection (blocks until a client connects)
                Socket socket = serverSocket.accept();
                 // Create a stream to receive objects sent by the client
                ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
                // Read the sent object (assumed to be a VehicleEvent)
                VehicleEvent event = (VehicleEvent) in.readObject();
                Sender.sendToEventHandler(new VehicleEvent(EventType.VEHICLE_ROAD_ARRIVAL, node,
                        clock.update(event.getLogicalClock()), event.getVehicle()));
                queue.add(event.getVehicle());
            }
        } catch (Exception e) {

        }
        stopReceiver();
    }
}