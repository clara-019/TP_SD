package Event;

import java.util.PriorityQueue;
import java.util.Comparator;


public class EventHandler {
    private final PriorityQueue<Event> eventQueue;
    private long currentTime;

    public EventHandler() {
        this.eventQueue = new PriorityQueue<>(Comparator.comparingLong(Event::getTime));
        this.currentTime = 0;
    }

    public void addEvent(Event event) {
        if (event == null) return;
        eventQueue.add(event);
        System.out.println("Evento adicionado: " + event);
    }

    public void processNextEvent() {
        Event next = eventQueue.poll();
        if (next == null) {
            System.out.println("Nenhum evento para processar!");
            return;
        }
        this.currentTime = next.getTime();
        System.out.println("Processando evento: " + next + " (tempo=" + this.currentTime + ")");
    }

    public boolean hasEvents() {
        return !eventQueue.isEmpty();
    }

    public int getEventCount() {
        return eventQueue.size();
    }

    public long getCurrentTime() {
        return currentTime;
    }
}