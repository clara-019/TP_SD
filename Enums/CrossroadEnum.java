package Enums;

public enum CrossroadEnum {
    /* E1, E2, */ E3, /* Cr1, Cr2, */ Cr3, /* Cr4, Cr5, */ S;

    public String toString() {
        switch (this) {
            /*
             * case E1:
             * return "E1";
             * case E2:
             * return "E2";
             */
            case E3:
                return "E3";
            /*
             * case Cr1:
             * return "Cr1";
             * case Cr2:
             * return "Cr2";
             */
            case Cr3:
                return "Cr3";
            /*
             * case Cr4:
             * return "Cr4";
             * case Cr5:
             * return "Cr5";
             */
            case S:
                return "S";
            default:
                return "Unknown Crossroad";
        }
    }

    public static CrossroadEnum toCrossroadEnum(String crossString) {
        for (CrossroadEnum crossroad : values()) {
            if (crossroad.toString().equals(crossString)) {
                return crossroad;
            }
        }
        return null;
    }

    public int getPort() {
        switch (this) {
            /*
             * case E1:
             * return 5001;
             * case E2:
             * return 5002;
             */
            case E3:
                return 5003;
            /*
             * case Cr1:
             * return 6001;
             * case Cr2:
             * return 6002;
             */
            case Cr3:
                return 6003;
            /*
             * case Cr4:
             * return 6004;
             * case Cr5:
             * return 6005;
             */
            case S:
                return 7000;
            default:
                return -1;
        }
    }

    
}
