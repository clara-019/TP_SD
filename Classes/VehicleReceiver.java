package Classes;

import Enums.CrossroadEnum;

public class VehicleReceiver extends Thread {

    private CrossroadEnum crossroad;
    private SynchronizedQueue<Vehicle> queue;


    public VehicleReceiver(SynchronizedQueue<Vehicle> queue, CrossroadEnum crossroad) {
        this.queue = queue;
        this.crossroad = crossroad;
    }

    @Override
    public void run() {
       
    }

}