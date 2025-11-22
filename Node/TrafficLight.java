package Node;

import java.util.List;

import Comunication.Sender;
import Event.SignalChangeEvent;
import Utils.LogicalClock;
import Utils.RoundRobin;
import Utils.SynchronizedQueue;
import Vehicle.Vehicle;

public class TrafficLight extends Thread {
    private static final int GREEN_LIGHT_DURATION_MS = 5000;

    private static final long TIME_TO_PASS_MS = 1000;

    private final SynchronizedQueue<Vehicle> vehicleQueue;
    private final RoadEnum road;
    private final LogicalClock clock;
    private final RoundRobin roundRobin;

    public TrafficLight(SynchronizedQueue<Vehicle> vehicleQueue,
            RoadEnum road,
            LogicalClock clock, RoundRobin roundRobin) {
        this.vehicleQueue = vehicleQueue;
        this.road = road;
        this.clock = clock;
        this.roundRobin = roundRobin;
    }

    @Override
    public void run() {
        NodeEnum currentNode = road.getDestination();
        while (true) {
            try {
                roundRobin.esperarTurno(road);

                long greenStartTime = System.currentTimeMillis();
                long greenEndTime = greenStartTime + GREEN_LIGHT_DURATION_MS;

                System.out.println("Traffic Light GREEN for: " + road);
                Sender.sendToEventHandler(
                        new SignalChangeEvent(currentNode, clock.get(), "green"));

                while (true) {
                    long now = System.currentTimeMillis();
                    if (now >= greenEndTime) {
                        break;
                    }
                    Vehicle vehicle = vehicleQueue.peek();
                    if (vehicle == null) {
                        Thread.sleep(50);
                        continue;
                    }
                    long passTimeMs = vehicle.getType().getTimeToPass(TIME_TO_PASS_MS);
                    if (now + passTimeMs > greenEndTime) {
                        break;
                    }
                    Thread.sleep(passTimeMs);
                    
                    vehicleQueue.remove();
                    List<NodeEnum> path = vehicle.getPath().getPath();
                    int idx = path.indexOf(currentNode);

                    if (idx == -1 || idx + 1 >= path.size()) {
                        System.err.println("TrafficLight: caminho inválido para veículo " + vehicle.getId());
                    } else {
                        NodeEnum nextNode = path.get(idx + 1);
                        Sender.sendVehicleDeparture(vehicle,
                                nextNode.getPort(),
                                currentNode,
                                clock);
                    }
                    System.out.println("Vehicle " + vehicle.getId()
                            + " passed GREEN at TL: " + road);
                }
                System.out.println("Traffic Light RED for: " + road);
                Sender.sendToEventHandler(
                        new SignalChangeEvent(currentNode, clock.get(), "red"));

                roundRobin.terminarTurno();

            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
