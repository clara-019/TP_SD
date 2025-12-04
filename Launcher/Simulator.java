package Launcher;

import Node.*;

import java.io.File;
import java.util.*;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.TimeUnit;

import Event.*;

/**
 * Simulator coordinates external node processes and collects events.
 * <p>
 * The Simulator launches external Java processes for each node
 * (entrances, crossroads and exits), starts an {@link EventHandler}
 * that reads events into a priority queue and exposes that queue to
 * callers. It also provides lifecycle control methods to start,
 * stop and request a graceful stop of entrance processes.
 */
public class Simulator {
    private volatile boolean running;
    private java.util.Map<NodeEnum, Process> processes;

    private PriorityBlockingQueue<Event> eventQueue = new PriorityBlockingQueue<Event>(10,
            Comparator.comparingLong(Event::getLogicalClock));

    private EventHandler eventHandler;
    private String javaCmd;

    /**
     * Create a new Simulator instance.
     * <p>
     * The constructor initializes internal maps and determines the
     * platform java executable path used to spawn node processes.
     */
    public Simulator() {
        this.running = false;
        this.processes = new java.util.HashMap<>();
        this.javaCmd = System.getProperty("java.home") + File.separator + "bin" + File.separator + "java";
    }

    /**
     * Start the simulation.
     * <p>
     * This method starts the internal {@link EventHandler} to collect
     * events, launches external processes for each configured node and
     * waits briefly for initialization. If the simulator is already
     * running the call is a no-op.
     */
    public void startSimulation() {
        if (running) {
            System.out.println("Simulation is already running!");
            return;
        }
        running = true;

        System.out.println("STARTING TRAFFIC SIMULATION");
        System.out.println("=====================================");

        this.eventHandler = new EventHandler(eventQueue, running);
        this.eventHandler.start();

        System.out.println("Starting nodes...");
        for (NodeEnum node : NodeEnum.values()) {
            if (node.getType() == NodeType.ENTRANCE) {
                startProcess(node, "Node.Entrance", "Entrance");
                continue;
            } else if (node.getType() == NodeType.EXIT) {
                startProcess(node, "Node.Exit", "Exit");
                continue;
            } else {
                startProcess(node, "Node.Crossroad", "Crossroad");
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

    /**
     * Start an external Java process for the given node.
     * <p>
     * The method uses {@link ProcessBuilder} to spawn a JVM running
     * the specified {@code mainClass} with the node enum as an argument.
     * The resulting {@link Process} is stored in the {@code processes}
     * map so it can be stopped later.
     *
     * @param node      the node enum to start
     * @param mainClass the fully-qualified main class name to run
     * @param roleLabel human-readable label used for logging
     */
    private void startProcess(NodeEnum node, String mainClass, String roleLabel) {
        try {
            String classpath = System.getProperty("java.class.path");
            File workDir = new File(System.getProperty("user.dir"));
            ProcessBuilder pb = new ProcessBuilder(this.javaCmd, "-cp", classpath, mainClass, node.toString());
            pb.directory(workDir);
            Process process = pb.start();
            this.processes.put(node, process);

            System.out.println(" " + roleLabel + " " + node + " started on port " + node.getPort());

        } catch (Exception e) {
            System.err.println(" Error starting " + node + ": " + e.getMessage());
        }
    }

    /**
     * Stop and clean up all spawned node processes.
     * <p>
     * The method iterates a snapshot of the {@code processes} map,
     * attempts to destroy each process and waits briefly for it to
     * terminate. If a process does not terminate it is forcibly
     * destroyed. Any problems are logged to stdout/stderr but the loop
     * continues to ensure all processes are attempted.
     */
    private void stopAllProcesses() {
        System.out.println("Stopping all processes...");
        for (Map.Entry<NodeEnum, Process> e : new ArrayList<>(this.processes.entrySet())) {
            NodeEnum n = e.getKey();
            Process p = e.getValue();
            if (p == null) {
                this.processes.remove(n);
                continue;
            }
            try {
                if (!p.isAlive()) {
                    System.out.println("Process " + n + " already stopped");
                    this.processes.remove(n);
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
                    this.processes.remove(n);
            }
        }
    }

    /**
     * Stop the simulation immediately.
     * <p>
     * This stops all node processes, stops the event handler and
     * marks the simulator as not running.
     */
    public void stopSimulation() {
        stopAllProcesses();
        this.eventHandler.stopHandler();
        System.out.println("Stopped simulation...");
        this.running = false;
    }

    /**
     * Request a graceful stop of entrance processes only.
     * <p>
     * This is used when the simulator should stop accepting new
     * vehicles but allow existing traffic to finish. The method attempts
     * to stop all entrance processes and waits briefly for termination.
     */
    public void stopEntranceProcesses() {
        System.out.println("Stopping entrance processes (graceful)");
        for (Map.Entry<NodeEnum, Process> e : new ArrayList<>(this.processes.entrySet())) {
            NodeEnum n = e.getKey();
            Process p = e.getValue();
            if (n.getType() == NodeType.ENTRANCE) {
                try {
                    if (p == null) {
                        this.processes.remove(n);
                        continue;
                    }
                    if (!p.isAlive()) {
                        System.out.println("Entrance process " + n + " already stopped");
                        this.processes.remove(n);
                        continue;
                    }
                    p.destroy();
                    if (!p.waitFor(1, TimeUnit.SECONDS)) {
                        System.out.println("Entrance " + n + " did not exit, forcing destroyForcibly()");
                        p.destroyForcibly();
                        if (!p.waitFor(1, TimeUnit.SECONDS))
                            System.out.println("Entrance " + n + " still alive after forcible destroy");
                    }
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                } catch (Throwable t) {
                    System.out.println("Error stopping entrance " + n + ": " + t.getMessage());
                } finally {
                    this.processes.remove(n);
                }
            }
        }
    }

    /**
     * Return whether the simulator is currently running.
     *
     * @return true if running, false otherwise
     */
    public boolean isRunning() {
        return this.running;
    }

    /**
     * Return the simulator's event queue.
     *
     * @return a priority blocking queue of {@link Event}
     */
    public PriorityBlockingQueue<Event> getEventQueue() {
        return this.eventQueue;
    }
}