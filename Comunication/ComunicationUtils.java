package Comunication;

import java.io.*;
import java.net.Socket;

public class ComunicationUtils {

    public static synchronized void sendObject(Socket s, Object obj) throws IOException {
        ObjectOutputStream out = new ObjectOutputStream(s.getOutputStream());
        out.writeObject(obj);
        out.flush();
    }

    public static Object reciveObject(InputStream in) throws IOException, ClassNotFoundException {
        ObjectInputStream ois = new ObjectInputStream(in);
        return ois.readObject();
    }
}
