package Launcher;

import Node.*;

import java.io.File;
import java.util.*;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.TimeUnit;

import Event.*;

public class Simulator {
    private volatile boolean running;
    private java.util.Map<NodeEnum, Process> processes;

    private PriorityBlockingQueue<Event> eventQueue = new PriorityBlockingQueue<Event>(10, Comparator.comparingLong(Event::getLogicalClock));
    
    private EventHandler eventHandler;
    private String javaCmd;

    public Simulator() {
        this.running = false;
        this.processes = new java.util.HashMap<>();
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

        eventHandler = new EventHandler(eventQueue, running);
        eventHandler.start();

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
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        System.out.println("Simulation fully initialized!");
        System.out.println("=====================================");
    }

    private void startEntranceProcess(NodeEnum entrance, String classpath, File workDir) {
        try {
            ProcessBuilder pb = new ProcessBuilder(this.javaCmd, "-cp", classpath, "Node.Entrance", entrance.toString());
            pb.directory(workDir);
            Process process = pb.start();
            processes.put(entrance, process);

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
            processes.put(exit, process);

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
            processes.put(crossroad, process);

            System.out.println(" Crossroad " + crossroad + " started on port " + crossroad.getPort());

        } catch (Exception e) {
            System.err.println(" Error starting " + crossroad + ": " + e.getMessage());
        }
    }


    private void stopAllProcesses() {
        System.out.println("Stopping all processes...");
        for (Map.Entry<NodeEnum, Process> e : new ArrayList<>(processes.entrySet())) {
            NodeEnum n = e.getKey();
            Process p = e.getValue();
            if (p == null) {
                processes.remove(n);
                continue;
            }
            try {
                if (!p.isAlive()) {
                    System.out.println("Process " + n + " already stopped");
                    processes.remove(n);
                    continue;
                }
                p.destroy();

                if (!p.waitFor(1, TimeUnit.SECONDS)) {
                    System.out.println("Process " + n + " did not exit after destroy(), forcing destroyForcibly()");
                    p.destroyForcibly();
                    if (!p.waitFor(1, TimeUnit.SECONDS)) {
                        System.out.println("Process " + n + " still alive after destroyForcibly()");
                    } else {
                        System.out.println("Process " + n + " terminated after destroyForcibly()");
                    }
                } else {
                    System.out.println("Process " + n + " exited cleanly after destroy()");
                }
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                System.out.println("Interrupted while stopping process " + n);
            } catch (Throwable ex) {
                System.out.println("Error stopping process " + n + ": " + ex.getMessage());
            } finally {
                processes.remove(n);
            }
        }
    }

    public void stopSimulation() {
        stopAllProcesses();
        eventHandler.stopHandler();
        System.out.println("Stopped simulation...");
        running = false;
    }

    public void stopEntranceProcesses() {
        System.out.println("Stopping entrance processes (graceful)");
        for (Map.Entry<NodeEnum, Process> e : new ArrayList<>(processes.entrySet())) {
            NodeEnum n = e.getKey();
            Process p = e.getValue();
            if (n.getType() == NodeType.ENTRANCE) {
                try {
                    if (p == null) {
                        processes.remove(n);
                        continue;
                    }
                    if (!p.isAlive()) {
                        System.out.println("Entrance process " + n + " already stopped");
                        processes.remove(n);
                        continue;
                    }
                    p.destroy();
                    if (!p.waitFor(1, TimeUnit.SECONDS)) {
                        System.out.println("Entrance " + n + " did not exit, forcing destroyForcibly()");
                        p.destroyForcibly();
                        if (!p.waitFor(1, TimeUnit.SECONDS)) System.out.println("Entrance " + n + " still alive after forcible destroy");
                    }
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                } catch (Throwable t) {
                    System.out.println("Error stopping entrance " + n + ": " + t.getMessage());
                } finally {
                    processes.remove(n);
                }
            }
        }
    }

    public boolean isRunning() {
        return running;
    }

    public PriorityBlockingQueue<Event> getEventQueue() {
        return eventQueue;
    }
}