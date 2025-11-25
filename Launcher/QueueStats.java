package Launcher;

public class QueueStats {
    private int max = 0;
    private long sum = 0L;
    private long samples = 0L;

    public synchronized void recordSample(int size) {
        this.sum += size;
        this.samples += 1;
        if (size > this.max) this.max = size;
    }

    public synchronized int getMax() { return max; }
    public synchronized double getAverage() { return samples == 0 ? 0.0 : ((double) sum) / samples; }
    public synchronized long getSamples() { return samples; }
}
