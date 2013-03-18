package me.chichikov.simplehttp.server;

import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * SimpleHttpServerTest - юнит тесты (JUnit 4.11) для логики класса SimpleHttpServer.
 *
 * @author Anatoly Chichikov (12.03.2013)
 * @since 1.7
 */
public class SimpleHttpServerTest {

    Map<ConfigurationParameters, Object> parameters;
    List<Map<String, String>> responses;
    SimpleHttpServer server;

    @Before
    @Test
    public void initTests() {
        parameters = new HashMap<>(4);
        server = new SimpleHttpServer();
        parameters.put(ConfigurationParameters.PASSWORD, "password");
        parameters.put(ConfigurationParameters.USER, "user");
        parameters.put(ConfigurationParameters.PORT, 5003);
        responses = new ArrayList<>();
    }

    /**
     * Инициализация с активным общим доступом
     */
    @Test
    public void testInitializationWithAnyAccess() {
        parameters.put(ConfigurationParameters.IS_ANY_HAS_ACCESS, true);
        server.initialize(parameters, responses);
        assertEquals(5003, server.getPort());
        assertTrue(server.isAnyHasAccess());
        assertNull(server.getPassword());
        assertNull(server.getUser());
    }

    /**
     * Проверка инициализации паролей с неактивным общим доступом
     */
    @Test
    public void testInitializationWithoutAnyAccess() {
        parameters.put(ConfigurationParameters.IS_ANY_HAS_ACCESS, false);
        server.initialize(parameters, responses);
        assertFalse(server.isAnyHasAccess());
        assertEquals("password", server.getPassword());
        assertEquals("user", server.getUser());
    }

    /**
     * Некорректное выражение разрешения общего доступа в отображении (тип не Boolean)
     */
    @Test(expected = IllegalArgumentException.class)
    public void testWrongIsAnyHasAccessInitialization() {
        parameters.put(ConfigurationParameters.IS_ANY_HAS_ACCESS, 12);
        server.initialize(parameters, responses);
    }

    /**
     * Некорректно задан порт в отображении (тип не Integer или выходит за рамки 1025 .. 65535)
     */
    @Test(expected = IllegalArgumentException.class)
    public void testWrongPortInitialization() {
        parameters.put(ConfigurationParameters.IS_ANY_HAS_ACCESS, false);
        parameters.remove(ConfigurationParameters.PORT);
        parameters.put(ConfigurationParameters.PORT, -5.9);
        server.initialize(parameters, responses);
    }

    /**
     * Некорректное имя пользователя в отображении (тип не String)
     */
    @Test(expected = IllegalArgumentException.class)
    public void testIllegalUsernameInitialization() {
        parameters.put(ConfigurationParameters.IS_ANY_HAS_ACCESS, false);
        parameters.remove(ConfigurationParameters.USER);
        parameters.put(ConfigurationParameters.USER, 123);
        server.initialize(parameters, responses);
    }

    /**
     * Некорректный пароль в отображении (тип не String)
     */
    @Test(expected = IllegalArgumentException.class)
    public void testIllegalPasswordInitialization() {
        parameters.put(ConfigurationParameters.IS_ANY_HAS_ACCESS, false);
        parameters.remove(ConfigurationParameters.PASSWORD);
        parameters.put(ConfigurationParameters.PASSWORD, 123);
        server.initialize(parameters, responses);
    }

    /**
     * Некорректное число параметров в отображении (не добавлен IS_ANY_HAS_ACCESS)
     */
    @Test(expected = IllegalArgumentException.class)
    public void testNoArgumentsInitialization() {
        server.initialize(parameters, responses);
    }

    /**
     * Корректный старт сервера
     */
    @Test
    public void testCorrectStart() throws IOException {
        parameters.put(ConfigurationParameters.IS_ANY_HAS_ACCESS, true);
        server.initialize(parameters, responses);
        server.start();
        assertTrue(server.isRunning());
        server.stop();
    }

    /**
     * Тестирование ответа на запрос страницы по умолчанию
     */
    @Test
    public void normalResponseWithAnyAccessTest() throws IOException {
        server = new ServerFactory().getServerByXML("target/test-resources/settings.xml");
        server.start();

        try {
            URL url = new URL("http://localhost:5003/");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            StringBuilder allPage = new StringBuilder();

            try (InputStreamReader in = new InputStreamReader(conn.getInputStream())) {
                int n;
                while ((n = in.read()) != -1) {
                    allPage.append((char) n);
                }
            }
            assertEquals("Greetings, Chosen One!", allPage.toString());
        }
        finally {
            server.stop();
        }
    }

    /**
     * Тестирование ответа на POST запрос
     */
    @Test
    public void postResponseTest() throws IOException {
        server = new ServerFactory().getServerByXML("target/test-resources/settings.xml");
        server.start();

        try {
            URL url = new URL("http://localhost:5003/");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            StringBuilder allPage = new StringBuilder();

            try (InputStreamReader in = new InputStreamReader(conn.getInputStream())) {
                int n;
                while ((n = in.read()) != -1) {
                    allPage.append((char) n);
                }
            }
            assertEquals("Unsupported request type. Only GET requests supported.", allPage.toString());
        }
        finally {
            server.stop();
        }
    }

