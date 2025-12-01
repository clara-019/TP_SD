package Traffic;

import Utils.SynchronizedQueue;
import Vehicle.*;

import java.util.*;

import Node.NodeEnum;

public class TrafficSorter extends Thread {
    private Map<RoadEnum, SynchronizedQueue<Vehicle>> trafficQueues;
    private SynchronizedQueue<Vehicle> vehiclesToSort;
    private NodeEnum node;

    public TrafficSorter(Map<RoadEnum, SynchronizedQueue<Vehicle>> trafficQueues,
            SynchronizedQueue<Vehicle> vehiclesToSort, NodeEnum node) {
        this.trafficQueues = trafficQueues;
        this.vehiclesToSort = vehiclesToSort;
        this.node = node;
    }

    @Override
    public void run() {
        // Main loop: receives vehicles that arrived at the node and forwards
        // them to the correct road queue based on the vehicle's path.
        while (true) {
            Vehicle vehicle = vehiclesToSort.remove();
            System.out.println("[TrafficSorter " + node.toString() + "] Sorting vehicle " + vehicle.getId());
            List<NodeEnum> path = vehicle.getPath().getPath();
            NodeEnum previousNode = path.get(path.indexOf(node) - 1);
            RoadEnum road = RoadEnum.toRoadEnum(previousNode.toString() + "_" + node.toString());
            trafficQueues.get(road).add(vehicle);
        }

    }
}
