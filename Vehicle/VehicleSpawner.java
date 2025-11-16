package Vehicle;

import Enums.*;
import Utils.SynchronizedQueue;

import java.util.Random;
import java.time.Instant;

public class VehicleSpawner extends Thread {
    private SynchronizedQueue<Vehicle> vehiclesToSend;
    private boolean running;

    public VehicleSpawner(SynchronizedQueue<Vehicle> vehiclesToSend, boolean running) {
        this.vehiclesToSend = vehiclesToSend;
        this.running = running;
    }

    @Override
    public void run() {
        int counter = 0;
        while(running){
            try {
            Vehicle newVehicle = createNewVehicle("Vehicle_" + counter);
            vehiclesToSend.add(newVehicle);
            Thread.sleep(5000);
            counter++;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private Vehicle createNewVehicle(String id){
        // preserve provided id, randomize other attributes
        Random rnd = new Random();

        // Random vehicle type
        VehicleTypes[] types = VehicleTypes.values();
        VehicleTypes type = types[rnd.nextInt(types.length)];

        // Random path (choose from available PathEnum values)
        PathEnum[] paths = PathEnum.values();
        PathEnum path = paths[rnd.nextInt(paths.length)];

        Vehicle v = new Vehicle(id, type, path);
        v.setEntranceTime((int) (Instant.now().getEpochSecond() & Integer.MAX_VALUE));
        v.setExitTime(0);
        return v;
    }
}
