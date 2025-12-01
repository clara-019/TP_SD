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

public class Receiver extends Thread {
    private final SynchronizedQueue<Vehicle> queue;
    private final int port;
    private final NodeEnum node;
    private LogicalClock clock;

    private volatile boolean running = true;
    private ServerSocket serverSocket;

    public Receiver(SynchronizedQueue<Vehicle> queue, int port, NodeEnum node, LogicalClock clock) {
        this.queue = queue;
        this.port = port;
        this.node = node;
        this.clock = clock;
    }

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

    /**
    * Main thread body.
    * Creates a ServerSocket and waits for incoming events,
    * processing each one as it arrives.
    */
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
                // Send the processed event to the EventHandler
                Sender.sendToEventHandler(new VehicleEvent(EventType.VEHICLE_ROAD_ARRIVAL, event.getVehicle(), node,
                        clock.update(event.getLogicalClock())));
                queue.add(event.getVehicle());
            }
        } catch (Exception e) {

        }
        stopReceiver();
    }
}