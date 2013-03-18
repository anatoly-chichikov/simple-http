package me.chichikov.simplehttp.server;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.*;
import java.util.List;
import java.util.Map;

/**
 * SimpleHttpHandler - обрабатывает каждый авторизованный http
 * запрос. Логика обработки запроса находится в методе handle().
 *
 * @author Anatoly Chichikov (17.03.2013)
 * @since 1.7
 */
class SimpleHttpHandler implements HttpHandler {

    /**
     * Список, содержащий в себе два отображения (в обоих ключами являются запросы пользователей):<br />
     * - первое (с индексом 0) это непосредственно итоговые ответы сервера.
     * - второе (с индексом 1) это типы ответов.
     */
    private List<Map<String, String>> responses;

    /**
     * Метод определенный в HttpHandler, служит для обработки каждого входящего
     * http запроса. В данной реализации отбрасывает все не являющиеся GET запросы.
     * Все запросы, ответы, и типы ответов ассоциированных с запросами хранятся в списке responses.
     * В случае передачи потока бинарных данных, content-type http заголовка
     * определяется методом extractContentTypeByExtension().
     */
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (exchange.getRequestMethod().equals("GET")) {
            Map<String, String> values = responses.get(0);
            Map<String, String> types = responses.get(1);

            if (exchange.getRequestURI().toString().equals("/")) {
                writeTextMessage(exchange, "Greetings, Chosen One!");
            }
            else if (values.containsKey(exchange.getRequestURI().toString())) {
                if (types.get(exchange.getRequestURI().toString()).equals("inplace")) {
                    writeTextMessage(exchange, values.get(exchange.getRequestURI().toString()));
                }
                else if (types.get(exchange.getRequestURI().toString()).equals("binary")) {

                    Headers headers = exchange.getResponseHeaders();
                    File file = new File(values.get(exchange.getRequestURI().toString()));
                    headers.add("Content-Type", extractContentTypeByExtension(file.toString()));

                    byte[] byteArray = new byte[(int) file.length()];
                    FileInputStream fis = new FileInputStream(file);
                    BufferedInputStream in = new BufferedInputStream(fis);
                    in.read(byteArray, 0, byteArray.length);

                    exchange.sendResponseHeaders(200, file.length());
                    OutputStream out = exchange.getResponseBody();
                    out.write(byteArray, 0, byteArray.length);
                    out.close();
                    exchange.close();
                }
            }
            else {
                writeTextMessage(exchange, "Unknown resource.");
            }
        }
        else {
            writeTextMessage(exchange, "Unsupported request type. Only GET requests supported.");
        }
    }

    /**
     * Передает в тело http ответа строку из параметра message.
     */
    private void writeTextMessage(HttpExchange exchange, String message) throws IOException {
        exchange.sendResponseHeaders(200, 0);
        PrintWriter out = new PrintWriter(exchange.getResponseBody());
        out.print(message);
        out.close();
        exchange.close();
    }

    /**
     * Метод возвращает http "content-type" исходя из расширения файла.
     * На данный момент поддерживаются форматы:<br />
     * - документы: xml, pdf;<br />
     * - изображения: png, jpeg, jpg.
     *
     * @param path указывает путь либо имя файла.
     * @return возвращает content-type файла, например application/xml для
     *         xml документа.
     */
    String extractContentTypeByExtension(String path) {
        path = path.toLowerCase();

        switch (path.substring(path.lastIndexOf(".") + 1)) {
            case "xml":
                return "application/xml";
            case "png":
                return "image/png";
            case "jpg":
            case "jpeg":
                return "image/jpeg";
            case "pdf":
                return "application/pdf";
        }
        return null;
    }

    void setResponses(List<Map<String, String>> responses) {
        this.responses = responses;
    }
}
