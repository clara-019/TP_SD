package Traffic;

import Utils.SynchronizedQueue;
import Vehicle.*;

import java.util.*;

import Node.NodeEnum;

/**
 * Distributes arriving vehicles into per-road queues for a given node.
 *
 * <p>
 * The TrafficSorter consumes vehicles from a shared arrival queue
 * (`vehiclesToSort`) and, using each vehicle's path, determines which
 * incoming road the vehicle arrived on. It then forwards the vehicle to
 * the appropriate {@link Utils.SynchronizedQueue} associated with that
 * {@link RoadEnum} so that local pass-through handlers or traffic lights
 * can process them.
 */
public class TrafficSorter extends Thread {
    private Map<RoadEnum, SynchronizedQueue<Vehicle>> trafficQueues;
    private SynchronizedQueue<Vehicle> vehiclesToSort;
    private NodeEnum node;

    /**
     * Create a TrafficSorter.
     *
     * @param trafficQueues  map from {@link RoadEnum} to destination queues
     * @param vehiclesToSort shared queue with newly arrived vehicles
     * @param node           the node where sorting occurs
     */
    public TrafficSorter(Map<RoadEnum, SynchronizedQueue<Vehicle>> trafficQueues,
            SynchronizedQueue<Vehicle> vehiclesToSort, NodeEnum node) {
        this.trafficQueues = trafficQueues;
        this.vehiclesToSort = vehiclesToSort;
        this.node = node;
    }

    /**
     * Main loop: remove vehicles from the arrival queue, compute the
     * incoming road based on the vehicle's path, and add the vehicle to
     * the corresponding per-road queue.
     */
    @Override
    public void run() {
        while (true) {
            Vehicle vehicle = this.vehiclesToSort.remove();
            System.out.println("[TrafficSorter " + this.node.toString() + "] Sorting vehicle " + vehicle.getId());
            NodeEnum previousNode = vehicle.findPreviousNode(this.node);
            RoadEnum road = RoadEnum.toRoadEnum(previousNode.toString() + "_" + this.node.toString());
            this.trafficQueues.get(road).add(vehicle);
        }

    }
}
