package Event;

import java.io.*;
import java.net.*;
import java.util.*;

public class EventServer extends Thread {
    public static final int DEFAULT_PORT = 6000;
    private final int port;
    private volatile boolean running = true;
    private ServerSocket serverSocket;

    private final Map<String, List<String>> logs = Collections.synchronizedMap(new HashMap<>());

    public EventServer(int port) {
        this.port = port;
        setName("LogServer");
        setDaemon(true);
    }

    @Override
    public void run() {
        try {
            serverSocket = new ServerSocket(port);
            while (running) {
                Socket client = serverSocket.accept();
                new ClientHandler(client).start();
            }
        } catch (Exception e) {
            if (running) e.printStackTrace();
        }
    }

    public void shutdown() {
        running = false;
        try {
            if (serverSocket != null) serverSocket.close();
        } catch (Exception ignored) {}
    }

    private class ClientHandler extends Thread {
        private final Socket socket;

        ClientHandler(Socket socket) {
            this.socket = socket;
            setName("LogClientHandler");
            setDaemon(true);
        }

        @Override
        public void run() {
            try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
                String line;
                while ((line = in.readLine()) != null) {
                    // expected format: PROCESS|LEVEL|message
                    String out = line;
                    String proc;
                    int idx = line.indexOf('|');
                    if (idx > 0) {
                        proc = line.substring(0, idx).trim();
                        if (proc.isEmpty()) proc = "Local";
                    } else {
                        // lines without a process prefix are treated as Local
                        proc = "Local";
                    }
                    synchronized (logs) {
                        logs.computeIfAbsent(proc, k -> new ArrayList<>()).add(out);
                    }
                }
            } catch (Exception ignored) {}
            try { socket.close(); } catch (Exception ignored) {}
        }
    }

    /**
     * Return and clear accumulated logs since last call.
     * The returned map is a shallow copy: process -> list of lines.
     */
    public Map<String, List<String>> drainLogs() {
        Map<String, List<String>> snapshot = new HashMap<>();
        synchronized (logs) {
            for (Map.Entry<String, List<String>> e : logs.entrySet()) {
                snapshot.put(e.getKey(), new ArrayList<>(e.getValue()));
                e.getValue().clear();
            }
        }
        return snapshot;
    }
}
