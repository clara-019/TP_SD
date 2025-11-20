package Comunication;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.Socket;

import Message.*;
import Utils.*;

public class MessageClient extends Thread {
    private static final int PORT = MessageServer.getPort();
    private final SynchronizedQueue<Message> queue;

    public MessageClient(SynchronizedQueue<Message> queue) {
        this.queue = queue; 
    }

    @Override
    public void run() {
        while (!Thread.currentThread().isInterrupted()) {
            Socket socket = null;
            try {
                socket = new Socket("localhost", PORT);
                ObjectOutputStream out = null;

                try {
                    out = new ObjectOutputStream(socket.getOutputStream());
                    while (!Thread.currentThread().isInterrupted()) {
                        try {
                            Message message = queue.remove();
                            if (message != null) {
                                out.writeObject(message);
                                out.flush();
                                System.out.println(message.toString());
                            }
                            Thread.sleep(300);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            break;
                        }
                    }
                } finally {
                    if (out != null) {
                        try {
                            out.close();
                        } catch (IOException ignored) {
                        }
                    }
                }

            } catch (Exception e) {
            } finally {
                if (socket != null) {
                    try {
                        socket.close();
                    } catch (IOException ignored) {
                    }
                }
            }

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}

