package Vehicle;

import Utils.SynchronizedQueue;

import java.util.*;
import java.time.*;

/**
 * Thread responsible for creating random vehicles periodically
 * Generates different types of vehicles with random paths
 */
public class VehicleSpawner extends Thread {
    private SynchronizedQueue<Vehicle> vehiclesToSend;
    private volatile boolean running;
    private int spawnIntervalMs;

    public VehicleSpawner(SynchronizedQueue<Vehicle> vehiclesToSend, boolean running, int spawnIntervalMs) {
        this.vehiclesToSend = vehiclesToSend;
        this.running = running;
        this.spawnIntervalMs = spawnIntervalMs;
    }

    @Override
    public void run() {
        int vehicleCounter = 0;
        Random random = new Random();

        System.out.println("[VehicleSpawner] Starting vehicle spawner");

        while(running){
            try {
                Vehicle newVehicle = createNewVehicle("V" + vehicleCounter, random);
                vehiclesToSend.add(newVehicle);

                System.out.println("[VehicleSpawner] Vehicle created: " + newVehicle.getId() +
                        " Type: " + newVehicle.getType() +
                        " Path: " + newVehicle.getPath());

                vehicleCounter++;
                Thread.sleep(spawnIntervalMs);

            } catch (InterruptedException e) {
                System.out.println("[VehicleSpawner] Interrupted");
                running = false;
            }
        }

        System.out.println("[VehicleSpawner] Finished. Total vehicles created: " + vehicleCounter);
    }

    /**
     * Creates a new vehicle with random characteristics
     * @param id Unique vehicle identifier
     * @param random Random number generator
     * @return New created vehicle
     */
    private Vehicle createNewVehicle(String id, Random random){
        VehicleTypes[] types = VehicleTypes.values();
        VehicleTypes type = types[random.nextInt(types.length)];

        PathEnum[] paths = PathEnum.values();
        PathEnum path = paths[random.nextInt(paths.length)];

        Vehicle vehicle = new Vehicle(id, type, path);
        vehicle.setEntranceTime((int) (Instant.now().getEpochSecond() & Integer.MAX_VALUE));

        return vehicle;
    }

    /**
     * Stops vehicle generation
     */
    public void stopSpawning() {
        this.running = false;
        this.interrupt();
    }
}