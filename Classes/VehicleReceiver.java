package Classes;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.LocalTime;
import java.util.List;

import Enums.CrossroadEnum;
import Enums.RoadEnum;

public class VehicleReceiver extends Thread {

    private final CrossroadEnum crossroad;
    private final RoadEnum road;
    private final List<SynchronizedQueue<Vehicle>> queues;
    private volatile boolean running = true;

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

    /** Permite parar o receiver de forma segura */
    public void stopReceiver() {
        running = false;
        this.interrupt();
    }

    @Override
    public void run() {
        int port = (crossroad != null) ? crossroad.getPort() : road.getPort();

        // Thread paralela para escutar mensagens na porta
        new Thread(() -> listenForVehicles(port)).start();

        // Thread principal processa a queue
        while (running) {
            for (SynchronizedQueue<Vehicle> queue : queues) {
                Vehicle vehicle = queue.remove();
                if (vehicle != null) {
                    System.out.printf("[%s] [Receiver-%s] Vehicle %s has crossed%n",
                            LocalTime.now(),
                            (crossroad != null ? crossroad : road),
                            vehicle.getId());
                }
            }

            try {
                Thread.sleep(300);
            } catch (InterruptedException e) {
                running = false; // parar de forma segura
            }
        }
    }

    private void listenForVehicles(int port) {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.printf("[%s] [Receiver-%s] Listening on port %d%n",
                    LocalTime.now(),
                    (crossroad != null ? crossroad : road),
                    port);

            while (running) {
                Socket socket = serverSocket.accept();
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                String message = in.readLine();

                if (message != null && !message.isEmpty()) {
                    // Apenas ID simples
                    String id = message.trim();
                    Vehicle vehicle = new Vehicle(id);

                    queues.get(0).add(vehicle);
                    System.out.printf("[%s] [Receiver-%s] Vehicle %s received and queued%n",
                            LocalTime.now(),
                            (crossroad != null ? crossroad : road),
                            vehicle.getId());
                }

                socket.close();
            }

        } catch (Exception e) {
            System.err.println("[Receiver] Error on " +
                    (crossroad != null ? crossroad : road) +
                    ": " + e.getMessage());
        }
    }
}
