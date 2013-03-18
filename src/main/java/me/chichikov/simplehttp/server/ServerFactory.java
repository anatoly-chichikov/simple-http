package me.chichikov.simplehttp.server;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Класс ServerFactory - служит для создания экземпляров SimpleHttpServer. На данный момент
 * поддерживается только с помощью xml файла конфигурации
 *
 * @author Anatoly Chichikov (12.03.2013)
 * @since 1.7
 */
public class ServerFactory {

    /**
     * Отображение предназначенное для хранения параметров конфигурации
     * итогового сервера.
     */
    private Map<ConfigurationParameters, Object> parameters = new HashMap<>(4);

    /**
     * Список, содержащий в себе два отображения (в обоих ключами являются запросы пользователей):<br />
     * - первое (с индексом 0) это непосредственно итоговые ответы сервера.
     * - второе (с индексом 1) это типы ответов.
     */
    private List<Map<String, String>> responses = new ArrayList<>();

    private DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    private Document document;
    private XPathFactory xpathFactory = XPathFactory.newInstance();
    private XPath xpath;
    private XPathExpression expression;

    /**
     * Позволяет получить экземпляр SimpleHttpServer, конфигурация которого определена в xml
     * файле путь к которому указан в path. В случае неудачи возвращает null.
     *
     * @param path абсолютный либо относительный (относительно директории с .jar файлом)
     *             путь к файлу настроек.
     * @return возвращает ссылку на сконфигурированный сервер (в случае ошибки возвращает null).
     */
    public SimpleHttpServer getServerByXML(String path) {
        SimpleHttpServer resultServer;
        try {
            resultServer = new SimpleHttpServer();
            parseXML(path);
            resultServer.initialize(new HashMap<>(parameters), new ArrayList<>(responses));
        }
        catch (SAXParseException | IllegalArgumentException e) {
            System.out.println("Invalid settings file: \"" + path + "\".");
            return null;
        }
        catch (IOException e) {
            System.out.println("Can't read file: \"" + path + "\".");
            return null;
        }
        catch (Exception e) {
            System.out.println("Unknown error.");
            return null;
        }
        return resultServer;
    }

    /**
     * Класс осуществляющий парсинг указанного в path .xml файла. Узвлечение значений
     * осуществляется посредством xPath запросов. Логика класса разделена на два метода: <br />
     * - метод осуществляющий разбор параметров сервера; <br />
     * - метод осуществляющий разбор ответов на запросы пользователя;
     *
     * @param path путь к ".xml" файлу, разбор которого необходимо осуществить.
     * @throws SAXException             возбуждается при некорректной структуре .xml файла.
     * @throws IllegalArgumentException возбуждается при несоответствии структуры .xml
     *                                  документа установленному шаблону.
     */
    void parseXML(String path) throws ParserConfigurationException, SAXException, IOException, XPathExpressionException {
        factory.setNamespaceAware(true);
        DocumentBuilder builder = factory.newDocumentBuilder();
        document = builder.parse(path);
        xpath = xpathFactory.newXPath();
        parseParameters();
        parseSourcesAndTypes();
    }

    /**
     * Извлекает из xml документа, параметры необходимые для инициализации сервера.
     *
     * @throws IllegalArgumentException возбуждается в случае отсутстствия необходимого тега,
     *                                  либо значения в нем.
     */
    private void parseParameters() throws XPathExpressionException {
        String stringResult;

        expression = xpath.compile("//connection/port/text()");
        stringResult = (String) expression.evaluate(document, XPathConstants.STRING);
        if (stringResult.equals("")) {
            throw new IllegalArgumentException();
        }
        parameters.put(ConfigurationParameters.PORT, Integer.parseInt(stringResult));

        expression = xpath.compile("//connection/auth/text()");
        stringResult = (String) expression.evaluate(document, XPathConstants.STRING);
        if (stringResult.equals("")) {
            throw new IllegalArgumentException();
        }
        parameters.put(ConfigurationParameters.IS_ANY_HAS_ACCESS, stringResult.equals("any"));

        expression = xpath.compile("//connection/user/text()");
        stringResult = (String) expression.evaluate(document, XPathConstants.STRING);
        if (stringResult.equals("")) {
            throw new IllegalArgumentException();
        }
        parameters.put(ConfigurationParameters.USER, stringResult);

        expression = xpath.compile("//connection/password/text()");
        stringResult = (String) expression.evaluate(document, XPathConstants.STRING);
        if (stringResult.equals("")) {
            throw new IllegalArgumentException();
        }
        parameters.put(ConfigurationParameters.PASSWORD, stringResult);
    }

    /**
     * Извлекает из xml документа, параметры ответов запросы клиентов.
     * В итоге формируются два отображения (ключом в обоих случаях является запрос):<br />
     * - в первом хранятся значения ответов;<br />
     * - во втором хранятся типы ответов.
     */
    private void parseSourcesAndTypes() throws XPathExpressionException {
        NodeList queryNodeList, typeNodeList, valueNodeList;

        expression = xpath.compile("//responses/response/query/text()");
        queryNodeList = (NodeList) expression.evaluate(document, XPathConstants.NODESET);

        expression = xpath.compile("//responses/response/type/text()");
        typeNodeList = (NodeList) expression.evaluate(document, XPathConstants.NODESET);

        expression = xpath.compile("//responses/response/value/text()");
        valueNodeList = (NodeList) expression.evaluate(document, XPathConstants.NODESET);

        if (!((queryNodeList.getLength() == typeNodeList.getLength()) &&
            (queryNodeList.getLength() == valueNodeList.getLength()))) {
            throw new IllegalArgumentException();
        }

        responses.add(0, new HashMap<String, String>());
        responses.add(1, new HashMap<String, String>());

        for (int i = 0; i < queryNodeList.getLength(); i++) {
            responses.get(0).put(queryNodeList.item(i).getNodeValue(), valueNodeList.item(i).getNodeValue());
            responses.get(1).put(queryNodeList.item(i).getNodeValue(), typeNodeList.item(i).getNodeValue());
        }
    }

    Map<ConfigurationParameters, Object> getParameters() {
        return parameters;
    }

    List<Map<String, String>> getResponses() {
        return responses;
    }
}
