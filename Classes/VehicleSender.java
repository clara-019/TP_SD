package Classes;

import Enums.CrossroadEnum;

public class VehicleSender extends Thread {
    CrossroadEnum crossroad;
    SynchronizedQueue<Vehicle> vehicleToSendQueue;

    public VehicleSender(SynchronizedQueue<Vehicle> vehicleToSendQueue, CrossroadEnum crossroad) {
        this.vehicleToSendQueue = vehicleToSendQueue;
        this.crossroad = crossroad;
    }

    @Override
    public void run() {  
        while (true) {

        }
    }
}
