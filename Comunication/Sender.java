package Comunication;

import Utils.*;
import Vehicle.*;
import Event.*;

import java.net.Socket;
import java.io.IOException;
import java.io.ObjectOutputStream;

public class Sender extends Thread {
    private final SynchronizedQueue<Vehicle> vehiclesToSend;
    private final int port;

    public Sender(SynchronizedQueue<Vehicle> vehiclesToSend, int port) {
        this.vehiclesToSend = vehiclesToSend;
        this.port = port;
    }


    @Override
    public void run() {
        while (!Thread.currentThread().isInterrupted()) {
            Socket socket = null;
            try {
                socket = new Socket("localhost", port);
                System.out.println("[Sender] Connected to port " + port + " - sending vehicles");

                ObjectOutputStream out = null;
                try {
                    out = new ObjectOutputStream(socket.getOutputStream());
                    while (!Thread.currentThread().isInterrupted()) {
                        try {
                            Vehicle vehicle = vehiclesToSend.remove();
                            if (vehicle != null) {
                                out.writeObject(new Event(vehicle, System.currentTimeMillis()));
                                out.flush();
                                System.out.println("[Sender] Vehicle " + vehicle.getId() + " sent to port " + port);
                            }
                            Thread.sleep(300);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            break;
                        }
                    }
                } finally {
                    if (out != null) {
                        try {
                            out.close();
                        } catch (IOException ignored) {
                        }
                    }
                }

            } catch (Exception e) {
                System.err.println("[Sender] Could not connect to localhost:" + port + " - " + e.getMessage());
            } finally {
                if (socket != null) {
                    try {
                        socket.close();
                    } catch (IOException ignored) {
                    }
                }
            }
        }
    }
}
