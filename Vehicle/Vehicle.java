package Vehicle;

import java.io.Serializable;

public class Vehicle implements Serializable {
    private static final long serialVersionUID = 1L;
    private String id;
    private VehicleTypes type;
    private int entranceTime;
    private int exitTime;
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

    public int getEntranceTime() {
        return entranceTime;
    }

    public void setEntranceTime(int entranceTime) {
        this.entranceTime = entranceTime;
    }

    public int getExitTime() {
        return exitTime;
    }

    public void setExitTime(int exitTime) {
        this.exitTime = exitTime;
    }

    public PathEnum getPath() {
        return path;
    }

}