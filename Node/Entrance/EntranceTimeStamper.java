package Node;

import Utils.SynchronizedQueue;
import Vehicle.*;

public class EntranceTimeStamper extends Thread {
    private SynchronizedQueue<Vehicle> vehiclesToStamp;
    private SynchronizedQueue<Vehicle> vehicleToSendQueue;

    public EntranceTimeStamper(SynchronizedQueue<Vehicle> vehiclesToStamp,
            SynchronizedQueue<Vehicle> vehicleToSendQueue) {
        this.vehiclesToStamp = vehiclesToStamp;
        this.vehicleToSendQueue = vehicleToSendQueue;
    }

    @Override
    public void run() {
        while (true) {
            Vehicle vehicle = vehiclesToStamp.remove();
            if (vehicle != null) {
                vehicle.setEntranceTime((int) System.currentTimeMillis());
                vehicleToSendQueue.add(vehicle);
            }
        }
    }
}