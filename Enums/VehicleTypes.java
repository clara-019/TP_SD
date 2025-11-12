package Enums;

public enum VehicleTypes {
    CAR,
    TRUCK,
    MOTORCYCLE;

    private static final double CAR_MULTIPLIER = 1.0;
    private static final double TRUCK_MULTIPLIER = 4.0;
    private static final double MOTORCYCLE_MULTIPLIER = 0.5;

    public String getTypeToString() {
        switch (this) {
            case CAR:
                return "Car";
            case TRUCK:
                return "Truck";
            case MOTORCYCLE:
                return "Motorcycle";
            default:
                return "Unknown Vehicle Type";
        }
    }

    public static VehicleTypes getVehicleTypeFromString(String typeStr) {
        for (VehicleTypes type : VehicleTypes.values()) {
            if (type.getTypeToString().equalsIgnoreCase(typeStr)) {
                return type;
            }
        }
        return null; // or throw an exception if preferred
    }
    public long getTimeToPass(int temp) {
        switch (this) {
            case CAR:
                return (long) (CAR_MULTIPLIER * temp);
            case TRUCK:
                return (long) (TRUCK_MULTIPLIER * MOTORCYCLE_MULTIPLIER * CAR_MULTIPLIER * temp);
            case MOTORCYCLE:
                return (long) (MOTORCYCLE_MULTIPLIER * CAR_MULTIPLIER * temp);
            default:
                return -1;
        }
    }
}
