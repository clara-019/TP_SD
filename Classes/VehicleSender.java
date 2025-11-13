package Classes;

import Enums.*;
import java.util.*;
import java.io.PrintWriter;
import java.net.Socket;

public class VehicleSender extends Thread {
    private final SynchronizedQueue<Vehicle> vehiclesToSend;
    private final CrossroadEnum crossroad;

    public VehicleSender(SynchronizedQueue<Vehicle> vehiclesToSend) {
        this(vehiclesToSend, null);
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
                // Retira um veículo da fila (bloqueia até haver um)
                Vehicle vehicle = vehiclesToSend.remove();

                if (vehicle != null) {
                    // Envia o veículo para os próximos cruzamentos ao longo do seu path
                    List<CrossroadEnum> path = vehicle.getPath().getPath();

                    // Tenta encontrar a posição atual do veículo no path usando originRoad
                    int startIndex = -1;
                    if (vehicle.getOriginRoad() != null) {
                        try {
                            String[] parts = vehicle.getOriginRoad().name().split("_");
                            if (parts.length >= 2) {
                                String destCode = parts[1]; // ex: CR3
                                String normalized = destCode.substring(0,1) + destCode.substring(1).toLowerCase(); // CR3 -> Cr3
                                CrossroadEnum originCross = CrossroadEnum.valueOf(normalized);
                                startIndex = path.indexOf(originCross);
                            }
                        } catch (Exception ignored) {
                            startIndex = -1;
                        }
                    }

                    // Se não encontramos, começamos no início do path (index 0)
                    if (startIndex < 0) startIndex = 0;

                    for (int i = startIndex + 1; i < path.size(); i++) {
                        CrossroadEnum nextCross = path.get(i);
                        int port = nextCross.getPort();
                        sendVehicleToPort(vehicle, port);

                        // Atualiza a origem do veículo para a road correspondente (não obrigatoriamente usada aqui)
                        // Se quiseres mapear a road, podemos procurar o RoadEnum correspondente.

                        System.out.println("[Sender] Veículo " + vehicle.getId() + " enviado para " + nextCross + " (port=" + port + ")");
                    }
                }

                // Pequena pausa para evitar consumo de CPU excessivo
                Thread.sleep(200);

            } catch (InterruptedException e) {
                System.err.println("[Sender] Thread interrompida: " + e.getMessage());
                break;
            }
        }
    }

    private void sendVehicleToPort(Vehicle vehicle, int port) {
        try (Socket socket = new Socket("localhost", port);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {

            // Envia o veículo como um todo em linhas (id, type, path, originRoad)
            out.println(vehicle.getId());
            out.println(vehicle.getType() != null ? vehicle.getType().getTypeToString() : "");
            out.println(vehicle.getPath() != null ? vehicle.getPath().name() : "");
            out.println(vehicle.getOriginRoad() != null ? vehicle.getOriginRoad().toString() : "");

        } catch (Exception e) {
            System.err.println("[Sender] Erro ao enviar veículo para porta " + port + ": " + e.getMessage());
        }
    }
}
