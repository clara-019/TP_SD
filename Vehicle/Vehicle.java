package Vehicle;

import java.io.Serializable;

import Enums.*;
import Road.RoadEnum;

public class Vehicle implements Serializable {
    private static final long serialVersionUID = 1L;
    private String id;
    private VehicleTypes type;
    private int entranceTime;
    private int exitTime;
    private PathEnum path;
    private RoadEnum originRoad;

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

    public RoadEnum getOriginRoad() {
        return originRoad;
    }

    public void setOriginRoad(RoadEnum originRoad) {
        this.originRoad = originRoad;
    }

}