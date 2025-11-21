package Launcher;

import Node.*;
import java.io.File;
import java.util.*;

public class Simulator {
    private volatile boolean running;
    private List<Process> processes;
    
    private String javaCmd;

    public Simulator() {
        this.running = false;
        this.processes = new ArrayList<>();
        this.javaCmd = System.getProperty("java.home") + File.separator + "bin" + File.separator + "java";
    }

    public void startSimulation() {
        if (running) {
            System.out.println("Simulation is already running!");
            return;
        }
        running = true;

        System.out.println("STARTING TRAFFIC SIMULATION");
        System.out.println("=====================================");

        String classpath = System.getProperty("java.class.path");
        File workDir = new File(System.getProperty("user.dir"));

        System.out.println("Starting nodes...");
        for (NodeEnum node : NodeEnum.values()) {
            if (node.getType() == NodeType.ENTRANCE) {
                startEntranceProcess(node, classpath, workDir);
                continue;
            } else if (node.getType() == NodeType.EXIT) {
                startExitProcess(node, classpath, workDir);
                continue;
            } else {
                startCrossroadProcess(node, classpath, workDir);
            }
        }

        try {
            System.out.println("Waiting for component initialization...");
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        System.out.println("Entrances and crossroads will run as separate processes and generate/forward vehicles.");

        System.out.println("Simulation fully initialized!");
        System.out.println("=====================================");

        while (running) {
            try { Thread.sleep(200); } catch (InterruptedException ignored) {}
        }

        stopAllProcesses();
        System.out.println("=====================================");
        System.out.println("SIMULATION TERMINATED!");
    }

    private void startEntranceProcess(NodeEnum entrance, String classpath, File workDir) {
        try {
            ProcessBuilder pb = new ProcessBuilder(this.javaCmd, "-cp", classpath, "Node.Entrance", entrance.toString());
            pb.directory(workDir);
            Process process = pb.start();
            processes.add(process);

            System.out.println(" Entrance " + entrance + " started on port " + entrance.getPort());

        } catch (Exception e) {
            System.err.println(" Error starting " + entrance + ": " + e.getMessage());
        }
    }

    private void startExitProcess(NodeEnum exit, String classpath, File workDir) {
        try {
            ProcessBuilder pb = new ProcessBuilder(this.javaCmd, "-cp", classpath, "Node.Exit", exit.toString());
            pb.directory(workDir);
            Process process = pb.start();
            processes.add(process);

            System.out.println(" Exit " + exit + " started on port " + exit.getPort());

        } catch (Exception e) {
            System.err.println(" Error starting " + exit + ": " + e.getMessage());
        }
    }

    private void startCrossroadProcess(NodeEnum crossroad, String classpath, File workDir) {
        try {
            ProcessBuilder pb = new ProcessBuilder(this.javaCmd, "-cp", classpath, "Node.Crossroad", crossroad.toString());
            pb.directory(workDir);
            Process process = pb.start();
            processes.add(process);

            System.out.println(" Crossroad " + crossroad + " started on port " + crossroad.getPort());

        } catch (Exception e) {
            System.err.println(" Error starting " + crossroad + ": " + e.getMessage());
        }
    }


    private void stopAllProcesses() {
        System.out.println("Stopping all processes...");

        for (Process process : processes) {
            try {
                if (process.isAlive()) {
                    process.destroy();
                    try { Thread.sleep(200); } catch (InterruptedException ignored) {}
                    if (process.isAlive()) process.destroyForcibly();
                }
            } catch (Exception ignored) {}
        }
        processes.clear();


    }

    public void stopSimulation() {
        stopAllProcesses();
        System.out.println("Stopped simulation...");
        running = false;
    }


    public boolean isRunning() {
        return running;
    }
}