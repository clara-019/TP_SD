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
        int attempts = 0;
        while (attempts < 5 && running) {
            try {
                serverSocket = new ServerSocket(port);
                System.out.printf("[%s] [Receiver-%s] Listening on port %d%n",
                        LocalTime.now(), receiverName, port);

                while (running) {
                    Socket socket = serverSocket.accept();
                    new ClientHandler(socket).start();
                }
                break;
            } catch (java.net.BindException be) {
                attempts++;
                System.err.printf("[%s] [Receiver-%s] Port %d bind failed (attempt %d): %s%n",
                        LocalTime.now(), receiverName, port, attempts, be.getMessage());
                try { Thread.sleep(1000); } catch (InterruptedException ignored) { break; }
            } catch (IOException e) {
                e.printStackTrace();
                break;
            }
        }
    }

    private class ClientHandler extends Thread {
        private final Socket socket;

        ClientHandler(Socket socket) {
            this.socket = socket;
            setName("LogClientHandler");
            setDaemon(true);
        }

        @Override
        public void run() {
            ObjectInputStream ois = null;
            try {
                ois = new ObjectInputStream(socket.getInputStream());
                while (running && !socket.isClosed()) {
                    try {
                        Object obj = ois.readObject();
                        if (obj == null) continue;
                        System.out.printf("[%s] [Receiver-%s] Event received on port %d: %s%n",
                                LocalTime.now(), receiverName, port, obj.toString());

                        if (obj instanceof Event) {
                            Event event = (Event) obj;
                            Vehicle vehicle = event.getVehicle();
                            queue.add(vehicle);
                        }
                    } catch (Exception ignore) {
                    }
                }
            } catch (IOException e) {
                System.err.printf("[%s] [Receiver-%s] Failed to create input stream on port %d: %s%n", LocalTime.now(), receiverName, port, e.getMessage());
            } finally {
                try { if (ois != null) ois.close(); } catch (IOException ignored) {}
                try { if (socket != null && !socket.isClosed()) socket.close(); } catch (IOException ignored) {}
            }
        }
    }
}