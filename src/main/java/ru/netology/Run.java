package ru.netology;


import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Run implements Runnable {
    public static final String GET = "GET";
    public static final String POST = "POST";
    final List<String> allowedMethods = List.of(GET, POST);

    private final Socket socket;
    private Request request;
    private ConcurrentHashMap<String, Map<String, Handler>> mapHandle;

    Run(Socket socket, ConcurrentHashMap concurrentHashMap) {
        this.socket = socket;
        this.mapHandle = concurrentHashMap;
    }

    @Override
    public void run() {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             BufferedOutputStream out = new BufferedOutputStream(socket.getOutputStream());
        ) {

            final var requestLine = in.readLine();
            final var parts = requestLine.split(" ");


            if (parts.length != 3) {
                return;
            }

            //Создаем объект запроса
            request = new Request(requestLine);

            final var path = parts[1];
            final var method = parts[0];
            if (!allowedMethods.contains(method)) {
                badRequest(out);
                return;
            }
            System.out.println(method);

            // если метод и путь есть в коллекции обрабатываем запрос
            if (mapHandle.containsKey(request.getMethod())) {
                if (mapHandle.get(request.getMethod()).containsKey(request.getPath())) {
                    mapHandle.get(request.getMethod()).get(request.getPath()).handle(request, out);
                }
            }

            out.write((
                    "HTTP/1.1 200 OK\r\n" +
                            "Content-Type: " + "mimeType" + "\r\n" +
                            "Content-Length: " + "length" + "\r\n" +
                            "Connection: close\r\n" +
                            "\r\n"
            ).getBytes());
            out.flush();

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    private static void badRequest(BufferedOutputStream out) throws IOException {
        out.write((
                "HTTP/1.1 400 Bad Request\r\n" +
                        "Content-Length: 0\r\n" +
                        "Connection: close\r\n" +
                        "\r\n"
        ).getBytes());
        out.flush();
    }
}
