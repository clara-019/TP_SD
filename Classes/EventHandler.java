package Classes;

import java.util.PriorityQueue;
import java.util.Comparator;

public class EventHandler {
    private PriorityQueue<Event> eventQueue;  // Fila de eventos por ordem de tempo
    private long currentTime;                 // Tempo atual da simulação
    private Crossroad crossroad;              // Referência ao cruzamento principal

    // Construtor
    public EventHandler(Crossroad crossroad) {
        // Cria uma fila que ordena eventos pelo tempo (menor tempo primeiro)
        this.eventQueue = new PriorityQueue<>(Comparator.comparingLong(Event::getTime));
        this.currentTime = 0;
        this.crossroad = crossroad;
    }

    // Adiciona um evento à fila
    public void addEvent(Event event) {
        eventQueue.add(event);
        System.out.println("Evento adicionado: " + event);
    }

    // Processa o próximo evento da fila
    public void processNextEvent() {
        if (eventQueue.isEmpty()) {
            System.out.println("Nenhum evento para processar!");
            return;
        }

        // Pega o evento mais antigo (menor tempo)
        Event nextEvent = eventQueue.poll();
        this.currentTime = nextEvent.getTime();

        System.out.println("Processando: " + nextEvent);

        // Executa a ação conforme o tipo de evento
        executeEvent(nextEvent);
    }

    // Executa a ação do evento
    private void executeEvent(Event event) {
        switch (event.getType()) {
            case "CHEGADA":
                handleVehicleArrival(event);
                break;
            case "PARTIDA":
                handleVehicleDeparture(event);
                break;
            case "SEMAFORO_VERDE":
                handleGreenLight(event);
                break;
            default:
                System.out.println("Tipo de evento desconhecido: " + event.getType());
        }
    }

    // Quando um veículo chega a um cruzamento
    private void handleVehicleArrival(Event event) {
        Vehicle vehicle = event.getVehicle();
        CrossroadEnum location = event.getLocation();

        System.out.println("Veículo " + vehicle.getId() + " CHEGOU em " + location);

        // Aqui poderia adicionar o veículo a uma fila do semáforo
        // Por exemplo: adicionar à SynchronizedQueue do cruzamento
    }

    // Quando um veículo parte de um cruzamento
    private void handleVehicleDeparture(Event event) {
        Vehicle vehicle = event.getVehicle();
        CrossroadEnum location = event.getLocation();

        System.out.println("Veículo " + vehicle.getId() + " PARTIU de " + location);

        // Atualiza o próximo cruzamento do veículo
        vehicle.setNextCrossroad();

        // Se há próximo cruzamento, agenda chegada lá
        if (vehicle.getNextCrossroad() != null) {
            Event nextArrival = new Event(vehicle, vehicle.getNextCrossroad(),
                    currentTime + 1000, "CHEGADA");
            addEvent(nextArrival);
        } else {
            System.out.println("Veículo " + vehicle.getId() + " chegou ao destino final!");
        }
    }

    // Quando um semáforo fica verde
    private void handleGreenLight(Event event) {
        CrossroadEnum location = event.getLocation();
        System.out.println("Semáforo VERDE em " + location);

        // Agenda próximo ciclo do semáforo
        Event nextLightChange = new Event(null, location, currentTime + 12000, "SEMAFORO_VERDE");
        addEvent(nextLightChange);
    }

    // Verifica se ainda há eventos
    public boolean hasEvents() {
        return !eventQueue.isEmpty();
    }

    // Mostra quantos eventos faltam
    public int getEventCount() {
        return eventQueue.size();
    }

    // Getter para o tempo atual
    public long getCurrentTime() {
        return currentTime;
    }
}