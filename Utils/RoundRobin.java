package Utils;

/**
 * A simple round-robin turn coordinator for cooperating threads.
 * <p>
 * Threads register themselves using a numeric identifier (0..N-1) and
 * call {@link #esperarTurno(int)} to block until their turn. When a
 * participant finishes its work it calls {@link #terminarTurno()} to
 * advance the turn and wake waiting threads.
 */
public class RoundRobin {
    private final int totalThreads;
    private int turno = 0;

    /**
     * Create a new RoundRobin coordinator.
     *
     * @param totalThreads total number of threads participating (must be > 0)
     */
    public RoundRobin(int totalThreads) {
        this.totalThreads = totalThreads;
    }

    /**
     * Blocks until it is the turn of the thread with identifier {@code id}.
     * 
     * @param id thread identifier (0..totalThreads-1)
     * @throws InterruptedException if waiting is interrupted
     */
    public synchronized void esperarTurno(int id) throws InterruptedException {
        while (turno != id) {
            wait();
        }
    }

    /**
     * Ends the current turn and advances to the next one, signaling all
     * waiting threads so the newly active participant can proceed.
     */
    public synchronized void terminarTurno() {
        turno = (turno + 1) % totalThreads;
        notifyAll();
    }
}
