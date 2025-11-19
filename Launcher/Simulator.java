package Launcher;

import Node.*;
import Road.RoadEnum;
import Utils.SynchronizedQueue;
import Vehicle.*;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import Comunication.*;
public class Simulator {
    private volatile boolean running;
    private List<Process> processes;
    private List<Sender> senderThreads;
    private VehicleSpawner vehicleSpawner;
    private List<String> processWindowTitles;

    public Simulator() {
        this.running = false;
        this.processes = new ArrayList<>();
        this.senderThreads = new ArrayList<>();
        this.vehicleSpawner = null;
        this.processWindowTitles = new ArrayList<>();
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

        System.out.println("Starting entrances and crossroads...");
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

        System.out.println("Starting roads...");
        for (RoadEnum road : RoadEnum.values()) {
            startRoadProcess(road, classpath, workDir);
        }

        try {
            System.out.println("Waiting for component initialization...");
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        System.out.println("Starting vehicle spawner...");
        SynchronizedQueue<Vehicle> vehiclesGenerated = new SynchronizedQueue<>();
        for (NodeEnum entrance : NodeEnum.getEntrances()) {
            Sender sender = new Sender(vehiclesGenerated, entrance.getPort());
            sender.start();
            senderThreads.add(sender);
        }

        vehicleSpawner = new VehicleSpawner(vehiclesGenerated, true, 5000);
        vehicleSpawner.start();

        System.out.println("Simulation fully initialized!");
        System.out.println("=====================================");

        while (running) {
        }

        stopAllProcesses();
        System.out.println("=====================================");
        System.out.println("SIMULATION TERMINATED!");
    }

    private void startEntranceProcess(NodeEnum entrance, String classpath, File workDir) {
        try {
                        String title = "TP_Entrance_" + entrance.toString();
                        ProcessBuilder pb = new ProcessBuilder("cmd.exe", "/c", "start", '"' + title + '"',
                        "java", "-cp", classpath, "Node.Entrance.Entrance", entrance.toString());
            pb.directory(workDir);
            Process process = pb.start();
            processes.add(process);
                processWindowTitles.add(title);

            System.out.println(" Entrance " + entrance + " started on port " + entrance.getPort());

        } catch (Exception e) {
            System.err.println(" Error starting " + entrance + ": " + e.getMessage());
        }
    }

    private void startExitProcess(NodeEnum exit, String classpath, File workDir) {
        try {
                        String title = "TP_Exit_" + exit.toString();
                        ProcessBuilder pb = new ProcessBuilder("cmd.exe", "/c", "start", '"' + title + '"',
                        "java", "-cp", classpath, "Node.Exit.Exit", exit.toString());
            pb.directory(workDir);
            Process process = pb.start();
            processes.add(process);
                processWindowTitles.add(title);

            System.out.println(" Exit " + exit + " started on port " + exit.getPort());

        } catch (Exception e) {
            System.err.println(" Error starting " + exit + ": " + e.getMessage());
        }
    }

    private void startCrossroadProcess(NodeEnum crossroad, String classpath, File workDir) {
        try {
                        String title = "TP_Crossroad_" + crossroad.toString();
                        ProcessBuilder pb = new ProcessBuilder("cmd.exe", "/c", "start", '"' + title + '"',
                        "java", "-cp", classpath, "Node.Crossroad.Crossroad", crossroad.toString());
            pb.directory(workDir);
            Process process = pb.start();
            processes.add(process);
                processWindowTitles.add(title);

            System.out.println(" Crossroad " + crossroad + " started on port " + crossroad.getPort());

        } catch (Exception e) {
            System.err.println(" Error starting " + crossroad + ": " + e.getMessage());
        }
    }

    private void startRoadProcess(RoadEnum road, String classpath, File workDir) {
        try {
                        String title = "TP_Road_" + road.toString();
                        ProcessBuilder pb = new ProcessBuilder("cmd.exe", "/c", "start", '"' + title + '"',
                        "java", "-cp", classpath, "Road.Road", road.toString());
            pb.directory(workDir);
            Process process = pb.start();
            processes.add(process);
                processWindowTitles.add(title);

            System.out.println(" Road " + road + " started (" +
                    road.getOrigin() + " -> " + road.getDestination() +
                    ") na porta " + road.getPort());

        } catch (Exception e) {
            System.err.println(" Error starting " + road + ": " + e.getMessage());
        }
    }


    private void stopAllProcesses() {
        System.out.println("Stopping all processes...");

        try {
            if (vehicleSpawner != null && vehicleSpawner.isAlive()) {
                vehicleSpawner.stopSpawning();
                try { vehicleSpawner.join(2000); } catch (InterruptedException ignored) {}
            }
        } catch (Exception ignored) {}

        for (Thread s : senderThreads) {
            if (s != null && s.isAlive()) {
                s.interrupt();
                try { s.join(1000); } catch (InterruptedException ignored) {}
            }
        }
        senderThreads.clear();

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

        // Ensure any cmd windows opened with `start` are closed by title
        for (String title : processWindowTitles) {
            try {
                ProcessBuilder killPb = new ProcessBuilder("cmd.exe", "/c", "taskkill", "/F", "/FI", "WINDOWTITLE eq " + title);
                killPb.redirectErrorStream(true);
                Process killProc = killPb.start();
                try (java.io.InputStream is = killProc.getInputStream()) {
                    byte[] buf = new byte[1024];
                    while (is.read(buf) > 0) {}
                } catch (Exception ignored) {}
            } catch (Exception ignored) {}
        }
        processWindowTitles.clear();
    }

    public void stopSimulation() {
        System.out.println("Stopped simulation...");
        stopAllProcesses();
        running = false;
    }


    public boolean isRunning() {
        return running;
    }
}