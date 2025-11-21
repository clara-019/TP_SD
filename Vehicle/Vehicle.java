package Vehicle;

import java.io.Serializable;

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

}