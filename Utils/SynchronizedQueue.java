package Utils;

import java.util.LinkedList;
import java.util.Queue;

public class SynchronizedQueue<E> {
    private Queue<E> queue = new LinkedList<E>();

    public synchronized void add(E element) {
        queue.add(element);
    }

    public synchronized boolean isEmpty() {
        return queue.isEmpty();
    }

    public synchronized E remove() {
        return queue.poll();
    }

    public synchronized E peek(){
        return queue.peek();
    }
}
