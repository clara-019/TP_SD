package Vehicle;

import java.io.Serializable;

import Node.NodeEnum;

public class Vehicle implements Serializable {
    private static final long serialVersionUID = 1L;
    private String id;
    private VehicleTypes type;
    private long entranceTime;
    private long exitTime;
    private PathEnum path;

    public Vehicle(String id, VehicleTypes type, PathEnum path) {
        this.id = id;
        this.type = type;
        this.path = path;
    }

    public String getId() {
        return id;
    }

    public VehicleTypes getType() {
        return type;
    }

    public long getEntranceTime() {
        return entranceTime;
    }

    public void setEntranceTime(long entranceTime) {
        this.entranceTime = entranceTime;
    }

    public long getExitTime() {
        return exitTime;
    }

    public void setExitTime(long exitTime) {
        this.exitTime = exitTime;
    }

    public PathEnum getPath() {
        return path;
    }

    public NodeEnum findNextNode(NodeEnum current) {
        java.util.List<NodeEnum> list = this.path.getPath();
        for (int i = 0; i < list.size() - 1; i++)
            if (list.get(i) == current)
                return list.get(i + 1);

        return null;
    }

    public NodeEnum findPreviousNode(NodeEnum current) {
        java.util.List<NodeEnum> list = this.path.getPath();
        for (int i = 1; i < list.size(); i++)
            if (list.get(i) == current)
                return list.get(i - 1);

        return null;
    }
}