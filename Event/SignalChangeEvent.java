package Event;

import Node.NodeEnum;

public class SignalChangeEvent extends Event {
    private static final long serialVersionUID = 1L;
    private final String signalColor;

    public SignalChangeEvent(NodeEnum node, long time, String signalColor) {
        super(EventType.TRAFFIC_LIGHT_CHANGE, node, time);
        this.signalColor = signalColor;
    }

    public String getSignalColor() {
        return signalColor;
    }

    @Override
    public String toString() {
        return String.format("SignalChangeEvent[%s, signalColor=%s]", super.toString(), signalColor);
    }
    
}
