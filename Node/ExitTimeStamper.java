package Node.Exit;

import Utils.*;
import Vehicle.*;

public class ExitTimeStamper extends Thread {
    private SynchronizedQueue<Vehicle> vehiclesToStamp;
    private SynchronizedQueue<Vehicle> vehicleToSendQueue;

    public ExitTimeStamper(SynchronizedQueue<Vehicle> vehiclesToStamp,
            SynchronizedQueue<Vehicle> vehicleToSendQueue) {
        this.vehiclesToStamp = vehiclesToStamp;
        this.vehicleToSendQueue = vehicleToSendQueue;
    }

    @Override
    public void run() {
        while (true) {
            Vehicle vehicle = vehiclesToStamp.remove();
            if (vehicle != null) {
                vehicle.setExitTime((int) System.currentTimeMillis());
                vehicleToSendQueue.add(vehicle);
            }
        }
    }
}