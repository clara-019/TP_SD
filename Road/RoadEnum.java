package Road;

import java.util.*;
import Crossroad.*;

public enum RoadEnum {
    /* E1_CR1, E2_CR2, */ E3_CR3, /* CR1_CR2, CR1_CR4, CR2_CR1, CR2_CR5, CR2_CR3, CR3_CR2, */ CR3_S /*
                                                                                                     * , CR4_CR5, CR5_S
                                                                                                     */;

    public String toString() {
        switch (this) {
            /*
             * case E1_CR1:
             * return "E1-CR1";
             * case E2_CR2:
             * return "E2-CR2";
             */
            case E3_CR3:
                return "E3_Cr3";
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
                return "Cr3_S";
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

    public static RoadEnum toRoadEnum(String roadStr) {
        for (RoadEnum road : values()) {
            if (road.toString().equals(roadStr)) {
                return road;
            }
        }
        return null;
    }

    public int getPort() {
        switch (this) {
            /*
             * case E1_CR1:
             * return 5161;
             * case E2_CR2:
             * return 5262;
             */
            case E3_CR3:
                return 5363;
            /*
             * case CR1_CR2:
             * return 6162;
             * case CR1_CR4:
             * return 6164;
             * case CR2_CR1:
             * return 6261;
             * case CR2_CR5:
             * return 6265;
             * case CR2_CR3:
             * return 6263;
             * case CR3_CR2:
             * return 6362;
             */
            case CR3_S:
                return 6370;
            /*
             * case CR4_CR5:
             * return 6465;
             * case CR5_S:
             * return 6570;
             */
            default:
                return -1;
        }
    }


    public CrossroadEnum getOrigin() {
        return CrossroadEnum.toCrossroadEnum(this.toString().split("_")[0].trim());
    }

    public CrossroadEnum getDestination() {
        return CrossroadEnum.toCrossroadEnum(this.toString().split("_")[1].trim());
    }

    public static List<RoadEnum> getRoadsToCrossroad(CrossroadEnum crossroadEnum) {
        List<RoadEnum> roads = new ArrayList<>();

        for (RoadEnum road : values()) {
            if (road.getDestination() == crossroadEnum) {
                roads.add(road);
            }
        }

        return roads;
    }

}
