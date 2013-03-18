package me.chichikov.simplehttp.server;

import com.sun.net.httpserver.*;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map;


/**
 * SimpleHttpServer - простой Http сервер. Позволяет обрабатывать только GET запросы, в ответ выдает
 * сконфигурированные заранее документы. Запуск осуществляется посредством метода start(), остановка
 * методом stop().
 *
 * @author Anatoly Chichikov (12.03.2013)
 * @since 1.7
 */
public class SimpleHttpServer {

    private HttpServer server;
    private boolean isAnyHasAccess = false;
    private boolean isInitialized = false;
    private boolean isRunning = false;
    private String user;
    private String password;
    private int port = 0;

    /**
     * Список, содержащий в себе два отображения (в обоих ключами являются запросы пользователей):<br />
     * - первое (с индексом 0) это непосредственно итоговые ответы сервера.
     * - второе (с индексом 1) это типы ответов.
     */
    private List<Map<String, String>> responses;

    BasicAuthenticator authenticator = new SimpleServerAuthenticator("simpleServerRealm");

    /**
     * Класс осуществлющий аутентификацию клиентов SimpleHttpServer.
     * Инициализируется в методе start.
     */
    class SimpleServerAuthenticator extends BasicAuthenticator {
        public SimpleServerAuthenticator(String s) {
            super(s);
        }

        /**
         * Непосредственно логика аутентификации.
         *
         * @param user     имя пользователя введенное клиентом.
         * @param password пароль введенный клиентом.
         * @return возвращает true в случае успеха, false в случае неудачи.
         */
        @Override
        public boolean checkCredentials(String user, String password) {
            return user.equals(getUser()) && password.equals(getPassword());
        }
    }

    /**
     * Метод проверяющий наличие и корректность типов параметров в отображении.
     *
     * @param parameters отображение передающееся по цепочке из метода initialize().
     * @throws IllegalArgumentException выбрасывается при несоответствии типов.
     */
    private void checkCorrectParameters(Map<ConfigurationParameters, Object> parameters) throws IllegalArgumentException {
        if (!(parameters.containsKey(ConfigurationParameters.PORT) &&
            parameters.containsKey(ConfigurationParameters.IS_ANY_HAS_ACCESS))) {
            throwException(SimpleServerException.ILLEGAL_ARGUMENT);
        }
        if (parameters.get(ConfigurationParameters.PORT).getClass() != Integer.class) {
            throwException(SimpleServerException.ILLEGAL_ARGUMENT);
        }
        if (parameters.get(ConfigurationParameters.IS_ANY_HAS_ACCESS).getClass() != Boolean.class) {
            throwException(SimpleServerException.ILLEGAL_ARGUMENT);
        }
        if (!isAnyHasAccess) {
            if ((parameters.get(ConfigurationParameters.USER).getClass() != String.class) || (parameters.get(ConfigurationParameters.PASSWORD).getClass() != String.class)) {
                throwException(SimpleServerException.ILLEGAL_ARGUMENT);
            }
        }
    }

    /**
     * Метод инициализирующий сервер. Данные для инициализации передаются в виде отображения
     * с ключом-перечислением ConfigurationParameters. В случае если isAnyAccess равно true
     * пропускается инициализация логина и пароля.
     *
     * @param parameters отображение содержащее в себе необходимые для инициализации сервера параметры.
     *                   Должно содержать в себе значения:<br />
     *                   - объект Boolean с ключом IS_ANY_HAS_ACCESS;<br />
     *                   - объект Integer с ключом PORT;<br />
     *                   - два объекта String с ключами USER и PASSWORD.
     * @param responses  список с отображениями для инициализации this.responses.
     * @throws IllegalArgumentException в случае отсутствия либо несоответствия типа параметра
     *                                  в отображении parameters.
     */
    void initialize(Map<ConfigurationParameters, Object> parameters, List<Map<String, String>> responses) throws IllegalArgumentException {
        this.responses = responses;
        checkCorrectParameters(parameters);
        port = (Integer) parameters.get(ConfigurationParameters.PORT);
        if ((port > 65536) || (port < 1025)) {
            throwException(SimpleServerException.ILLEGAL_ARGUMENT);
        }
        isAnyHasAccess = (Boolean) parameters.get(ConfigurationParameters.IS_ANY_HAS_ACCESS);
        if (!isAnyHasAccess) {
            password = (String) parameters.get(ConfigurationParameters.PASSWORD);
            user = (String) parameters.get(ConfigurationParameters.USER);
        }
        isInitialized = true;
    }

    /**
     * Метод возбуждающий специфичные для класса исключения. Принимает параметр-перечисление
     * SimpleServerException, определенное в пакете me.chichikov.simplehttp.server.
     *
     * @param exception перечисление характеризующее произошедшую ошибку.
     */
    private void throwException(SimpleServerException exception) {
        switch (exception) {
            case UNSUPPORTED_OPERATION:
                throw new UnsupportedOperationException();
            case ILLEGAL_ARGUMENT:
                throw new IllegalArgumentException();
        }
    }

    /**
     * Метод запускающий сервер. Производится инициализация обработчика запросов и в
     * случае ограниченного доступа инициализируется экземпляр внутреннего класса
     * SimpleServerAuthenticator.
     *
     * @throws UnsupportedOperationException в случае если сервер не инициализирован.
     */
    public void start() throws IOException, UnsupportedOperationException {
        if (isInitialized) {
            if (!isRunning) {
                SimpleHttpHandler handler = new SimpleHttpHandler();
                handler.setResponses(responses);
                server = HttpServer.create(new InetSocketAddress(port), 0);
                HttpContext context = server.createContext("/", handler);
                if (!isAnyHasAccess) {
                    context.setAuthenticator(authenticator);
                }
                server.start();
                isRunning = true;
            }
        }
        else {
            throwException(SimpleServerException.UNSUPPORTED_OPERATION);
        }
    }

    /**
     * Метод останавливающий работающий сервер.
     *
     * @throws UnsupportedOperationException в случае если сервер уже запущен.
     */
    public void stop() throws UnsupportedOperationException {
        if (isInitialized && isRunning) {
            server.stop(0);
        }
        else {
            throwException(SimpleServerException.UNSUPPORTED_OPERATION);
        }
    }

    @Override
    public String toString() {
        return "Server state:" +
            "\n- initialization " + (isInitialized() ? "performed;" : "not performed;") +
            "\n- start " + (isRunning() ? "performed;" : "not performed;") +
            "\n- access for all users " + (isAnyHasAccess() ? "allowed;" : "denied;") +
            (isInitialized() ? ("\n- port listening: " + port + ";") : ("\n- no port available;"));
    }

    SimpleHttpServer() {
        super();
    }

    String getPassword() {
        return password;
    }

    int getPort() {
        return port;
    }

    String getUser() {
        return user;
    }

    boolean isAnyHasAccess() {
        return isAnyHasAccess;
    }

    public boolean isInitialized() {
        return isInitialized;
    }

    public boolean isRunning() {
        return isRunning;
    }
}