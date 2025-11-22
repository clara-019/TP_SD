package Utils;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import Node.NodeEnum;
import Node.RoadEnum;

public class RoundRobin {
    private final int totalThreads;
    private int turno = 0;

    private final Lock lock = new ReentrantLock();
    private final Condition cond = lock.newCondition();

    public RoundRobin(NodeEnum node) {
        this.totalThreads = RoadEnum.getRoadsToCrossroad(node).size();
    }

    public void esperarTurno(RoadEnum road) throws InterruptedException {
        lock.lock();
        try {
            while (turno != RoadEnum.getRoadsToCrossroad(road.getDestination()).indexOf(road)) {
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
