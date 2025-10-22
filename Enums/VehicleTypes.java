package Enums;

public enum VehicleTypes {
    CAR,
    TRUCK,
    MOTORCYCLE;

    private static final double CAR_MULTIPLIER = 1.0;
    private static final double TRUCK_MULTIPLIER = 4.0;
    private static final double MOTORCYCLE_MULTIPLIER = 0.5;

    public String getType() {
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

    public double getTimeToPass(int temp) {
        switch (this) {
            case CAR:
                return CAR_MULTIPLIER * temp;
            case TRUCK:
                return TRUCK_MULTIPLIER * MOTORCYCLE_MULTIPLIER * CAR_MULTIPLIER * temp;
            case MOTORCYCLE:
                return MOTORCYCLE_MULTIPLIER * CAR_MULTIPLIER * temp;
            default:
                return -1;
        }
    }
}
