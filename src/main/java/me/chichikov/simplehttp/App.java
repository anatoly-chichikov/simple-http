package me.chichikov.simplehttp;

import me.chichikov.simplehttp.server.ServerFactory;
import me.chichikov.simplehttp.server.SimpleHttpServer;

import java.io.IOException;

/**
 * Класс App - точка входа в программу, инициализация и запуск
 * сервера.
 *
 * @author Anatoly Chichikov (12.03.2013)
 * @since 1.7
 */
public class App {
    public static void main(String[] args) throws IOException {
        SimpleHttpServer server = new ServerFactory().getServerByXML("settings.xml");
        if (server != null) {
            server.start();
            System.out.println(server);
            System.out.println("Press Enter to stop.");
            System.in.read(); // нужно заменить
            server.stop();
            System.out.println("Server was stopped.");
        }
        else {
            System.out.println("Can't initialize server.");
        }
    }
}
