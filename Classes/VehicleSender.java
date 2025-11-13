package Classes;

import Enums.*;
import java.util.*;
import java.io.PrintWriter;
import java.net.Socket;

public class VehicleSender extends Thread {
    private final SynchronizedQueue<Vehicle> vehiclesToSend;

    public VehicleSender(SynchronizedQueue<Vehicle> vehiclesToSend) {
        this.vehiclesToSend = vehiclesToSend;
    }

    @Override
    public void run() {
        System.out.println("[Sender] A enviar veículos");

        while (true) {
            try {
                // Retira um veículo da fila (bloqueia até haver um)
                Vehicle vehicle = vehiclesToSend.remove();

                if (vehicle != null) {
                    CrossroadEnum nextCrossroad;
                    if(vehicle.getOriginRoad() != null){
                        List<CrossroadEnum> path = vehicle.getPath().getPath();
                        CrossroadEnum currentCrossroad = vehicle.getOriginRoad().getDestination();
                        nextCrossroad = path.get(path.indexOf(currentCrossroad) + 1);
                    }
                    sendVehicle(vehicle);
                    System.out.println("[Sender] Veículo " + vehicle.getId() + " enviado para " + vehicle);
                }

                // Pequena pausa para evitar consumo de CPU excessivo
                Thread.sleep(200);

            } catch (InterruptedException e) {
                System.err.println("[Sender] Thread interrompida: " + e.getMessage());
                break;
            }
        }
    }

    private void sendVehicle(Vehicle vehicle) {
        try (Socket socket = new Socket("localhost", crossroad.getPort());
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {

            // Envia os campos em linhas separadas para facilitar o parsing sem usar regex
            // Linha 1: id
            // Linha 2: type (nome do enum VehicleTypes)
            // Linha 3: path (nome do enum PathEnum)
            out.println(vehicle.getId());
            // send the human-friendly type string so receiver can map to VehicleTypes
            out.println(vehicle.getType() != null ? vehicle.getType().getTypeToString() : "");
            out.println(vehicle.getPath() != null ? vehicle.getPath().name() : "");
            // Linha 4: origin road (to help the receiving crossroad place vehicle in correct queue)
            out.println(vehicle.getOriginRoad() != null ? vehicle.getOriginRoad().toString() : "");

        } catch (Exception e) {
            System.err.println("[Sender] Erro ao enviar veículo para " + crossroad + ": " + e.getMessage());
        }
    }
}
