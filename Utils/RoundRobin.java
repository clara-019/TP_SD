package Utils;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class RoundRobin {
    private final int totalThreads;
    private int turno = 0;

    private final Lock lock = new ReentrantLock();
    private final Condition cond = lock.newCondition();

    public RoundRobin(int totalThreads) {
        this.totalThreads = totalThreads;
    }

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
