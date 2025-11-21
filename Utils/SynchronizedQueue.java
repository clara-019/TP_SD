package Utils;

import java.util.*;

public class SynchronizedQueue<E> {
    private Queue<E> queue = new LinkedList<E>();

    public synchronized void add(E element) {
        queue.add(element);
        notifyAll();
    }

    public synchronized boolean isEmpty() {
        return queue.isEmpty();
    }

    public synchronized E remove() {
        while (queue.isEmpty()) {
            try {
                wait();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return null;
            }
        }
        return queue.poll();
    }

    public synchronized E peek(){
        return queue.peek();
    }

    public synchronized int size() { return queue.size(); }
}
