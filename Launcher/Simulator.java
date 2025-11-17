package Launcher;

import Event.*;
import Road.RoadEnum;
import Utils.SynchronizedQueue;
import Vehicle.*;
import Crossroad.*;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import Comunication.VehicleSender;

/**
 * Classe principal que coordena toda a simulação
 * Inicia todos os cruzamentos, estradas e veículos
 */
public class Simulator {
    private volatile boolean running;
    private List<Process> processes;

    public Simulator() {
        this.running = false;
        this.processes = new ArrayList<>();
    }

    /**
     * Inicia a simulação completa
     */
    public void startSimulation() {
        if (running) {
            System.out.println("Simulação já está em execução!");
            return;
        }

        running = true;
        System.out.println("INICIANDO SIMULAÇÃO DE TRÁFEGO 🚦");
        System.out.println("=====================================");

        // Configurações
        String classpath = System.getProperty("java.class.path");
        File workDir = new File(System.getProperty("user.dir"));

        // Iniciar todos os cruzamentos
        System.out.println("Iniciando cruzamentos...");
        for (CrossroadEnum crossroad : CrossroadEnum.values()) {
            startCrossroadProcess(crossroad, classpath, workDir);
        }

        // Iniciar todas as estradas
        System.out.println("Iniciando estradas...");
        for (RoadEnum road : RoadEnum.values()) {
            startRoadProcess(road, classpath, workDir);
        }

        // Aguardar inicialização dos componentes
        try {
            System.out.println("Aguardando inicialização dos componentes...");
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Iniciar envio de veículos
        System.out.println("Iniciando gerador de veículos...");
        SynchronizedQueue<Vehicle> vehiclesToSend = new SynchronizedQueue<>();
        VehicleSender vehicleSender = new VehicleSender(vehiclesToSend);
        VehicleSpawner vehicleSpawner = new VehicleSpawner(vehiclesToSend, running, 5000);

        vehicleSender.start();
        vehicleSpawner.start();

        System.out.println("Simulação totalmente inicializada!");
        System.out.println("=====================================");

        // Manter simulação ativa
        while (running) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                running = false;
            }
        }

        // Encerramento
        stopAllProcesses();
        System.out.println("=====================================");
        System.out.println("SIMULAÇÃO TERMINADA!");
    }

    /**
     * Inicia um processo para um cruzamento
     */
    private void startCrossroadProcess(CrossroadEnum crossroad, String classpath, File workDir) {
        try {
            ProcessBuilder pb = new ProcessBuilder("cmd.exe", "/c", "start", "\"\"", "cmd.exe", "/k",
                    "java", "-cp", classpath, "Crossroad.Crossroad", crossroad.toString());
            pb.directory(workDir);
            Process process = pb.start();
            processes.add(process);

            System.out.println(" Cruzamento " + crossroad + " iniciado na porta " + crossroad.getPort());

        } catch (Exception e) {
            System.err.println(" Erro ao iniciar cruzamento " + crossroad + ": " + e.getMessage());
        }
    }

    /**
     * Inicia um processo para uma estrada
     */
    private void startRoadProcess(RoadEnum road, String classpath, File workDir) {
        try {
            ProcessBuilder pb = new ProcessBuilder("cmd.exe", "/c", "start", "\"\"", "cmd.exe", "/k",
                    "java", "-cp", classpath, "Road.Road", road.toString());
            pb.directory(workDir);
            Process process = pb.start();
            processes.add(process);

            System.out.println(" Estrada " + road + " iniciada (" +
                    road.getOrigin() + " → " + road.getDestination() +
                    ") na porta " + road.getPort());

        } catch (Exception e) {
            System.err.println(" Erro ao iniciar estrada " + road + ": " + e.getMessage());
        }
    }

    /**
     * Para todos os processos em execução
     */
    private void stopAllProcesses() {
        System.out.println("Encerrando todos os processos...");
        for (Process process : processes) {
            if (process.isAlive()) {
                process.destroy();
            }
        }
        processes.clear();
    }

    /**
     * Para a simulação
     */
    public void stopSimulation() {
        System.out.println("Parando simulação...");
        running = false;
    }

    /**
     * Verifica se a simulação está em execução
     */
    public boolean isRunning() {
        return running;
    }
}