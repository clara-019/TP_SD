package Utils;

/**
 * Simple logical clock to order events by logical timestamp.
 * Implements atomic operations to advance and synchronize the clock.
 */
public class LogicalClock {
    private long time = 0;

    /**
     * Increments the clock and returns the new value.
     *
     * @return new clock value after increment
     */
    public synchronized long tick() {
        return ++time;
    }

    /**
     * Updates the clock with an external value ensuring causal order
     * (Lamport clocks): time = max(time, received) + 1.
     *
     * @param received timestamp received from another process
     * @return new clock value after update
     */
    public synchronized long update(long received) {
        time = Math.max(time, received) + 1;
        return time;
    }

    /**
     * Gets the current logical clock value.
     *
     * @return clock value
     */
    public synchronized long get() {
        return time;
    }
}
