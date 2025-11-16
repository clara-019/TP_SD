package Comunication;

import java.io.*;
import java.net.*;
import java.time.LocalTime;
import java.util.*;

import Crossroad.*;
import Event.*;
import Road.RoadEnum;
import Utils.SynchronizedQueue;
import Vehicle.*;

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

    public void stopReceiver() {
        running = false;
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (Exception ignored) {
        }
        this.interrupt();
    }

    @Override
    public void run() {
        int port;
        String receiverName;
        if (crossroad == null && road == null) {
            port = DEFAULT_PORT;
            receiverName = "SimulatorReceiver";
        } else {
            port = (crossroad != null) ? crossroad.getPort() : road.getPort();
            receiverName = (crossroad != null) ? crossroad.toString() : road.toString();
        }

        try {
            System.out.println("[" + receiverName + "] A receber veículos na porta " + port);
            serverSocket = new ServerSocket(port);
            System.out.printf("[%s] [Receiver-%s] Listening on port %d%n",
                    LocalTime.now(), receiverName, port);

            while (running) {
                Socket socket = serverSocket.accept();
                ObjectInputStream in = new ObjectInputStream(socket.getInputStream());

                Object obj = in.readObject();
                Event event;
                System.out.printf("[%s] [Receiver-%s] Evento recebido na porta %d: %s%n",
                        LocalTime.now(), receiverName, port, obj.toString());
                if (obj instanceof Event) {
                    event = (Event) obj;
                    Vehicle vehicle = event.getVehicle();

                    if (crossroad != null) {
                        if (queues.size() == 1) {
                            queues.get(0).add(vehicle);
                            continue;
                        }
                        for (SynchronizedQueue<Vehicle> queue : queues) {
                            if (queue.getRoad() != null
                                    && vehicle.getOriginRoad().toString().equals(queue.getRoad().toString())) {
                                queue.add(vehicle);
                                continue;
                            }
                        }
                    
                    } else {
                        vehicle.setExitTime((int) (java.time.Instant.now().getEpochSecond() & Integer.MAX_VALUE));
                    }

                    try {
                        Thread.sleep(300);
                    } catch (InterruptedException e) {
                        running = false;
                    }
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }

    }
}
