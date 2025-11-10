package Classes;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;

import Enums.CrossroadEnum;
import Enums.RoadEnum;

public class VehicleReceiver extends Thread {

    private final CrossroadEnum crossroad;
    private final RoadEnum road;
    private final List<SynchronizedQueue<Vehicle>> queues;

    public VehicleReceiver(List<SynchronizedQueue<Vehicle>> queues, CrossroadEnum crossroad) {
        this.queues = queues;
        this.crossroad = crossroad;
        this.road = null;
    }

    public VehicleReceiver(List<SynchronizedQueue<Vehicle>> queues, RoadEnum road) {
        this.queues = queues;
        this.road = road;
        this.crossroad = null;
    }

    @Override
    public void run() {
        int port = (crossroad != null) ? crossroad.getPort() : road.getPort();

        // Thread paralela para escutar mensagens na porta
        new Thread(() -> listenForVehicles(port)).start();

        // Thread principal processa a queue
        while (true) {
            for (SynchronizedQueue<Vehicle> queue : queues) {
                Vehicle vehicle = queue.remove();
                if (vehicle != null) {
                    System.out.println("Vehicle " + vehicle.getId() + " has crossed " +
                        (crossroad != null ? crossroad : road));
                }
            }

            try {
                Thread.sleep(300);
            } catch (InterruptedException e) {
                break;
            }
        }
    }

    private void listenForVehicles(int port) {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("[Receiver] Listening on port " + port + " (" +
                (crossroad != null ? crossroad : road) + ")");

            while (true) {
                Socket socket = serverSocket.accept();
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                String message = in.readLine();

                if (message != null && !message.isEmpty()) {
                    // Por agora assume-se que a mensagem é apenas o ID
                    int id = Integer.parseInt(message.trim());
                    Vehicle vehicle = new Vehicle(id);

                    // Adiciona à primeira fila (ou decide com base na direção, etc.)
                    queues.get(0).insert(vehicle);
                    System.out.println("[Receiver] Vehicle " + id + " received and queued at " +
                        (crossroad != null ? crossroad : road));
                }

                socket.close();
            }

        } catch (Exception e) {
            System.err.println("[Receiver] Error on " + (crossroad != null ? crossroad : road) +
                ": " + e.getMessage());
        }
    }
}