package Comunication;

import java.io.*;
import java.net.*;

import Event.*;
import Utils.*;
import Vehicle.*;
import Node.NodeEnum;

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

    /**
     * Main loop: accepts connections on the configured port, reads incoming
     * vehicle events, forwards a corresponding arrival event to the
     * {@link EventHandler} (updating the logical clock), and enqueues the
     * received vehicle into the local queue.
     */
    @Override
    public void run() {
        try {
            serverSocket = new ServerSocket(port);
            while (running) {
                Socket socket = serverSocket.accept();
                ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
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