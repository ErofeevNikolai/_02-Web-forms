package ru.netology;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {
    private final int port;
    ConcurrentHashMap<String, Map<String, Handler>> mapHandle = new ConcurrentHashMap<>();

    public static void main(String[] args) {
        Server server = new Server(9999);
        server.addHandler("GET", "/messages", new Handler() {
            public void handle(Request request, BufferedOutputStream responseStream) {
                System.out.println("Логика обработки запроса GET /messages");
            }
        });
        server.startServer();
    }


    public Server(int port) {
        this.port = port;
    }

    public void startServer() {
        System.out.println("Server start");
        ExecutorService threadPool = Executors.newFixedThreadPool(64);
        try (final var serverSocket = new ServerSocket(port)) {
            while (true) {
                try {
                    Socket socket = serverSocket.accept();
                    threadPool.submit(new Run(socket, mapHandle));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void addHandler(String method, String path, Handler handler) {
        mapHandle.put(method, new HashMap<>());
        mapHandle.get(method).put(path, handler);
    }


}



