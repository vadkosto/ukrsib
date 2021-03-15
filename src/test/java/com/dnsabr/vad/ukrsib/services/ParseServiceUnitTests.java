package com.dnsabr.vad.ukrsib.services;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import com.dnsabr.vad.ukrsib.models.Trans;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.slf4j.LoggerFactory;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.Assert;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Unit-тесты ParseService
 */
@RunWith(JUnit4.class)
public class ParseServiceUnitTests {

    private final ParseService parseService = new ParseService();
    private final StoreService store = new StoreService();

    /**
     * Метод для выполнения действий перед началом всех тестов класса
     * Чтение вывода в консоль используется некоторыми тестами
     */
    @BeforeClass
    public static void init() {
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        loggerContext.getLogger(ParseService.class).setLevel(Level.toLevel("INFO"));
    }

    /**
     * Метод для выполнения действий перед каждым тестом класса
     * Приведение состояния базы данных и необходимых для тестов полей сервисов в состояние как перед первым запуском
     */
    @Before
    public void setUp() {
        ReflectionTestUtils.invokeMethod(StoreService.class,"doTerminate");
        ReflectionTestUtils.setField(StoreService.class, "parserDone", false);
        ReflectionTestUtils.setField(StoreService.class, "terminated", false);
        ReflectionTestUtils.setField(StoreService.class, "countErrorsBeforeShutdown", new AtomicInteger(0));
        ((Map<Long,Integer>) ReflectionTestUtils.getField(Trans.class,"transactions")).clear();
        ReflectionTestUtils.setField(parseService, "store", store);
        ReflectionTestUtils.setField(store, "batchSize", 150);
        ReflectionTestUtils.setField(store, "batchAmount", 100);
         ReflectionTestUtils.setField(parseService,"doCheck",false);
    }

    /**
     * Метод для выполнения действий после каждого теста класса
     */
    @After
    public void tearDown() {
        ReflectionTestUtils.invokeMethod(StoreService.class,"doTerminate");
        ReflectionTestUtils.setField(StoreService.class, "parserDone", false);
        ReflectionTestUtils.setField(StoreService.class, "terminated", false);
        ReflectionTestUtils.setField(StoreService.class, "countErrorsBeforeShutdown", new AtomicInteger(0));
        ((Map<Long,Integer>) ReflectionTestUtils.getField(Trans.class,"transactions")).clear();
        ReflectionTestUtils.setField(parseService, "store", store);
        ReflectionTestUtils.setField(store, "batchSize", 150);
        ReflectionTestUtils.setField(store, "batchAmount", 100);
        ReflectionTestUtils.setField(parseService,"doCheck",false);
    }

