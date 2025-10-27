package Classes;

import java.util.LinkedList;
import java.util.Queue;

public class SynchronizedQueue<E> {
    private Queue<E> list = new LinkedList<E>();

    public synchronized void add(E element) {
        list.add(element);
    }

    public synchronized boolean isEmpty() {
        return list.isEmpty();
    }

    public synchronized E remove() {
        return list.poll();
    }

    public synchronized E peek(){
        return list.peek();
    }
}
