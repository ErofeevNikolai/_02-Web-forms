package ru.netology;

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.spec.RSAOtherPrimeInfo;
import java.time.LocalDateTime;
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

            // если метод и путь есть в коллекции обрабатываем запрос
            if (mapHandle.containsKey(request.getMethod())) {
                if (mapHandle.get(request.getMethod()).containsKey(request.getPath())) {
                    mapHandle.get(request.getMethod()).get(request.getPath()).handle(request, out);
                }
            }

            final var path = parts[1];
            final var method = parts[0];
            if (!allowedMethods.contains(method)) {
                out.write((
                        "HTTP/1.1 404 Not Found\r\n" +
                                "Content-Length: 0\r\n" +
                                "Connection: close\r\n" +
                                "\r\n"
                ).getBytes());
                out.flush();
                return;

            }
            System.out.println(method);


            final var filePath = Path.of(".", "public", path);
            final var mimeType = Files.probeContentType(filePath);

            //обработка запроса

            if (path.equals("/classic.html")) {
                final var template = Files.readString(filePath);
                final var content = template.replace(
                        "{time}",
                        LocalDateTime.now().toString()
                ).getBytes();
                out.write((
                        "HTTP/1.1 200 OK\r\n" +
                                "Content-Type: " + mimeType + "\r\n" +
                                "Content-Length: " + content.length + "\r\n" +
                                "Connection: close\r\n" +
                                "\r\n"
                ).getBytes());
                out.write(content);
                out.flush();
            }

            final var length = Files.size(filePath);
            out.write((
                    "HTTP/1.1 200 OK\r\n" +
                            "Content-Type: " + mimeType + "\r\n" +
                            "Content-Length: " + length + "\r\n" +
                            "Connection: close\r\n" +
                            "\r\n"
            ).getBytes());
            Files.copy(filePath, out);
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
}
