package Node;

import java.util.List;
import java.util.Map;

import Road.RoadEnum;
import Utils.SynchronizedQueue;
import Vehicle.Vehicle;

public class ExitSorter extends Thread {
    private Map<RoadEnum, SynchronizedQueue<Vehicle>> trafficQueues;
    private SynchronizedQueue<Vehicle> vehiclesToSort;
    private NodeEnum node;

    public ExitSorter(Map<RoadEnum, SynchronizedQueue<Vehicle>> trafficQueues, SynchronizedQueue<Vehicle> vehiclesToSort, NodeEnum node) {
        this.trafficQueues = trafficQueues;
        this.vehiclesToSort = vehiclesToSort;
        this.node = node;
    }

    @Override
    public void run() {
        while (true) {
            Vehicle vehicle = vehiclesToSort.remove();
            if (vehicle == null) continue;
            System.out
                    .println("[ExitSorter " + node.toString() + "] Sorting vehicle " + vehicle.getId());
            List<NodeEnum> path = vehicle.getPath().getPath();
            NodeEnum nextNode = path.get(path.indexOf(node) + 1);
            RoadEnum road = RoadEnum.toRoadEnum(node.toString() + "_" + nextNode.toString());
            trafficQueues.get(road).add(vehicle);
        }

    }
}
