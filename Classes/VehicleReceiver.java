package Classes;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.LocalTime;
import java.util.List;

import Enums.CrossroadEnum;
import Enums.RoadEnum;
import Enums.VehicleTypes;
import Enums.PathEnum;

public class VehicleReceiver extends Thread {

    private final CrossroadEnum crossroad;
    private final RoadEnum road;
    private final List<SynchronizedQueue<Vehicle>> queues;
    private volatile boolean running = true;
    private ServerSocket serverSocket;

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

    /** Para o receiver de forma segura */
    public void stopReceiver() {
        running = false;
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (Exception ignored) {}
        this.interrupt();
    }

    @Override
    public void run() {
        int port = (crossroad != null) ? crossroad.getPort() : road.getPort();
        String receiverName = (crossroad != null ? crossroad.toString() : road.toString());

        // Thread separada para escutar ligações TCP
        new Thread(() -> listenForVehicles(port), "ReceiverListener-" + receiverName).start();

        // Thread principal processa a fila
        while (running) {
            for (SynchronizedQueue<Vehicle> queue : queues) {
                Vehicle vehicle = queue.remove();
                if (vehicle != null) {
                    System.out.printf("[%s] [Receiver-%s] Vehicle %s has crossed%n",
                            LocalTime.now(), receiverName, vehicle.getId());
                }
            }

            try {
                Thread.sleep(300);
            } catch (InterruptedException e) {
                running = false;
            }
        }
    }

    private void listenForVehicles(int port) {
        String receiverName = (crossroad != null ? crossroad.toString() : road.toString());

        try {
            serverSocket = new ServerSocket(port);
            System.out.printf("[%s] [Receiver-%s] Listening on port %d%n",
                    LocalTime.now(), receiverName, port);

            while (running) {
                try (Socket socket = serverSocket.accept();
                     BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

                    // Ler 3 linhas: id, type, path
                    String idLine = in.readLine();
                    String typeLine = in.readLine();
                    String pathLine = in.readLine();

                    if (idLine == null || idLine.isEmpty() || typeLine == null || typeLine.isEmpty()) continue;

                    String id = idLine.trim();
                    VehicleTypes type = VehicleTypes.getVehicleTypeFromString(typeLine);
                    PathEnum path = PathEnum.E3_1; // valor por omissão

                    try {
                        if (pathLine != null && !pathLine.isEmpty()) {
                            path = PathEnum.valueOf(pathLine.trim());
                        }
                    } catch (IllegalArgumentException iae) {
                        System.err.printf("[Receiver-%s] Unknown PathEnum: '%s', using default.%n", receiverName, pathLine);
                    }

                    Vehicle vehicle = new Vehicle(id, type, path);

                    if (!queues.isEmpty()) {
                        queues.get(0).add(vehicle);
                    }

                    System.out.printf("[%s] [Receiver-%s] Vehicle %s received and queued%n",
                            LocalTime.now(), receiverName, vehicle.getId());

                } catch (Exception e) {
                    if (running) { // evita spam quando a socket é fechada para parar o receiver
                        System.err.printf("[Receiver-%s] Error receiving vehicle: %s%n", receiverName, e.getMessage());
                    }
                }
            }

        } catch (Exception e) {
            if (running) {
                System.err.printf("[Receiver-%s] Server error on port %d: %s%n", receiverName, port, e.getMessage());
            }
        }
    }
}
