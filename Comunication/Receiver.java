package Comunication;

import java.io.*;
import java.net.*;

import Event.*;
import Utils.*;
import Vehicle.*;
import Node.NodeEnum;

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

    @Override
    public void run() {
        try {
            serverSocket = new ServerSocket(port);
            while (running) {
                Socket socket = serverSocket.accept();
                ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
                VehicleEvent event = (VehicleEvent) in.readObject();
                Sender.sendToEventHandler(new VehicleEvent(EventType.VEHICLE_ARRIVAL, event.getVehicle(), node, clock.update(event.getLogicalClock())));
                queue.add(event.getVehicle());
            }
        } catch (Exception e) {

        }
        stopReceiver();
    }
}