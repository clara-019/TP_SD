package Enums;

public enum RoadsEnum {
    /*E1_CR1, E2_CR2,*/ E3_CR3, /*CR1_CR2, CR1_CR4, CR2_CR1, CR2_CR5, CR2_CR3, CR3_CR2,*/ CR3_S /* , CR4_CR5, CR5_S*/;

    public String getRoadToString() {
        switch (this) {
            /*
             * case E1_CR1:
             * return "E1-CR1";
             * case E2_CR2:
             * return "E2-CR2";
             */
            case E3_CR3:
                return "E3-CR3";
            /*
             * case CR1_CR2:
             * return "CR1-CR2";
             * case CR1_CR4:
             * return "CR1-CR4";
             * case CR2_CR1:
             * return "CR2-CR1";
             * case CR2_CR5:
             * return "CR2-CR5";
             * case CR2_CR3:
             * return "CR2-CR3";
             * case CR3_CR2:
             * return "CR3-CR2";
             */
            case CR3_S:
                return "CR3-S";
            /*
             * case CR4_CR5:
             * return "CR4-CR5";
             * case CR5_S:
             * return "CR5-S";
             */
            default:
                return "Unknown Road";
        }
    }

    public int getPort() {
        switch (this) {
            /*
             * case E1_CR1:
             * return 7001;
             * case E2_CR2:
             * return 7002;
             */
            case E3_CR3:
                return 7003;
            /*
             * case CR1_CR2:
             * return 8001;
             * case CR1_CR4:
             * return 8002;
             * case CR2_CR1:
             * return 8003;
             * case CR2_CR5:
             * return 8004;
             * case CR2_CR3:
             * return 8005;
             * case CR3_CR2:
             * return 8006;
             */
            case CR3_S:
                return 8007;
            /*
             * case CR4_CR5:
             * return 8008;
             * case CR5_S:
             * return 8009;
             */
            default:
                return -1;
        }
    }

    public CrossroadsEnum getOrigin(){
        switch (this) {
            /*
             * case E1_CR1:
             * return Crossroads.E1;
             * case E2_CR2:
             * return Crossroads.E2;
             */
            case E3_CR3:
                return CrossroadsEnum.E3;
            /*
             * case CR1_CR2:
             * case CR1_CR4:
             * return Crossroads.Cr1;
             * case CR2_CR1:
             * case CR2_CR5:
             * case CR2_CR3:
             * return Crossroads.Cr2;
             * case CR3_CR2:
             */
            case CR3_S:
                return CrossroadsEnum.Cr3;
            /*
             * case CR4_CR5:
             * return Crossroads.Cr4;
             * case CR5_S:
             * return Crossroads.Cr5;
             */
            default:
                return null;
        }
    }

    public CrossroadsEnum getDestination(){
        switch (this) {
            /*
             * case E1_CR1:
             * return Crossroads.Cr1;
             * case E2_CR2:
             * return Crossroads.Cr2;
             */
            case E3_CR3:
                return CrossroadsEnum.Cr3;
            /*
             * case CR1_CR2:
             * return Crossroads.Cr2;
             * case CR1_CR4:
             * return Crossroads.Cr4;
             * case CR2_CR1:
             * return Crossroads.Cr1;
             * case CR2_CR5:
             * return Crossroads.Cr5;
             * case CR2_CR3:
             * return Crossroads.Cr3;
             * case CR3_CR2:
             */
            case CR3_S:
                return CrossroadsEnum.S;
            /*
             * case CR4_CR5:
             * return Crossroads.Cr5;
             * case CR5_S:
             * return Crossroads.S;
             */
            default:
                return null;
        }
    }

}
