package Classes;

import java.util.List;
import Enums.*;

public class Vehicle {
    private String id;
    private VehicleTypes type;
    private int entranceTime;
    private int exitTime;
    private CrossroadEnum nextCrossroad;
    private PathEnum path;

    public Vehicle(String id, VehicleTypes type, PathEnum path) {
        this.id = id;
        this.type = type;
        this.path = path;
        this.nextCrossroad = path.getPath().get(0);
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

    public CrossroadEnum getNextCrossroad() {
        return nextCrossroad;
    }

    public PathEnum getPath() {
        return path;
    }

    public void setNextCrossroad(){
        int currentIndex = path.getPath().indexOf(nextCrossroad);
        if (currentIndex + 1 < path.getPath().size()) {
            this.nextCrossroad = path.getPath().get(currentIndex + 1);
        } else {
            this.nextCrossroad = null;
        }
    }


}