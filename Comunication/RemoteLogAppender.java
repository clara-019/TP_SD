package Comunication;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.time.Instant;

/**
 * Utility for child processes to redirect System.out/System.err to the central LogServer.
 * Usage from child `main`:
 *    RemoteLogAppender.install("TP_Exit_S", "localhost", 6000);
 */
public class RemoteLogAppender {
    private static volatile PrintWriter writer;
    private static volatile Socket socket;
    private static String processName = "unknown";

    public static void install(String procName, String host, int port) {
        processName = procName;
        try {
            socket = new Socket();
            socket.connect(new InetSocketAddress(host, port), 2000);
            writer = new PrintWriter(socket.getOutputStream(), true);

            // redirect System.out and System.err
            System.setOut(new java.io.PrintStream(new java.io.OutputStream() {
                private StringBuilder sb = new StringBuilder();
                @Override public void write(int b) {
                    if (b == '\n') {
                        sendLine("INFO", sb.toString());
                        sb.setLength(0);
                    } else {
                        sb.append((char)b);
                    }
                }
            }, true));

            System.setErr(new java.io.PrintStream(new java.io.OutputStream() {
                private StringBuilder sb = new StringBuilder();
                @Override public void write(int b) {
                    if (b == '\n') {
                        sendLine("ERROR", sb.toString());
                        sb.setLength(0);
                    } else {
                        sb.append((char)b);
                    }
                }
            }, true));

            sendLine("INFO", "RemoteLogAppender installed at " + Instant.now().toString());

        } catch (IOException e) {
            System.err.println("[RemoteLogAppender] Could not connect to log server " + host + ":" + port + " - " + e.getMessage());
        }
    }

    private static synchronized void sendLine(String level, String msg) {
        if (writer != null) {
            writer.println(processName + "|" + level + "|" + msg);
        }
    }

    public static void shutdown() {
        try { if (writer != null) writer.close(); } catch (Exception ignored) {}
        try { if (socket != null) socket.close(); } catch (Exception ignored) {}
    }
}
