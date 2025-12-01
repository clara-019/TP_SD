package Utils;

import java.util.*;

/**
 * Simple synchronized queue for communication between threads.
 * Implements essential blocking and non-blocking operations.
 *
 * @param <E> type of elements in the queue
 */
public class SynchronizedQueue<E> {
    private Queue<E> queue = new LinkedList<E>();

    /**
     * Adds an element to the queue and notifies waiting threads.
     *
     * @param element element to add
     */
    public synchronized void add(E element) {
        queue.add(element);
        notifyAll();
    }

    /**
     * Removes and returns the first element of the queue. If the queue is
     * empty, waits until an element becomes available.
     *
     * @return first element of the queue
     */
    public synchronized E remove() {
        while (queue.isEmpty()) {
            try {
                wait();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        return queue.poll();
    }

    /**
     * Removes and returns the first element of the queue without blocking
     * (may return {@code null}).
     *
     * @return first element or {@code null} if the queue is empty
     */
    public synchronized E poll() {
        return queue.poll();
    }

    /**
     * Returns the first element without removing it.
     *
     * @return first element or {@code null}
     */
    public synchronized E peek() {
        return queue.peek();
    }

    /**
     * Returns the last element of the queue.
     *
     * @return last element or {@code null}
     */
    public synchronized E peekLast() {
        return ((LinkedList<E>) queue).peekLast();
    }
}
