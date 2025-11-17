package Road;

import Utils.*;
import Vehicle.*;

public class PassRoadThread extends Thread {
    private SynchronizedQueue<Vehicle> vehicleQueue;
    private int passTimeMs;

    public PassRoadThread(SynchronizedQueue<Vehicle> vehicleQueue, int passTimeMs) {
        this.vehicleQueue = vehicleQueue;
        this.passTimeMs = passTimeMs;
    }

    @Override
    public void run() {
        while (true) {
            try {
                Vehicle vehicle = vehicleQueue.remove();
                if (vehicle != null) {
                    Thread.sleep(passTimeMs);
                    System.out.printf("[%s] [PassRoad-%s] Vehicle %s has passed the road%n",
                            java.time.LocalTime.now(),
                            vehicle.getId());
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
