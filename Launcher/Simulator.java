package Launcher;

import Event.*;
import Node.*;
import Road.RoadEnum;
import Utils.SynchronizedQueue;
import Vehicle.*;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import Comunication.*;

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

        String classpath = System.getProperty("java.class.path");
        File workDir = new File(System.getProperty("user.dir"));

        System.out.println("Iniciando cruzamentos...");
        for (NodeEnum crossroad : NodeEnum.values()) {
            if(EntranceEnum.toEntranceEnum(crossroad.toString()) != null){
                startEntranceProcess(crossroad, classpath, workDir);
                continue;
            }else if(ExitEnum.toExitEnum(crossroad.toString()) != null){
                startExitProcess(crossroad, classpath, workDir);
                continue;
            }else{
                startCrossroadProcess(crossroad, classpath, workDir);
            }
        }

        System.out.println("Iniciando estradas...");
        for (RoadEnum road : RoadEnum.values()) {
            startRoadProcess(road, classpath, workDir);
        }

        try {
            System.out.println("Aguardando inicialização dos componentes...");
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        //Funciona para já, mas é preciso alterar quando forem incluidas mais entradas
        System.out.println("Iniciando gerador de veículos...");
        SynchronizedQueue<Vehicle> vehiclesGenerated = new SynchronizedQueue<>();
        for (EntranceEnum entrance : EntranceEnum.values()) {
            new Sender(vehiclesGenerated, NodeEnum.toNodeEnum(entrance.toString()).getPort()).start();
        }
        new VehicleSpawner(vehiclesGenerated, running, 5000).start();

        System.out.println("Simulação totalmente inicializada!");
        System.out.println("=====================================");

        while (running) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                running = false;
            }
        }

        stopAllProcesses();
        System.out.println("=====================================");
        System.out.println("SIMULAÇÃO TERMINADA!");
    }

    private void startEntranceProcess(NodeEnum entrance, String classpath, File workDir) {
        try {
            ProcessBuilder pb = new ProcessBuilder("cmd.exe", "/c", "start", "\"\"", "cmd.exe", "/k",
                    "java", "-cp", classpath, "Node.Entrance", entrance.toString());
            pb.directory(workDir);
            Process process = pb.start();
            processes.add(process);

            System.out.println(" Entrada " + entrance + " iniciado na porta " + entrance.getPort());

        } catch (Exception e) {
            System.err.println(" Erro ao iniciar cruzamento " + entrance + ": " + e.getMessage());
        }
    }

    private void startExitProcess(NodeEnum exit, String classpath, File workDir) {
        try {
            ProcessBuilder pb = new ProcessBuilder("cmd.exe", "/c", "start", "\"\"", "cmd.exe", "/k",
                    "java", "-cp", classpath, "Node.Exit", exit.toString());
            pb.directory(workDir);
            Process process = pb.start();
            processes.add(process);

            System.out.println(" Exit " + exit + " iniciado na porta " + exit.getPort());

        } catch (Exception e) {
            System.err.println(" Erro ao iniciar cruzamento " + exit + ": " + e.getMessage());
        }
    }

    /**
     * Inicia um processo para um cruzamento
     */
    private void startCrossroadProcess(NodeEnum crossroad, String classpath, File workDir) {
        try {
            ProcessBuilder pb = new ProcessBuilder("cmd.exe", "/c", "start", "\"\"", "cmd.exe", "/k",
                    "java", "-cp", classpath, "Node.Crossroad", crossroad.toString());
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