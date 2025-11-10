package Classes;

import Enums.CrossroadEnum;
import java.io.PrintWriter;
import java.net.Socket;

public class VehicleSender extends Thread {
    private final CrossroadEnum crossroad;
    private final SynchronizedQueue<Vehicle> vehicleToSendQueue;

    public VehicleSender(SynchronizedQueue<Vehicle> vehicleToSendQueue, CrossroadEnum crossroad) {
        this.vehicleToSendQueue = vehicleToSendQueue;
        this.crossroad = crossroad;
    }

    @Override
    public void run() {
        System.out.println("[Sender] A enviar veículos para " + crossroad + " (porta " + crossroad.getPort() + ")...");

        while (true) {
            try {
                // Retira um veículo da fila (bloqueia até haver um)
                Vehicle vehicle = vehicleToSendQueue.remove();

                if (vehicle != null) {
                    sendVehicle(vehicle);
                    System.out.println("[Sender] Veículo " + vehicle.getId() + " enviado para " + crossroad);
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

            // Simplesmente envia o ID — podes depois mudar para JSON
            out.println(vehicle.getId());

        } catch (Exception e) {
            System.err.println("[Sender] Erro ao enviar veículo para " + crossroad + ": " + e.getMessage());
        }
    }
}
