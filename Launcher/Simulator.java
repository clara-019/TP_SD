package Launcher;

import Enums.*;
import Event.Event;
import Event.EventHandler;
import Road.RoadEnum;
import Utils.SynchronizedQueue;
import Vehicle.Vehicle;
import Vehicle.VehicleSpawner;
import Crossroad.*;
import java.io.File;

import Comunication.VehicleSender;

public class Simulator {
    private EventHandler eventHandler;
    private boolean running;

    public Simulator() {
        this.running = false;
        this.eventHandler = new EventHandler(null);
    }

    public void startSimulation() {
        if (running) {
            System.out.println("Simulação já está a correr!");
            return;
        }
        running = true;
        System.out.println("SIMULAÇÃO INICIADA!");
        System.out.println("======================");
        String classpath = System.getProperty("java.class.path");
        File workDir = new File(System.getProperty("user.dir"));

        for (CrossroadEnum cr : CrossroadEnum.values()) {
            try {
                    String title = "Crossroad-" + cr.toString();
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
                ProcessBuilder pb = new ProcessBuilder("cmd.exe", "/c", "start", "\"\"", "cmd.exe", "/k", "java", "-cp", classpath, "Classes.Road", road.toString());
                pb.directory(workDir);
                pb.start();
            } catch (Exception e) {
                e.printStackTrace();
            }
            System.out.println("Iniciando processo para a rua: " + road.toString());
        }

        SynchronizedQueue<Vehicle> vehiclesToSend = new SynchronizedQueue<Vehicle>();
        new VehicleSender(vehiclesToSend).start();
        new VehicleSpawner(vehiclesToSend, running).start();

        while (running) {
            try {
                if (eventHandler.hasEvents()) {
                    eventHandler.processNextEvent();
                }
                Thread.sleep(200);
            } catch (InterruptedException e) {
                running = false;
            }
        }

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