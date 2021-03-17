package com.dnsabr.vad.ukrsib.systemtests;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import com.dnsabr.vad.ukrsib.models.*;
import com.dnsabr.vad.ukrsib.repository.*;
import com.dnsabr.vad.ukrsib.services.*;
import com.dnsabr.vad.ukrsib.utils.Utils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.Commit;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Системные тесты работы приложения
 */
@RunWith(SpringRunner.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class SystemTests {

    @Autowired
    private ClientRepository clientRepository;
    @Autowired
    private PlaceRepository placeRepository;
    @Autowired
    private TransRepository transRepository;
    @Autowired
    private StoreService store;
    @Autowired
    private MainService mainService;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private CacheManager cacheManager;
    @PersistenceContext
    private EntityManager entityManager;
    @Autowired
    private ParseService parseService;

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
    }

    /**
     * Метод для выполнения действий после каждого теста класса
     * Приведение состояния полей сервисов в состояние как перед первым запуском
     */
    @After
    public void tearDown() {
        ReflectionTestUtils.invokeMethod(StoreService.class,"doTerminate");
        ReflectionTestUtils.setField(StoreService.class, "parserDone", false);
        ReflectionTestUtils.setField(StoreService.class, "terminated", false);
        ReflectionTestUtils.setField(StoreService.class, "countErrorsBeforeShutdown", new AtomicInteger(0));
        ((Map<Long,Integer>) ReflectionTestUtils.getField(Trans.class,"transactions")).clear();
    }

    /**
     * Сравнивает данные, которые содержатся во входящем XML-файле с данными полученными из базы данных после
     *  завершения работы приложения.
     * Для этого сначала формирует пары списков транзакций, клиентов и мест транзакций
     * Actual в названии списка обозначает данные полученные из базы данных.
     * Эти данные берет с помощью обращения к стандартным методам findAll соответствующего репозитария
     * Expected - данные внесенные вручную во вспомогательных методах тестового пакета
     * com.dnsabr.vad.ukrsib.utils.Utils (обращение к методу fillLists)
     * Выполняет 2 вида сопоставлений для уверенности в том, что данные идентичны
     *  1. Сравнение размера соответствующих списков попарно
     *  2. Подробное сравнение данных в списках (в циклах для каждой пары списков)
     * Данные сравниваются с помощью методов deepEquals соответствующих объектов классов Client, Place, Trans
     *  с учетом связанности объектов (в методе deepEquals класса Trans проверяется идентичность
     *  связанных с объектом класса Trans объектов классов Client и Place)
     *
     *  Если запустить приложение, то все и не более транзакции, клиенты и места будут добавлены в БД
     */
    @Test
    @Transactional
    public void dataAccuracyTest() {

        // Запуск основного приложения
        mainService.start();

        // Очистка кэш 2-го уровня и entityManager
        cacheManager.getCacheNames().forEach(cache->cacheManager.getCache(cache).clear());
        entityManager.clear();
        try {
            TimeUnit.SECONDS.sleep(4);
        } catch (InterruptedException e) {/*пустое*/}

        List<Client> clientsActual = clientRepository.findAll();
        List<Place> placesActual = placeRepository.findAll();
        List<Trans> transActual = transRepository.findAll();

        List<Client> clientsExpected = new ArrayList<>();
        List<Place> placesExpected = new ArrayList<>();
        List<Trans> transExpected = new ArrayList<>();
        Utils.fillLists(clientsExpected, placesExpected, transExpected,3000);

        Assert.isTrue(clientsExpected.size()==clientsActual.size()
                ,"Количество клиентов не совпадают\nожидаемое:" + clientsExpected.size() +
                        "\nактуальное: "+ clientsActual.size());
        Assert.isTrue(placesExpected.size()==placesActual.size()
                ,"Количество мест не совпадают\nожидаемое:" + placesExpected.size() +
                "\nактуальное: "+ placesActual.size());
        Assert.isTrue(transExpected.size()==transActual.size()
                ,"Количество транзакций не совпадают\nожидаемое:" + transExpected.size() +
                "\nактуальное: "+ transActual.size());

        for (int i = 0; i<clientsExpected.size(); i++) {
            Assert.isTrue(clientsExpected.get(i).deepEquals(clientsActual.get(i))
                    , "Данные клиентов не совпадают!\nожидаемые:"
                            + clientsExpected.get(i) + "\nактуальные: " + clientsActual.get(i));
        }
        for (int i = 0; i<placesExpected.size(); i++) {
            Assert.isTrue(placesExpected.get(i).deepEquals(placesActual.get(i))
                    ,"Данные мест не совпадают!\nожидаемые:"
                            +placesExpected.get(i)+"\nактуальные: "+placesActual.get(i));
        }
        for (int i = 0; i<transExpected.size(); i++) {
            Assert.isTrue(transExpected.get(i).deepEquals(transActual.get(i))
                    ,"Данные транзакций не совпадают!\nожидаемые:"
                            +transExpected.get(i)+"\nактуальные: "+transActual.get(i));
        }
    }

    /**
     * Тест запуска сервисов и принудительной остановки всех сервисов
     * Когда запускается MainService, запускаются ParseService и как минимум один поток SaveService
     * Когда приложение останавливается принудительно, ParseService не успевает обработать весь файл и в БД не добавлены
     * все транзакции и соответствующие сообщения о принудительной остановке выводятся в консоль
     */
    @Test
    public void servicesStartsAndStopedOnDemandTest() {

        ReflectionTestUtils.setField(parseService,"fileName","Java_test_12000.xml");

        // Установка уровня логирования
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        loggerContext.getLogger(MainService.class).setLevel(Level.toLevel("INFO"));
        loggerContext.getLogger(ParseService.class).setLevel(Level.toLevel("INFO"));
        loggerContext.getLogger(SaveService.class).setLevel(Level.toLevel("INFO"));

        // В отдельном потоке инициируем остановку приложения через несколько секунд
        (new Thread(() -> {
            try {
                TimeUnit.MILLISECONDS.sleep(4000);
            } catch (InterruptedException e) {/*пустое*/}
            // Останавливаем хранилище - приложение должно завершиться
            ReflectionTestUtils.invokeMethod(store,"doTerminate");
        })).start();

        // Переопределяем системный вывод для чтения сообщений сервисов
        final PrintStream standardOut = System.out;
        final ByteArrayOutputStream outputStreamCaptor = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outputStreamCaptor));

        // Запускаем основное приложение
        mainService.start();

        // Возврат вывода в консоль
        System.setOut(standardOut);
        String logs = outputStreamCaptor.toString();
        System.out.println(logs);

        // Если MainService запущен, он выводит сообщение
        boolean gotMessageMainServiceStart = logs.contains("Начало работы приложения");
        Assert.isTrue(gotMessageMainServiceStart,"Приложение не запущено");

        // Если ParseService запущен, он выводит сообщение
        boolean gotMessageParseServiceStart = logs.contains("Запущен сервис разбора входящего XML-файла");
        Assert.isTrue(gotMessageParseServiceStart,"Сервис разбора входящего XML-файла не запущен");

        // Если SaveService запущен, он выводит сообщение
        boolean gotMessageSaveServiceStart = logs.contains("Запущен новый поток сервиса сохранения данных в БД");
        Assert.isTrue(gotMessageSaveServiceStart,"Ни один поток сервиса сохранения данных в БД не запущен");


        int parserCountExpected = 162000;
        int parserCountActual = ((Map<Long,Integer>) ReflectionTestUtils.getField(Trans.class,"transactions")).size();
        long amountOfTransactionsExpected = 162000;
        long amountOfTransactionsActual = jdbcTemplate.queryForObject("select count(*) from transactions;", Long.class);


        // Если приложение было остановлено принудительно, то хранилище закрыто, парсер прочитал меньше транзакций, чем
        // ожидалось, в БД добавлено меньше транзакций, чем ожидалось и сервисы вывели соответствующие сообщения

        boolean gotMessageParseServiceFinish = logs.contains("Принудительно остановлен сервис разбора входящего XML-файла");
        boolean gotMessageSaveServiceFinish = logs.contains("Принудительно остановлен поток сервиса сохранения данных в БД");
        boolean gotMessageMainServiceFinish = logs.contains("Принудительно остановлено приложение");

        Assert.isTrue(store.isTerminated() && parserCountActual<parserCountExpected
                        && amountOfTransactionsActual<amountOfTransactionsExpected &&
                        (gotMessageParseServiceFinish && gotMessageSaveServiceFinish && gotMessageMainServiceFinish)
                ,"Приложение не было остановлено принудительно, но могло успеть отработать на мощном оборудовании");
    }
}
