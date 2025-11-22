package Node;

import java.util.List;

import Comunication.Sender;
import Event.SignalChangeEvent;
import Utils.LogicalClock;
import Utils.SynchronizedQueue;
import Vehicle.Vehicle;

public class TrafficLight extends Thread {

    // Duração dos estados do semáforo
    private static final int GREEN_LIGHT_DURATION_MS = 5000;
    private static final int RED_LIGHT_DURATION_MS   = 5000;

    // Tempo base (t_sem) que um CARRO demora a atravessar o semáforo (em ms)
    private static final long TIME_TO_PASS_MS = 1000;

    private final SynchronizedQueue<Vehicle> vehicleQueue;
    private final RoadEnum road;
    private final LogicalClock clock;

    public TrafficLight(SynchronizedQueue<Vehicle> vehicleQueue,
                        RoadEnum road,
                        LogicalClock clock) {
        this.vehicleQueue = vehicleQueue;
        this.road = road;
        this.clock = clock;
    }

    @Override
    public void run() {

        // O nó onde está o semáforo (destino da estrada)
        NodeEnum currentNode = road.getDestination();

        while (true) {
            try {
                // =========================
                //          VERDE
                // =========================
                long greenStartTime = System.currentTimeMillis();
                long greenEndTime   = greenStartTime + GREEN_LIGHT_DURATION_MS;

                System.out.println("Traffic Light GREEN for: " + road);
                Sender.sendToEventHandler(
                        new SignalChangeEvent(currentNode, clock.get(), "Green")
                );

                // Enquanto o semáforo está verde, deixamos passar veículos
                while (true) {
                    long now = System.currentTimeMillis();
                    if (now >= greenEndTime) {
                        // Acabou o verde
                        break;
                    }

                    // Espreita o próximo veículo na fila (não remove ainda)
                    Vehicle vehicle = vehicleQueue.peek();
                    if (vehicle == null) {
                        // Ninguém na fila → espera um bocadinho e volta a tentar
                        Thread.sleep(50);
                        continue;
                    }

                    // Tempo que ESTE tipo de veículo demora a atravessar o semáforo
                    long passTimeMs = vehicle.getType().getTimeToPass(TIME_TO_PASS_MS);

                    // Se não há tempo suficiente neste verde para ele atravessar
                    // deixamos o veículo para o próximo ciclo de verde
                    if (now + passTimeMs > greenEndTime) {
                        break;
                    }

                    // Agora sim: vamos mesmo deixá-lo passar neste verde
                    vehicleQueue.remove(); // remove da fila

                    // Simula o tempo de travessia do semáforo
                    Thread.sleep(passTimeMs);

                    // Determina o próximo nó no caminho do veículo
                    List<NodeEnum> path = vehicle.getPath().getPath();
                    int idx = path.indexOf(currentNode);
                    if (idx == -1 || idx + 1 >= path.size()) {
                        System.err.println("TrafficLight: caminho inválido para veículo " + vehicle.getId());
                    } else {
                        NodeEnum nextNode = path.get(idx + 1);
                        // Envia o evento de saída deste cruzamento para o próximo nó
                        Sender.sendVehicleDeparture(vehicle,
                                nextNode.getPort(),
                                currentNode,
                                clock);
                    }

                    System.out.println("Vehicle " + vehicle.getId()
                            + " passed GREEN at TL: " + road);
                }

                // =========================
                //          VERMELHO
                // =========================
                System.out.println("Traffic Light RED for: " + road);
                Sender.sendToEventHandler(
                        new SignalChangeEvent(currentNode, clock.get(), "Red")
                );

                // Durante o vermelho NÃO tocamos na fila de veículos
                Thread.sleep(RED_LIGHT_DURATION_MS);

            } catch (InterruptedException e) {
                e.printStackTrace();
                // se quiseres parar a thread ao interromper:
                // break;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
