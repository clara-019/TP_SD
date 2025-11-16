package Enums;

import java.util.*;
import Crossroad.*;

public enum PathEnum {
    E3_1;

    public List<CrossroadEnum> getPath() {
        switch (this) {
            case E3_1:
                return new ArrayList<CrossroadEnum>(Arrays.asList(
                        CrossroadEnum.E3,
                        CrossroadEnum.Cr3,
                        CrossroadEnum.S
                ));
            default:
                return List.of();
        }
    }
}
