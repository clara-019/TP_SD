package Comunication;

import Utils.*;
import Vehicle.*;
import Event.*;

import java.net.Socket;


public class Sender extends Thread {
    private final SynchronizedQueue<Vehicle> vehiclesToSend;
    private final int port;

    public Sender(SynchronizedQueue<Vehicle> vehiclesToSend, int port) {
        this.vehiclesToSend = vehiclesToSend;
        this.port = port;
    }

    @Override
    public void run() {
        System.out.println("[Sender] A enviar veículos");

        try {
            Socket socket = new Socket("localhost", port);

            while (true) {
                try {
                    Vehicle vehicle = vehiclesToSend.remove();

                    if (vehicle != null) {
                        vehicle.setOriginRoad(vehiclesToSend.getRoad());
                        ComunicationUtils.sendObject(socket, new Event(vehicle, System.currentTimeMillis()));
                        System.out.println("[Sender] Veículo " + vehicle.getId() + " enviado para " + port
                                + " (port=" + port + ")");
                    }

                    Thread.sleep(300);

                } catch (InterruptedException e) {
                    System.err.println("[Sender] Thread interrompida: " + e.getMessage());
                    break;
                }
            }

            socket.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
