package Vehicle;

public enum VehicleTypes {
    CAR(1.0),
    TRUCK(2.0), // truck = 2x the car
    MOTORCYCLE(0.5); // motorcycle = half

    private final double multiplier;

    VehicleTypes(double multiplier) {
        this.multiplier = multiplier;
    }

    public double getMultiplier() {
        return multiplier;
    }

    /**
     * @param baseTimeMs base time for the road from RoadEnum (in ms)
     * @return final time (in ms) adjusted by the vehicle type
     */
    public long getTimeToPass(long baseTimeMs) {
        return (long) (baseTimeMs * multiplier);
    }

    public String getTypeToString() {
        switch (this) {
            case CAR:
                return "Car";
            case TRUCK:
                return "Truck";
            case MOTORCYCLE:
                return "Motorcycle";
        }
        return null;
    }

    public static VehicleTypes getVehicleTypeFromString(String typeStr) {
        for (VehicleTypes type : values()) {
            if (type.getTypeToString().equalsIgnoreCase(typeStr)) {
                return type;
            }
        }
        return null;
    }
}
