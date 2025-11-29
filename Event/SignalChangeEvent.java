package Event;

import Node.NodeEnum;
import Traffic.RoadEnum;

public class SignalChangeEvent extends Event {
    private static final long serialVersionUID = 1L;
    private final String signalColor;
    private final RoadEnum road;

    public SignalChangeEvent(NodeEnum node, long time, String signalColor, RoadEnum road) {
        super(EventType.TRAFFIC_LIGHT_CHANGE, node, time);
        this.signalColor = signalColor;
        this.road = road;
    }

    public String getSignalColor() {
        return signalColor;
    }

    public RoadEnum getRoad() {
        return road;
    }

    @Override
    public String toString() {
        return String.format("SignalChangeEvent[%s, signalColor=%s, road=%s]", super.toString(), signalColor, road);
    }

}
