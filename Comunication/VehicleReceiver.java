package Comunication;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.LocalTime;
import java.util.List;

import Crossroad.*;
import Enums.*;
import Road.RoadEnum;
import Utils.SynchronizedQueue;
import Vehicle.Vehicle;
import Vehicle.VehicleTypes;

public class VehicleReceiver extends Thread {

    private static final int DEFAULT_PORT = 5000;
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

    public VehicleReceiver(List<SynchronizedQueue<Vehicle>> queues) {
        this.queues = queues;
        this.crossroad = null;
        this.road = null;
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
        int port;
         String receiverName;
        if(crossroad == null && road == null) {
            port = DEFAULT_PORT;
            receiverName = "SimulatorReceiver";
        } else {
            port = (crossroad != null) ? crossroad.getPort() : road.getPort();
            receiverName = (crossroad != null) ? crossroad.toString() : road.toString();
        }

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

                    // Ler 4 linhas: id, type, path, originRoad
                    String idLine = in.readLine();
                    String typeLine = in.readLine();
                    String pathLine = in.readLine();
                    String originRoadLine = in.readLine();

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

                    // If an originRoad was provided, try to map it
                    RoadEnum originRoad = null;
                    if (originRoadLine != null && !originRoadLine.isEmpty()) {
                        originRoad = RoadEnum.toRoadEnum(originRoadLine.trim());
                        if (originRoad == null) {
                            System.err.println("[Receiver] Unknown origin road: '" + originRoadLine + "'");
                        } else {
                            vehicle.setOriginRoad(originRoad);
                        }
                    }

                    // If this receiver represents a Crossroad, route vehicle to the right traffic light queue
                    if (crossroad != null) {
                        // Find the queue matching the origin road (the road from which vehicle arrived)
                        boolean queued = false;
                        if (originRoad != null) {
                            for (SynchronizedQueue<Vehicle> q : queues) {
                                if (q.getRoad() != null && q.getRoad().equals(originRoad)) {
                                    q.add(vehicle);
                                    queued = true;
                                    break;
                                }
                            }
                        }

                        // fallback: put in first queue if we couldn't determine origin
                        if (!queued && !queues.isEmpty()) {
                            queues.get(0).add(vehicle);
                        }

                        System.out.printf("[%s] [Receiver-%s] Vehicle %s received at crossroad and queued (originRoad=%s)%n",
                            LocalTime.now(),
                            crossroad,
                            vehicle.getId(),
                            (vehicle.getOriginRoad() != null ? vehicle.getOriginRoad() : "N/A"));

                    } else {
                        // This is a Road receiver: simulate road travel time, then enqueue for sending
                        // Use a base road travel time (ms)
                        final int BASE_ROAD_TRAVEL_MS = 1000;
                        try {
                            long sleepMs = (vehicle.getType() != null) ? vehicle.getType().getTimeToPass(BASE_ROAD_TRAVEL_MS) : BASE_ROAD_TRAVEL_MS;
                            Thread.sleep(sleepMs);
                        } catch (InterruptedException ie) {
                            // ignore or stop
                        }

                        queues.get(0).add(vehicle);
                        System.out.printf("[%s] [Receiver-%s] Vehicle %s received on road and queued after travel%n",
                                LocalTime.now(),
                                road,
                                vehicle.getId());
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
