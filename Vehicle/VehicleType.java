package Vehicle;

/**
 * Vehicle types supported by the simulator.
 * <p>
 * Each type contains a multiplier to adjust the base road traversal time.
 */
public enum VehicleType {
    CAR(1.0),
    TRUCK(2.0),
    MOTORCYCLE(0.5);

    private final double multiplier;

    /**
     * Create a vehicle type with the given time multiplier.
     *
     * @param multiplier multiplier applied to base road time
     */
    VehicleType(double multiplier) {
        this.multiplier = multiplier;
    }

    /**
     * Returns the multiplier applied to the road base time.
     *
     * @return multiplier (Ex. 1.0 for car, 2.0 for truck)
     */
    public double getMultiplier() {
        return multiplier;
    }

    /**
     * Calculates the time needed for this vehicle to pass a road given the
     * base road time.
     *
     * @param baseTimeMs base road time from {@code RoadEnum} (ms)
     * @return adjusted time for the vehicle type (ms)
     */
    public long getTimeToPass(long baseTimeMs) {
        return (long) (baseTimeMs * multiplier);
    }

    /**
     * String representation of the vehicle type.
     *
     * @return type name in English (used in UI/serialization)
     */
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
}