    /**
     * Тестирование ответа на неизвестный запрос
     */
    @Test
    public void unknownResponseTest() throws IOException {
        server = new ServerFactory().getServerByXML("target/test-resources/settings.xml");
        server.start();

        try {
            URL url = new URL("http://localhost:5003/unknown");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();

            StringBuilder allPage = new StringBuilder();
            try (InputStreamReader in = new InputStreamReader(conn.getInputStream())) {
                int n;
                while ((n = in.read()) != -1) {
                    allPage.append((char) n);
                }
            }
            assertEquals("Unknown resource.", allPage.toString());
        }
        finally {
            server.stop();
        }
    }

    /**
     * Тестирование ответа на запрос /text
     */
    @Test
    public void textResponseTest() throws IOException {
        server = new ServerFactory().getServerByXML("target/test-resources/settings.xml");
        server.start();

        URL url = new URL("http://localhost:5003/text");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        try (InputStreamReader in = new InputStreamReader(conn.getInputStream())) {
            StringBuilder allPage = new StringBuilder();
            int n;
            while ((n = in.read()) != -1) {
                allPage.append((char) n);
            }
            assertEquals("<text>text</text>", allPage.toString());
        }
        finally {
            server.stop();
        }
    }

    /**
     * Тестирование ответа на запрос /xml1
     */
    @Test
    public void xmlResponseTest() throws IOException {
        server = new ServerFactory().getServerByXML("target/test-resources/settings.xml");
        server.start();

        URL url = new URL("http://localhost:5003/xml1");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        try (InputStreamReader in = new InputStreamReader(conn.getInputStream())) {
            StringBuilder allPage = new StringBuilder();
            int n;
            while ((n = in.read()) != -1) {
                allPage.append((char) n);
            }
            assertEquals("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                "<somedata>\n" +
                "    <somesubdata>\n" +
                "        content\n" +
                "    </somesubdata>\n" +
                "</somedata>", allPage.toString());
        }
        finally {
            server.stop();
        }
    }

    /**
     * Тестирование ответа на запрос с ограниченным доступом
     */
    @Test
    public void privateResponseTest() throws IOException {
        server = new ServerFactory().getServerByXML("target/test-resources/settings-private.xml");
        server.start();

        Authenticator.setDefault(new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                if (getRequestingScheme().equalsIgnoreCase("basic")) {
                    return new PasswordAuthentication("user", "password".toCharArray());
                }
                return null;
            }
        });

        URL url = new URL("http://localhost:5003/xml1");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        try (InputStreamReader in = new InputStreamReader(conn.getInputStream())) {
            in.read();
        }
        finally {
            server.stop();
        }

    }

    /**
     * Тестирование ответа на запрос с ограниченным доступом (неверные данные аутентификации)
     */
    @Test(expected = ProtocolException.class)
    public void wrongPasswordPrivateResponseTest() throws IOException {
        server = new ServerFactory().getServerByXML("target/test-resources/settings-private.xml");
        server.start();

        Authenticator.setDefault(new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                if (getRequestingScheme().equalsIgnoreCase("basic")) {
                    return new PasswordAuthentication("us", "pass".toCharArray());
                }
                return null;
            }
        });

        URL url = new URL("http://localhost:5003/xml1");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        try (InputStreamReader in = new InputStreamReader(conn.getInputStream())) {
            in.read();
        }
        finally {
            server.stop();
        }

    }

    /**
     * Двойной вызов метода start()
     */
    @Test
    public void testDoubleStart() throws IOException {
        parameters.put(ConfigurationParameters.IS_ANY_HAS_ACCESS, true);
        server.initialize(parameters, responses);
        server.start();
        server.start();
        assertTrue(server.isRunning());
        assertTrue(server.isInitialized());
        assertTrue(server.isAnyHasAccess());
        assertEquals(5003, server.getPort());
        assertNull(server.getPassword());
        assertNull(server.getUser());
        server.stop();
    }

    /**
     * Неинициализированный старт сервера
     */
    @Test(expected = UnsupportedOperationException.class)
    public void testNonInitializedStart() throws IOException {
        server.start();
    }

    /**
     * Остановка неинициализированного сервера
     */
    @Test(expected = UnsupportedOperationException.class)
    public void testNonInitializedStop() throws IOException {
        server.stop();
    }

    /**
     * Остановка инициализированного но не активного сервера
     */
    @Test(expected = UnsupportedOperationException.class)
    public void testInitializedStopWithoutStart() {
        parameters.put(ConfigurationParameters.IS_ANY_HAS_ACCESS, true);
        server.initialize(parameters, responses);
        server.stop();
    }
}
