package Comunication;

import Crossroad.*;
import Road.RoadEnum;
import Utils.SynchronizedQueue;
import Vehicle.Vehicle;
import Event.*;

import java.util.*;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class VehicleSender extends Thread {
    private final SynchronizedQueue<Vehicle> vehiclesToSend;
    private final CrossroadEnum crossroad;

    public VehicleSender(SynchronizedQueue<Vehicle> vehiclesToSend) {
        this.vehiclesToSend = vehiclesToSend;
        this.crossroad = null;
    }

    public VehicleSender(SynchronizedQueue<Vehicle> vehiclesToSend, CrossroadEnum crossroad) {
        this.vehiclesToSend = vehiclesToSend;
        this.crossroad = crossroad;
    }

    @Override
    public void run() {
        System.out.println("[Sender] A enviar veículos");

        while (true) {
            try {
                Vehicle vehicle = vehiclesToSend.remove();

                if (vehicle != null) {
                    List<CrossroadEnum> path = vehicle.getPath().getPath();

                    if (crossroad != null && path.size() > path.indexOf(crossroad) + 1) {

                        CrossroadEnum nextCross = path.get(path.indexOf(crossroad) + 1);
                        RoadEnum roadToGo = RoadEnum.toRoadEnum(crossroad.toString() + "_" + nextCross.toString());
                        vehicle.setOriginRoad(null);
                        sendVehicleToPort(vehicle, roadToGo.getPort());

                    } else {
                        sendVehicleToPort(vehicle, path.get(0).getPort());
                    }
                }

                Thread.sleep(300);

            } catch (InterruptedException e) {
                System.err.println("[Sender] Thread interrompida: " + e.getMessage());
                break;
            }
        }
    }

    private void sendVehicleToPort(Vehicle vehicle, int port) {
        try {
            Socket socket = new Socket("localhost", port);
            ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());

            out.writeObject(new Event(vehicle, System.currentTimeMillis()));

            System.out.println("[Sender] Veículo " + vehicle.getId() + " enviado para " + port
                    + " (port=" + port + ")");
            out.close();
            socket.close();
        } catch (

        Exception e) {
            e.printStackTrace();
        }
    }
}
