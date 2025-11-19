package Comunication;

import java.io.*;
import java.net.*;
import java.time.LocalTime;

import Event.*;
import Utils.SynchronizedQueue;
import Vehicle.*;

public class Receiver extends Thread {
    private final String receiverName;
    private final int port;
    private final SynchronizedQueue<Vehicle> queue;
    private volatile boolean running = true;
    private ServerSocket serverSocket;

    public Receiver(SynchronizedQueue<Vehicle> queue, String receiverName, int port) {
        this.queue = queue;
        this.receiverName = receiverName;
        this.port = port;
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

            System.out.printf("[%s] [Receiver-%s] Listening on port %d%n",
                    LocalTime.now(), receiverName, port);

            while (running) {
                Socket socket = serverSocket.accept();

                Object obj = ComunicationUtils.reciveObject(socket.getInputStream());
                Event event;

                System.out.printf("[%s] [Receiver-%s] Event received on port %d: %s%n",
                        LocalTime.now(), receiverName, port, obj.toString());

                if (obj instanceof Event) {
                    event = (Event) obj;
                    Vehicle vehicle = event.getVehicle();
                    queue.add(vehicle);
                }

                try {
                    Thread.sleep(300);
                } catch (InterruptedException e) {
                    running = false;
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }
}