    /**
     * Тест работы парсера в режиме предварительной проверки с файлом содержащим ошибки
     * Переопределяет системный вывод для чтения сообщений ParseService
     * Если ParseService обрабатывает файл содержащий недостатки с его предварительной проверкой, то закончив проверку
     * прекратит работу и остановит приложение
     */
    @Test
    public void parseFileWithErrorsWithPreCheckTest() {

        ReflectionTestUtils.setField(parseService,"doCheck",true);
        ReflectionTestUtils.setField(parseService,"fileName","Java_test_nulls.xml");

        final PrintStream standardOut = System.out;
        final ByteArrayOutputStream outputStreamCaptor = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outputStreamCaptor));

        ReflectionTestUtils.invokeMethod(parseService,"run");

        System.setOut(standardOut);
        String logs = outputStreamCaptor.toString();
        System.out.println(logs);
        boolean gotMessageExpected = logs.contains("не прошел проверку");

        Assert.isTrue(gotMessageExpected
                ,"ParseService не нашел ошибки в файле их содержащем: Java_test_nulls.xml");

    }

    /**
     * Тест работы парсера без предварительной проверки с файлом содержащим ошибки
     * Переопределяет системный вывод для чтения сообщений ParseService
     * Если ParseService обрабатывает файл содержащий недостатки без его предварительной проверки, то после первой
     * ошибки прекратит работу и остановит приложение
     */
    @Test
    public void parseFileWithErrorsNoPreCheckTest() {
        ReflectionTestUtils.setField(parseService,"doCheck",false);
        ReflectionTestUtils.setField(parseService,"fileName","Java_test_nulls.xml");

        final PrintStream standardOut = System.out;
        final ByteArrayOutputStream outputStreamCaptor = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outputStreamCaptor));

        ReflectionTestUtils.invokeMethod(parseService,"run");

        System.setOut(standardOut);
        String logs = outputStreamCaptor.toString();
        System.out.println(logs);
        boolean gotMessageExpected = logs.contains("spring.jpa.properties.app.parser.errors.check=true");

        Assert.isTrue(gotMessageExpected
                ,"ParseService не нашел ошибки в файле их содержащем: Java_test_nulls.xml");
    }

    /**
     * Тест работы парсера с несуществующим файлом
     * Переопределяет системный вывод для чтения сообщений ParseService
     * Если ParseService обрабатывает несуществующий файл, то прекратит работу и остановит приложение
     */
    @Test
    public void parseFileDoesNotExistsTest() {
        ReflectionTestUtils.setField(parseService,"fileName","k:\\srg5afs565awffd43.chfggdf5tb4");

        final PrintStream standardOut = System.out;
        final ByteArrayOutputStream outputStreamCaptor = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outputStreamCaptor));

        ReflectionTestUtils.invokeMethod(parseService,"run");

        System.setOut(standardOut);
        String logs = outputStreamCaptor.toString();
        System.out.println(logs);
        boolean gotMessageExpected = logs.contains("Проверьте наличие файла");

        Assert.isTrue(gotMessageExpected
                ,"ParseService не завершился с ошибкой об отсутствии несуществующего файла");
    }

    /**
     * Тест работы парсера без предварительной проверки с файлом не содержащим ошибки
     * Переопределяет системный вывод для чтения сообщений ParseService
     * Если ParseService обрабатывает файл не содержащий недостатки, то заканчивает работу после полной обработки файла
     */
    @Test
    public void parseFileWithoutErrorsTest() {

        ReflectionTestUtils.setField(parseService,"fileName","Java_test.xml");
        final PrintStream standardOut = System.out;
        final ByteArrayOutputStream outputStreamCaptor = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outputStreamCaptor));

        ReflectionTestUtils.invokeMethod(parseService,"run");

        System.setOut(standardOut);
        String logs = outputStreamCaptor.toString();
        System.out.println(logs);
        boolean gotMessageExpected = logs.contains("Завершил работу сервис разбора");

        Assert.isTrue(gotMessageExpected
                ,"ParseService нашел ошибки в файле их не содержащем: Java_test.xml");
    }

    /**
     * Тест метода isDoCheck
     * Когда запрашиваем режим работы парсера, то получаем правильное состояние
     */
    @Test
    public void isDoCheckTest() {

        ReflectionTestUtils.setField(parseService,"doCheck",false);
        boolean actual = parseService.isDoCheck();
        Assert.isTrue(!actual
                ,"Метод ParseService.isDoCheck вернул неправильное значение\nожидаемое false\nактуальное:"+actual);

        ReflectionTestUtils.setField(parseService,"doCheck",true);
        actual = parseService.isDoCheck();
        Assert.isTrue(actual
                ,"Метод ParseService.isDoCheck вернул неправильное значение\nожидаемое true\nактуальное:"+actual);
    }
}

/*
 * Структура входящего тестового XML-файла
 *
 * <soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
 *  <soap:Body>
 *   <ns2:GetTransactionsResponse xmlns:ns2="http://dbo.qulix.com/ukrsibdbo">
 *    <transactions>
 *     <transaction>
 *      <place>A PLACE 1</place>
 *      <amount>10.01</amount>
 *      <currency>UAH</currency>
 *      <card>123456****1234</card>
 *      <client>
 *       <firstName>Ivan</firstName>
 *       <lastName>Ivanoff</lastName>
 *       <middleName>Ivanoff</middleName>
 *       <inn>1234567890</inn>
 *      </client>
 *     </transaction>
 *     <transaction>
 *       ........
 *     </transaction>
 *    </transactions>
 *   </ns2:GetTransactionsResponse>
 *  </soap:Body>
 * </soap:Envelope>
 */