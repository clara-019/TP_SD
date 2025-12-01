package Vehicle;

import java.io.Serializable;

import Node.NodeEnum;

/**
 * Represents a vehicle in the simulator.
 * <p>
 * Each vehicle has an identifier, a type (car, truck, motorcycle), a path
 * (sequence of nodes), and entry/exit timestamps within the system.
 */
public class Vehicle implements Serializable {
    private static final long serialVersionUID = 1L;
    private String id;
    private VehicleTypes type;
    private long entranceTime;
    private long exitTime;
    private PathEnum path;

    /**
     * Vehicle constructor.
     *
     * @param id   unique vehicle identifier
     * @param type vehicle type (affects traversal times)
     * @param path path the vehicle will follow
     */
    public Vehicle(String id, VehicleTypes type, PathEnum path) {
        this.id = id;
        this.type = type;
        this.path = path;
    }

    /**
     * Returns the vehicle identifier.
     *
     * @return vehicle id
     */
    public String getId() {
        return id;
    }

    /**
     * Returns the vehicle type.
     *
     * @return enum representing the vehicle type
     */
    public VehicleTypes getType() {
        return type;
    }

    /**
     * Returns the vehicle entry time in the system (ms).
     *
     * @return entry time in milliseconds
     */
    public long getEntranceTime() {
        return entranceTime;
    }

    /**
     * Sets the vehicle entry time in the system.
     *
     * @param entranceTime time in milliseconds
     */
    public void setEntranceTime(long entranceTime) {
        this.entranceTime = entranceTime;
    }

    /**
     * Returns the vehicle exit time from the system (ms).
     *
     * @return exit time in milliseconds
     */
    public long getExitTime() {
        return exitTime;
    }

    /**
     * Sets the vehicle exit time in the system.
     *
     * @param exitTime time in milliseconds
     */
    public void setExitTime(long exitTime) {
        this.exitTime = exitTime;
    }

    /**
     * Returns the vehicle path (sequence of nodes).
     *
     * @return enum representing the vehicle's path
     */
    public PathEnum getPath() {
        return path;
    }

    /**
     * Returns the next node in the path relative to the current node.
     *
     * @param current current node
     * @return next node if present, otherwise {@code null}
     */
    public NodeEnum findNextNode(NodeEnum current) {
        java.util.List<NodeEnum> list = this.path.getPath();
        for (int i = 0; i < list.size() - 1; i++)
            if (list.get(i) == current)
                return list.get(i + 1);

        return null;
    }

    /**
     * Returns the previous node in the path relative to the current node.
     *
     * @param current current node
     * @return previous node if present, otherwise {@code null}
     */
    public NodeEnum findPreviousNode(NodeEnum current) {
        java.util.List<NodeEnum> list = this.path.getPath();
        for (int i = 1; i < list.size(); i++)
            if (list.get(i) == current)
                return list.get(i - 1);

        return null;
    }
}