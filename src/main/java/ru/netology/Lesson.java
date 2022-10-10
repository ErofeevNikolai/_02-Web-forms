package ru.netology;

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.sql.SQLOutput;
import java.util.*;

public class Lesson {
    public static final String GET = "GET";
    public static final String POST = "POST";

    public static void main(String[] args) {
        final var allowedMethods = List.of(GET, POST);

        try (final var serverSocket = new ServerSocket(9999)) {
            while (true) {
                try (
                        final var socket = serverSocket.accept();
                        final var in = new BufferedInputStream(socket.getInputStream());
                        final var out = new BufferedOutputStream(socket.getOutputStream());
                ) {
                    /*
                    выставляем максимальное значение размера входящего запроса
                    (требуется для безопасности: исключаем тело)
                     */
                    // лимит на request line + заголовки
                    final var limit = 4096;
                    /*
                    mark - ставит в буфире отметку на длине limit
                    говорим что из буфера in считывается данные до отметки limit
                    создаем буфер в 4 килобайта
                    считываем поток в буфер
                     */
                    in.mark(limit);
                    final var buffer = new byte[limit];
                    final var read = in.read(buffer);


                    /*
                    ищем вхождения и переносов строки состоящего из двух символов
                    функция indexOf написана ниже, если искомых байт нет возвращаем -1
                    если requestLineEnd = -1 -> вызываем ошибку (badRequest) Завершаем текущий цикл
                     */
                    // ищем request line
                    final var requestLineDelimiter = new byte[]{'\r', '\n'};
                    final var requestLineEnd = indexOf(buffer, requestLineDelimiter, 0, read);
                    if (requestLineEnd == -1) {
                        badRequest(out);
                        continue;
                    }

                    /*
                    если все ок считываем request line из буфера в массив, далее создаем строку, и разделяем строку по пробелам
                     */
                    // читаем request line
                    final var requestLine = new String(Arrays.copyOf(buffer, requestLineEnd)).split(" ");
                    if (requestLine.length != 3) {
                        badRequest(out);
                        continue;
                    }

                    /*
                    Считываем метод запроса
                    если метод запроса не входит в список разрешенных
                    выводим исключение и прекращаем текущий цикл
                    если все ок то логируем
                     */
                    //Get / index.html HTTP/1.1
                    final var method = requestLine[0];
                    if (!allowedMethods.contains(method)) {
                        badRequest(out);
                        continue;
                    }
                    System.out.println(method);

                    /*
                    аналогично проверяем путь, на то что он начинается с /

                     */
                    final var path = requestLine[1];
                    if (!path.startsWith("/")) {
                        badRequest(out);
                        continue;
                    }
                    System.out.println(path);


                    /*
                    далее требуется считать заголовки
                    из структуры запроса мы знаем что заголовки отделяются двоим переносом -> это будет конец строки
                    начало заголовков нам известно, это конец метода + 2 байта (requestLineEnd + requestLineDelimiter)
                    если метод index.of не может найти конец заголовка получаем -1 и прекращаем текущую итерацию
                     */
                    // ищем заголовки
                    final var headersDelimiter = new byte[]{'\r', '\n', '\r', '\n'};
                    final var headersStart = requestLineEnd + requestLineDelimiter.length;
                    final var headersEnd = indexOf(buffer, headersDelimiter, headersStart, read);
                    if (headersEnd == -1) {
                        badRequest(out);
                        continue;
                    }

                    /*
                    Считываем заголовки.
                    Для работы с заголовками:
                    скидываем буфер начала(это нужно тк в процессе чтения мы могли остановиться в буфере не в начале)
                    считываем промежуток байт (конец нашего промежутка - промежуток который был прочитан ранее)
                    сооздаем массив строк и создаем сплит (делим по) /r/n
                     */
                    // отматываем на начало буфера
                    in.reset();
                    // пропускаем requestLine
                    in.skip(headersStart);

                    final var headersBytes = in.readNBytes(headersEnd - headersStart);
                    final var headers = Arrays.asList(new String(headersBytes).split("\r\n"));
                    System.out.println("Заголовки: ");
                    System.out.println(headers);


                    /*
                    Считываем тело
                    пропускаем байты которые нас отделяют от тела запроса
                    вычленяем Сontent-Length.
                    Если получается извлечь его значение,
                    то мы его распарсим в целое число
                    и считваем строку из буфера


                     */

                    // для GET тела нет -> тогда пропускаем данный метод
                    if (!method.equals(GET)) {
                        in.skip(headersDelimiter.length);
                        // вычитываем Content-Length, чтобы прочитать body
                        final var contentLength = extractHeader(headers, "Content-Length");
                        if (contentLength.isPresent()) {
                            final var length = Integer.parseInt(contentLength.get());
                            final var bodyBytes = in.readNBytes(length);

                            final var body = new String(bodyBytes);
                            System.out.println(body);
                        }
                    }

                    out.write((
                            "HTTP/1.1 200 OK\r\n" +
                                    "Content-Length: 0\r\n" +
                                    "Connection: close\r\n" +
                                    "\r\n"
                    ).getBytes());
                    out.flush();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static Optional<String> extractHeader(List<String> headers, String header) {
        return headers.stream()
                .filter(o -> o.startsWith(header))
                .map(o -> o.substring(o.indexOf(" ")))
                .map(String::trim)
                .findFirst();
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

    // from google guava with modifications
    private static int indexOf(byte[] array, byte[] target, int start, int max) {
        outer:
        for (int i = start; i < max - target.length + 1; i++) {
            for (int j = 0; j < target.length; j++) {
                if (array[i + j] != target[j]) {
                    continue outer;
                }
            }
            return i;
        }
        return -1;
    }
}