package Road;

import java.io.*;
import java.net.*;
import java.time.LocalTime;

import Event.*;
import Utils.SynchronizedQueue;
import Vehicle.*;

public class Receiver extends Thread {
    private final RoadEnum road;
    private final SynchronizedQueue<Vehicle> queue;
    private volatile boolean running = true;
    private ServerSocket serverSocket;

    public Receiver(SynchronizedQueue<Vehicle> queue, RoadEnum road) {
        this.queue = queue;
        this.road = road;
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
        int port = road.getPort();
        String receiverName = road.toString();

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
                    queue.add(vehicle);
                }

                try {
                    Thread.sleep(300);
                } catch (InterruptedException e) {
                    running = false;
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }

    }
}
