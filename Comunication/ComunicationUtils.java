package Comunication;

import java.io.*;
import java.net.Socket;

/**
 * Classe utilitária para envio e receção de objetos via sockets
 * Garante sincronização nas operações de comunicação
 */
public class ComunicationUtils {

    /**
     * Envia um objeto através de um socket de forma sincronizada
     * @param s Socket de destino
     * @param obj Objeto a ser enviado
     * @throws IOException Em caso de erro de I/O
     */
    public static synchronized void sendObject(Socket s, Object obj) throws IOException {
        ObjectOutputStream out = new ObjectOutputStream(s.getOutputStream());
        out.writeObject(obj);
        out.flush();
    }

    /**
     * Recebe um objeto de um InputStream
     * @param in InputStream de origem
     * @return Objeto recebido
     * @throws IOException Em caso de erro de I/O
     * @throws ClassNotFoundException Se a classe do objeto não for encontrada
     */
    public static Object reciveObject(InputStream in) throws IOException, ClassNotFoundException {
        ObjectInputStream ois = new ObjectInputStream(in);
        return ois.readObject();
    }
}