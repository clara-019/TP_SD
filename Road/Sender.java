package Road;

import Utils.SynchronizedQueue;
import Vehicle.Vehicle;
import Event.*;

import java.net.Socket;

import Comunication.ComunicationUtils;

public class Sender extends Thread {
    private final SynchronizedQueue<Vehicle> vehiclesToSend;
    private final RoadEnum road;

    public Sender(SynchronizedQueue<Vehicle> vehiclesToSend, RoadEnum road) {
        this.vehiclesToSend = vehiclesToSend;
        this.road = road;
    }

    @Override
    public void run() {
        System.out.println("[Sender] A enviar veículos");
        int port = road.getPort();

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
