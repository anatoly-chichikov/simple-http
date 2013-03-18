package me.chichikov.simplehttp.server;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * SimpleHttpHandlerTest - юнит тесты (JUnit 4.11) для логики класса SimpleHttpHandler
 *
 * @author Anatoly Chichikov (17.03.2013)
 * @since 1.7
 */
public class SimpleHttpHandlerTest {

    SimpleHttpHandler handler;

    @Before
    @Test
    public void initTests() {
        handler = new SimpleHttpHandler();

    }

    /**
     * Тест извлечения content-type
     */
    @Test
    public void testContentTypeExtracting() {
        Assert.assertEquals("application/xml", handler.extractContentTypeByExtension("/document.xml"));
    }
}
