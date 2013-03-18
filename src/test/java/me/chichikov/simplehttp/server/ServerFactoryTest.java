package me.chichikov.simplehttp.server;

import org.junit.Before;
import org.junit.Test;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * ServerFactoryTest - юнит тесты (JUnit 4.11) для логики класса ServerFactory
 *
 * @author Anatoly Chichikov (12.03.2013)
 * @since 1.7
 */
public class ServerFactoryTest {

    SimpleHttpServer server;
    ServerFactory factory;
    Map<ConfigurationParameters, Object> parameters;
    List<Map<String, String>> testSourceList;

    @Before
    @Test
    public void initTests() {
        factory = new ServerFactory();
        server = factory.getServerByXML("target/test-resources/settings.xml");
    }

    /**
     * Тест правильной инициализации сервера
     */
    @Test
    public void genericServerInitialization() {
        assertNotNull(server);
        assertEquals(5003, server.getPort());
        assertEquals(null, server.getUser());
        assertEquals(null, server.getPassword());
        assertEquals(true, server.isAnyHasAccess());
    }

    /**
     * Тест парсинга настроек
     */
    @Test
    public void defaultXMLParsing() throws IOException, SAXException, ParserConfigurationException, XPathExpressionException {
        factory.parseXML("target/test-resources/settings.xml");
        parameters = factory.getParameters();
        assertEquals(5003, parameters.get(ConfigurationParameters.PORT));
        assertTrue((Boolean) parameters.get(ConfigurationParameters.IS_ANY_HAS_ACCESS));
        assertEquals("password", parameters.get(ConfigurationParameters.PASSWORD));
        assertEquals("user", parameters.get(ConfigurationParameters.USER));
    }

    /**
     * Тест поврежденного .xml файла
     */
    @Test(expected = SAXParseException.class)
    public void brokenXMLParsing() throws IOException, SAXException, ParserConfigurationException, XPathExpressionException {
        factory.parseXML("target/test-resources/settings-broken.xml");
    }

    /**
     * Тест несуществующего .xml файла
     */
    @Test(expected = IOException.class)
    public void unavailableFileXMLParsing() throws ParserConfigurationException, SAXException, XPathExpressionException, IOException {
        factory.parseXML("target/test-resources/no-file.xml");
    }

    /**
     * Тест .xml файла с отсутствующим тегом (port)
     */
    @Test(expected = IllegalArgumentException.class)
    public void withoutTagXMLParsing() throws ParserConfigurationException, SAXException, XPathExpressionException, IOException {
        factory.parseXML("target/test-resources/settings-without-tags.xml");
    }

    /**
     * Тест файла с неполным набором под-тегов response
     */
    @Test(expected = IllegalArgumentException.class)
    public void incompleteResponseXMLParsing() throws ParserConfigurationException, SAXException, XPathExpressionException, IOException {
        factory.parseXML("target/test-resources/settings-with-incomplete-response.xml");
    }

    /**
     * Тест
     */
    @Test
    public void sourcesAndTypesListGenericTest() {
        testSourceList = factory.getResponses();
        assertEquals(2, testSourceList.size());
        assertEquals("target/test-resources/testdata/xml1.xml", testSourceList.get(0).get("/xml1"));
        assertEquals("target/test-resources/testdata/image1.png", testSourceList.get(0).get("/image1"));
        assertEquals("<text>text</text>", testSourceList.get(0).get("/text"));
        assertEquals("binary", testSourceList.get(1).get("/xml1"));
        assertEquals("binary", testSourceList.get(1).get("/image1"));
        assertEquals("inplace", testSourceList.get(1).get("/text"));
    }
}

