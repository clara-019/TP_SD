package Classes;

import java.util.LinkedList;
import java.util.Queue;

import Enums.RoadEnum;

public class SynchronizedQueue<E> {
    private Queue<E> queue = new LinkedList<E>();
    private RoadEnum road;
    
    public SynchronizedQueue(RoadEnum road) {
        this.road = road;
    }

    public SynchronizedQueue() {
        this.road = null;
    }

    public RoadEnum getRoad() {
        return road;
    }

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
