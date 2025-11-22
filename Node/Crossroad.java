package Node;

import java.util.*;

import Comunication.*;
import Utils.LogicalClock;
import Utils.SynchronizedQueue;
import Vehicle.Vehicle;

public class Crossroad {
    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("Please provide a crossroad string as an argument.");
            return;
        }
        String crossId = args[0];
        NodeEnum crossroad = NodeEnum.toNodeEnum(crossId);

        if (crossroad == null || crossroad.getType() != NodeType.CROSSROAD) {
            System.out.println("Invalid crossroad node: " + crossId);
            return;
        }

        LogicalClock clock = new LogicalClock();

        // Estradas que chegam a este cruzamento
        List<RoadEnum> roadsToCrossroad = RoadEnum.getRoadsToCrossroad(crossroad);

        // Fila de veículos por estrada (usada pelo TrafficSorter e pelos semáforos)
        Map<RoadEnum, SynchronizedQueue<Vehicle>> trafficQueues = new HashMap<>();

        // Fila de veículos que chegam do Receiver e ainda têm de ser encaminhados
        SynchronizedQueue<Vehicle> vehiclesToSort = new SynchronizedQueue<>();

        for (RoadEnum road : roadsToCrossroad) {
            // Fila de veículos que chegam a ESTE cruzamento vindo desta estrada
            SynchronizedQueue<Vehicle> vehicleQueue = new SynchronizedQueue<>();

            // Registar no mapa para o TrafficSorter usar
            trafficQueues.put(road, vehicleQueue);

            // Semáforo vai gerir ESTA fila
            TrafficLight trafficLight = new TrafficLight(vehicleQueue, road, clock);
            trafficLight.start();
        }

        // Recebe veículos vindos doutros nós
        new Receiver(vehiclesToSort, crossroad.getPort(), crossroad, clock).start();

        // Decide a que fila (estrada) pertence cada veículo
        new TrafficSorter(trafficQueues, vehiclesToSort, crossroad).start();
    }
}
