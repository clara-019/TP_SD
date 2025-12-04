package Launcher;

/**
 * Simple thread-safe statistics collector for queue sizes.
 * <p>
 * This class records integer samples (queue sizes) and provides
 * accessors for the maximum observed value, the running average and the
 * number of recorded samples. All methods are synchronized so the
 * collector can be safely used from multiple threads.
 */
public class QueueStats {
    private int max = 0;
    private long sum = 0L;
    private long samples = 0L;

    /**
     * Record a new queue size sample.
     * <p>
     * This updates the running sum, sample count and the maximum value
     * observed. Method is synchronized to be safe for concurrent callers.
     *
     * @param size the sampled queue size (non-negative)
     */
    public synchronized void recordSample(int size) {
        this.sum += size;
        this.samples += 1;
        if (size > this.max)
            this.max = size;
    }

    /**
     * Return the maximum observed queue size.
     *
     * @return max recorded sample (0 if no samples yet)
     */
    public synchronized int getMax() {
        return max;
    }

    /**
     * Return the average of recorded samples.
     *
     * @return the arithmetic mean of samples, or 0.0 if no samples were recorded
     */
    public synchronized double getAverage() {
        return samples == 0 ? 0.0 : ((double) sum) / samples;
    }

    /**
     * Return the number of samples that have been recorded.
     *
     * @return sample count
     */
    public synchronized long getSamples() {
        return samples;
    }
}
