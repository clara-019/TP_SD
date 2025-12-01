package Utils;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class RoundRobin {
    private final int totalThreads;
    private int turno = 0;

    private final Lock lock = new ReentrantLock();
    private final Condition cond = lock.newCondition();

    /**
     * Simple round-robin to coordinate turns between threads identified by id.
     *
     * @param totalThreads total number of threads in the round-robin
     */
    public RoundRobin(int totalThreads) {
        this.totalThreads = totalThreads;
    }

    /**
     * Blocks until it is the turn of the thread with identifier `id`.
     *
     * @param id thread identifier (0..totalThreads-1)
     * @throws InterruptedException if waiting is interrupted
     */
    public void esperarTurno(int id) throws InterruptedException {
        lock.lock();
        try {
            while (turno != id) {
                cond.await();
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * Ends the current turn and advances to the next one, signaling all
     * waiting threads.
     */
    public void terminarTurno() {
        lock.lock();
        try {
            turno = (turno + 1) % totalThreads;
            cond.signalAll();
        } finally {
            lock.unlock();
        }
    }
}
