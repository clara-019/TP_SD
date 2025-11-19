package Comunication;

import java.io.*;
import java.net.Socket;

/**
 * Utility class for sending and receiving objects via sockets
 * Ensures synchronization in communication operations
 */
public class ComunicationUtils {

    /**
     * Sends an object through a socket in a synchronized manner
     * @param s Destination socket
     * @param obj Object to be sent
     * @throws IOException In case of I/O error
     */
    public static synchronized void sendObject(Socket s, Object obj) throws IOException {
        ObjectOutputStream out = new ObjectOutputStream(s.getOutputStream());
        out.writeObject(obj);
        out.flush();
    }

    /**
     * Receives an object from an InputStream
     * @param in Source InputStream
     * @return Received object
     * @throws IOException In case of I/O error
     * @throws ClassNotFoundException If the object's class is not found
     */
    public static Object reciveObject(InputStream in) throws IOException, ClassNotFoundException {
        ObjectInputStream ois = new ObjectInputStream(in);
        return ois.readObject();
    }
}