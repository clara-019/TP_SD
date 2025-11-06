package Classes;

import Enums.CrossroadEnum;
import Enums.VehicleTypes;
import Enums.PathEnum;

public class Simulator {
    private EventHandler eventHandler;  // O gestor de eventos
    private boolean running;           // Se a simulação está a correr
    private Crossroad crossroad;       // O cruzamento principal

    // Construtor
    public Simulator(Crossroad crossroad) {
        this.crossroad = crossroad;
        this.eventHandler = new EventHandler(crossroad);
        this.running = false;

        // Configura eventos iniciais
        setupInitialEvents();
    }

    // Configura alguns eventos para começar a simulação
    private void setupInitialEvents() {
        // Cria alguns veículos de exemplo
        Vehicle car1 = new Vehicle("CAR-001", VehicleTypes.CAR, PathEnum.E3_1);
        Vehicle truck1 = new Vehicle("TRUCK-001", VehicleTypes.TRUCK, PathEnum.E3_1);

        // Agenda chegada inicial dos veículos
        eventHandler.addEvent(new Event(car1, CrossroadEnum.A, 0, "CHEGADA"));
        eventHandler.addEvent(new Event(truck1, CrossroadEnum.B, 500, "CHEGADA"));

        // Agenda primeiro ciclo de semáforo
        eventHandler.addEvent(new Event(null, CrossroadEnum.A, 0, "SEMAFORO_VERDE"));
    }

    // Inicia a simulação
    public void startSimulation() {
        if (running) {
            System.out.println("Simulação já está a correr!");
            return;
        }

        running = true;
        System.out.println("SIMULAÇÃO INICIADA!");
        System.out.println("======================");

        // Processa eventos até não haver mais nenhum ou parar manualmente
        while (running && eventHandler.hasEvents()) {
            eventHandler.processNextEvent();

            // Pequena pausa para vermos o que acontece
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        // Se chegou aqui, acabaram os eventos
        running = false;
        System.out.println("======================");
        System.out.println("SIMULAÇÃO TERMINADA!");
        showStatistics();
    }

    // Para a simulação
    public void stopSimulation() {
        running = false;
        System.out.println("Simulação parada pelo utilizador");
    }

    // Adiciona um evento à simulação
    public void addEvent(Event event) {
        eventHandler.addEvent(event);
    }

    // Mostra estatísticas
    public void showStatistics() {
        System.out.println("ESTATÍSTICAS FINAIS:");
        System.out.println("Tempo total: " + eventHandler.getCurrentTime() + " unidades");
        System.out.println("Eventos processados: " + eventHandler.getEventCount());
    }

    // Mostra estado atual
    public void showStatus() {
        System.out.println("Estado: " + (running ? "A CORRER" : "PARADA"));
        System.out.println("Eventos pendentes: " + eventHandler.getEventCount());
        System.out.println("Tempo atual: " + eventHandler.getCurrentTime());
    }
}