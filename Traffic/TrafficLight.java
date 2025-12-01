package Traffic;

import java.util.List;

import Comunication.Sender;
import Event.SignalChangeEvent;
import Node.NodeEnum;
import Utils.*;
import Vehicle.Vehicle;

/**
 * Traffic light controller for a single incoming road.
 * <p>
 * This thread interacts with a {@link RoundRobin} coordinator to acquire
 * a green turn for the associated road. During the green interval it allows
 * vehicles to pass subject to their individual pass times. It emits
 * {@link SignalChangeEvent} notifications when the light turns green and
 * red, and uses {@link Sender} to forward vehicle departures
 * to the next node.
 */
public class TrafficLight extends Thread {
    private static final long TIME_TO_PASS_MS = 1000;

    private final SynchronizedQueue<Vehicle> vehicleQueue;
    private final RoadEnum road;
    private final LogicalClock clock;
    private final RoundRobin roundRobin;
    private final NodeEnum node;

    /**
     * Create a traffic light controller for a specific road.
     *
     * @param vehicleQueue queue of vehicles waiting at the light
     * @param road         the {@link RoadEnum} this controller manages
     * @param clock        logical clock used for event timestamps
     * @param roundRobin   round-robin coordinator for turn scheduling
     */
    public TrafficLight(SynchronizedQueue<Vehicle> vehicleQueue,
            RoadEnum road, LogicalClock clock, RoundRobin roundRobin) {
        this.vehicleQueue = vehicleQueue;
        this.road = road;
        this.clock = clock;
        this.roundRobin = roundRobin;
        this.node = road.getDestination();
    }

    /**
     * Main loop: wait for the round-robin turn, announce green, allow
     * vehicles to pass during the green interval, announce red, then
     * release the turn and repeat indefinitely.
     */
    @Override
    public void run() {
        while (true) {
            try {
                this.roundRobin.esperarTurno(RoadEnum.getRoadsToCrossroad(this.node).indexOf(road));

                long greenStartTime = System.currentTimeMillis();
                long greenEndTime = greenStartTime + road.getGreenLightDuration();

                System.out.println("Traffic Light GREEN for: " + road);
                Sender.sendToEventHandler(new SignalChangeEvent(road, clock.get(), "Green"));

                handleGreenLight(greenEndTime);

                System.out.println("Traffic Light RED for: " + this.road);
                Sender.sendToEventHandler(new SignalChangeEvent(this.road, this.clock.get(), "Red"));

                Thread.sleep(200);
                this.roundRobin.terminarTurno();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Allow vehicles to pass while the green interval remains. Vehicles
     * are permitted only if their individual pass time fits within the
     * remaining green window.
     *
     * @param greenEndTime absolute system time in ms when green ends
     * @throws InterruptedException if the thread is interrupted while sleeping
     */
    private void handleGreenLight(long greenEndTime) throws InterruptedException {
        while (true) {
            long now = System.currentTimeMillis();
            if (now >= greenEndTime) {
                break;
            }
            Vehicle vehicle = this.vehicleQueue.peek();
            if (vehicle == null) {
                Thread.sleep(50);
                continue;
            }
            long passTimeMs = vehicle.getType().getTimeToPass(TIME_TO_PASS_MS);
            if (now + passTimeMs > greenEndTime) {
                break;
            }
            Thread.sleep(passTimeMs);
            handleDeparture();
        }
    }

    /**
     * Process a departing vehicle: remove it from the queue, determine the
     * next node from the vehicle path and send a network departure message.
     */
    private void handleDeparture() {
        Vehicle vehicle = this.vehicleQueue.remove();
        List<NodeEnum> path = vehicle.getPath().getPath();
        int idx = path.indexOf(this.node);

        if (idx == -1 || idx + 1 >= path.size()) {
            System.err.println("TrafficLight: caminho inválido para veículo " + vehicle.getId());
        } else {
            NodeEnum nextNode = path.get(idx + 1);
            Sender.sendVehicleDeparture(vehicle, nextNode.getPort(), this.node, this.clock);
        }
        System.out.println("Vehicle " + vehicle.getId() + " passed GREEN at TL: " + this.road);
    }
}
