package Utils;

public class LogicalClock {
    private long time = 0;

    public synchronized long tick() {
        return ++time;
    }

    public synchronized long update(long received) {
        time = Math.max(time, received) + 1;
        return time;
    }

    public synchronized long get() {
        return time;
    }
}
