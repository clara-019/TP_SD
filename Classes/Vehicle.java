package Classes;

import java.util.List;
import Enums.*;

public class Vehicle {
    private String id;
    private VehicleTypes type;
    private int entranceTime;
    private int exitTime;
    private Crossroad nextCrossroad;
    private List<Crossroad> path;

    public Vehicle(String id, VehicleTypes type, List<Crossroad> path) {
        this.id = id;
        this.type = type;
        this.path = path;
        this.nextCrossroad = path.get(0);
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

    public Crossroad getNextCrossroad() {
        return nextCrossroad;
    }

    public void setNextCrossroad(){
        int currentIndex = path.indexOf(nextCrossroad);
        if (currentIndex + 1 < path.size()) {
            this.nextCrossroad = path.get(currentIndex + 1);
        } else {
            this.nextCrossroad = null;
        }
    }


}