package Classes;

import Enums.*;
import java.io.File;

public class Simulator {
    private EventHandler eventHandler; // O gestor de eventos
    private boolean running; // Se a simulação está a correr
    // Construtor

    public Simulator() {
        this.running = false;

        // Initialize event handler (no specific Crossroad instance available here)
        this.eventHandler = new EventHandler(null);
    }

    // Configura alguns eventos para começar a simulação
    private void setupInitialEvents() {
        // Cria alguns veículos de exemplo
        Vehicle car1 = new Vehicle("CAR-001", VehicleTypes.CAR, PathEnum.E3_1);
        Vehicle truck1 = new Vehicle("TRUCK-001", VehicleTypes.TRUCK, PathEnum.E3_1);

        // Agenda chegada inicial dos veículos
        eventHandler.addEvent(new Event(car1, CrossroadEnum.Cr3, 0, "CHEGADA"));
        eventHandler.addEvent(new Event(truck1, CrossroadEnum.Cr3, 500, "CHEGADA"));

        // Agenda primeiro ciclo de semáforo
        // eventHandler.addEvent(new Event(null, CrossroadEnum.Cr3, 0,
        // "SEMAFORO_VERDE"));
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

        // Use the current JVM classpath so child processes can locate compiled classes
        String classpath = System.getProperty("java.class.path");
        File workDir = new File(System.getProperty("user.dir"));

        for (CrossroadEnum cr : CrossroadEnum.values()) {
            try {
                    String title = "Crossroad-" + cr.toString();
                    // pass an explicit empty quoted title (""") so `start` treats the next token as the command
                ProcessBuilder pb = new ProcessBuilder("cmd.exe", "/c", "start", "\"\"", "cmd.exe", "/k", "java", "-cp", classpath, "Classes.Crossroad", cr.toString());
                pb.directory(workDir);
                pb.start();
            } catch (Exception e) {
                e.printStackTrace();
            }
            System.out.println("Iniciando processo para o cruzamento: " + cr.toString());
        }

        for (RoadEnum road : RoadEnum.values()) {
            try {
                    String title = "Road-" + road.toString();
                    // pass an explicit empty quoted title (""") so `start` treats the next token as the command
                ProcessBuilder pb = new ProcessBuilder("cmd.exe", "/c", "start", "\"\"", "cmd.exe", "/k", "java", "-cp", classpath, "Classes.Road", road.toString());
                pb.directory(workDir);
                pb.start();
            } catch (Exception e) {
                e.printStackTrace();
            }
            System.out.println("Iniciando processo para a rua: " + road.toString());
        }

        // Setup initial events after starting processes
        setupInitialEvents();

        // Processa eventos até não haver mais nenhum ou parar manualmente
        while (running) {
            // process pending events (if any) and sleep to avoid busy-wait
            try {
                if (eventHandler.hasEvents()) {
                    eventHandler.processNextEvent();
                }
                Thread.sleep(200);
            } catch (InterruptedException e) {
                running = false;
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