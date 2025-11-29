package Vehicle;

public enum VehicleTypes {
    CAR(1.0),
    TRUCK(2.0), // camião = 2x o carro
    MOTORCYCLE(0.5); // moto = metade

    private final double multiplier;

    VehicleTypes(double multiplier) {
        this.multiplier = multiplier;
    }

    public double getMultiplier() {
        return multiplier;
    }

    /**
     * @param baseTimeMs tempo base da rua vindo do RoadEnum (em ms)
     * @return tempo final (em ms) ajustado ao tipo do veículo
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
