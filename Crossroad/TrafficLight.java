package Crossroad;

import java.util.*;

import Enums.*;
import Road.RoadEnum;
import Utils.SynchronizedQueue;
import Vehicle.Vehicle;

public class TrafficLight extends Thread{
    private static final int GREEN_LIGHT_DURATION_MS = 5000;
    private static final int YELLOW_LIGHT_DURATION_MS = 2000;
    private static final int RED_LIGHT_DURATION_MS = 5000;
    private static final int TIME_TO_PASS_MS = 1000;

    private SynchronizedQueue<Vehicle> vehicleQueue;
    private SynchronizedQueue<Vehicle> vehicleToSendQueue;
    private RoadEnum road;

    public TrafficLight(SynchronizedQueue<Vehicle> vehicleToSendQueue,SynchronizedQueue<Vehicle> vehicleQueue, RoadEnum road) {
        this.vehicleToSendQueue = vehicleToSendQueue;
        this.vehicleQueue = vehicleQueue;
        this.road = road;
    }

    @Override
    public void run() {
        while (true) {
            try {
                // Green Light
                System.out.println("Traffic Light GREEN for Traffic Light: " + road.toString());
                long greenEndTime = System.currentTimeMillis() + GREEN_LIGHT_DURATION_MS;
                while (System.currentTimeMillis() < greenEndTime) {
                    Vehicle vehicle = vehicleQueue.remove();
                    if (vehicle != null) {
                        Thread.sleep(vehicle.getType().getTimeToPass(TIME_TO_PASS_MS));
                        vehicleToSendQueue.add(vehicle);
                        System.out.println("Vehicle " + vehicle.getId() + " is passing through Traffic Light: " + road.toString());
                    }
                }

                // Yellow Light
                System.out.println("Traffic Light YELLOW for Traffic Light: " + road.toString());
                Thread.sleep(YELLOW_LIGHT_DURATION_MS);

                // Red Light
                System.out.println("Traffic Light RED for Traffic Light: " + road.toString());
                Thread.sleep(RED_LIGHT_DURATION_MS);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}