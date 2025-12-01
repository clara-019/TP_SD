package Event;

import Traffic.RoadEnum;

/**
 * Represents an event where a traffic signal changes state for a particular
 * road.
 * <p>
 * This event carries the target {@link Traffic.RoadEnum road}, the new signal
 * color (for example "GREEN" or "RED") and the logical time when the change
 * occurred. It extends {@link Event} and uses the
 * {@link EventType#TRAFFIC_LIGHT_CHANGE} event type.
 */
public class SignalChangeEvent extends Event {
    private static final long serialVersionUID = 1L;
    private final String signalColor;
    private final RoadEnum road;

    /**
     * Constructs a new SignalChangeEvent.
     *
     * @param road        the {@link Traffic.RoadEnum} identifying which road's
     *                    signal changed
     * @param time        the logical time (clock tick) when the change occurs
     * @param signalColor a human-readable representation of the new signal
     *                    color (for example "GREEN" or "RED")
     */
    public SignalChangeEvent(RoadEnum road, long time, String signalColor) {
        super(EventType.TRAFFIC_LIGHT_CHANGE, road.getDestination(), time);
        this.signalColor = signalColor;
        this.road = road;
    }

    /**
     * Returns the new signal color associated with this event.
     *
     * @return the new signal color (e.g. "GREEN" or "RED")
     */
    public String getSignalColor() {
        return signalColor;
    }

    /**
     * Returns the road affected by this signal change.
     *
     * @return the {@link Traffic.RoadEnum} for the affected road
     */
    public RoadEnum getRoad() {
        return road;
    }

    @Override
    public String toString() {
        return String.format("SignalChangeEvent[%s, signalColor=%s, road=%s]", super.toString(), signalColor, road);
    }

}
