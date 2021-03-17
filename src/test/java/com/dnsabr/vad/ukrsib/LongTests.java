package com.dnsabr.vad.ukrsib;

import com.dnsabr.vad.ukrsib.models.*;
import com.dnsabr.vad.ukrsib.repository.*;
import com.dnsabr.vad.ukrsib.services.*;
import org.hibernate.Session;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.Commit;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.StopWatch;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Тесты, которые могут занимать продолжительное время из-за возможного большого объема данных
 * Не обязательны для запуска, так как не тестируют ничего, что не включено в основные тесты
 * Исключены из всех TestsSuites
 */
@RunWith(SpringRunner.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@SpringBootTest
@ActiveProfiles({"long"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class LongTests {

    // Установите значения количества транзакций, клиентов и мест для автогенерации
    private final static int amountOfTransactionsExpected = 163051;
    private final static int amountOfClientsExpected = 10000;
    private final static int amountOfPlacesExpected = 100;
    // ----------------------------------------------------------------------------

    @Value("${spring.jpa.properties.app.sql.threads}")
    private int amountOfThreads;
    @Autowired
    private SaveService saveService;
    @Autowired
    TransService transService;
    @Autowired
    private StoreService store;
    @Autowired
    private TransRepository transRepository;
    @Autowired
    private ClientRepository clientRepository;
    @PersistenceContext
    private EntityManager entityManager;
    @Autowired
    private QueryService queryService;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private CacheManager cacheManager;

    private final Map<Long,Trans> transactionsExpected = new HashMap<>();
    private final Map<String,Client> clientsExpected = new HashMap<>();
    private final Map<String,Place> placesExpected = new HashMap<>();

    /**
     * Метод для выполнения действий перед каждым тестом класса
     * Приведение состояния базы данных и необходимых для тестов полей сервисов в состояние как перед первым запуском
     */
    @Before
    public void setUp() {
        queryService.dropTriggers();
        jdbcTemplate.execute("DELETE FROM transactions WHERE id>0;");
        jdbcTemplate.execute("DELETE FROM clients WHERE inn>0;");
        jdbcTemplate.execute("DELETE FROM places WHERE id>0;");
        jdbcTemplate.execute("commit;");
        ReflectionTestUtils.invokeMethod(StoreService.class,"doTerminate");
        ReflectionTestUtils.setField(StoreService.class, "parserDone", false);
        ReflectionTestUtils.setField(StoreService.class, "terminated", false);
        ReflectionTestUtils.setField(StoreService.class, "countErrorsBeforeShutdown", new AtomicInteger(0));
        ((Map<Long,Integer>) ReflectionTestUtils.getField(Trans.class,"transactions")).clear();

        try {
            TimeUnit.SECONDS.sleep(5);
        } catch (InterruptedException e) {/*пустое*/}
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
     * Тест-имитатор основного приложения с автогенерацией транзакций, клиентов и мест
     * Можно использовать как бенчмарк
     *
     * Если запустить тест, то все и не более сгенерированные транзакции, клиенты и места
     * будут добавлены в БД и данные будут эквивалентны
     */
    @Test
    @Transactional
    @Commit
    public void addHugeAmountOfRowsLongTest() {

        // Очистка кэш 2-го уровня и entityManager
        cacheManager.getCacheNames().forEach(cache->cacheManager.getCache(cache).clear());
        entityManager.clear();
        try {
            Thread.sleep(4000);
        } catch (InterruptedException e) {/*пустое*/}

        Logger logger = LoggerFactory.getLogger(this.getClass());
        StopWatch watch = new StopWatch();
        watch.start();
        logger.info("Начало работы теста");
        queryService.addTriggers();
        ReflectionTestUtils.setField(store, "parserDone", false);

        int threadsAvailable = Math.max(2,Math.min(amountOfThreads+1,Runtime.getRuntime().availableProcessors()));

        // Запуск сервисов добавления данных в БД
        ExecutorService executor = Executors.newFixedThreadPool(threadsAvailable);
        executor.execute(saveService);
        for (int i=2;i<threadsAvailable;i++) {
            if (!store.isParserDone()) {
                executor.execute(saveService);
            }
        }
        executor.shutdown();

        StopWatch watch1 = new StopWatch("addToDB");
        watch1.start();

        // Генерация и добавление транзакций в хранилище
        addToStore();

        while (!executor.isTerminated()) {
            try {
                Thread.sleep(1000);
                if (store.isTerminated()) {
                    executor.shutdownNow();
                    Thread.currentThread().interrupt();
                    break;
                }
            } catch (InterruptedException e) {/*пустое*/}
        }

        watch1.stop();
        logger.info("Добавлено "+ amountOfTransactionsExpected + " транзакций за " + (int)watch1.getTotalTimeSeconds() + " сек.");

        queryService.dropTriggers();

        logger.info("Проверки количества и соответствия данных...");
        long amountOfTransactionsActual = jdbcTemplate.queryForObject("select count(*) from transactions;", Long.class);
        Assert.isTrue(amountOfTransactionsActual==amountOfTransactionsExpected
                ,"Количество транзакций не совпадает\nожидаемое:" +amountOfTransactionsExpected+
                        "\nактуальное :"+amountOfTransactionsActual);
        long amountOfClientsActual = jdbcTemplate.queryForObject("select count(*) from clients;", Long.class);
        Assert.isTrue(amountOfClientsActual==clientsExpected.size()
                ,"Количество клиентов не совпадает\nожидаемое:" +clientsExpected.size()+
                        "\nактуальное :"+amountOfClientsActual);
        long amountOfPlacesActual = jdbcTemplate.queryForObject("select count(*) from places;", Long.class);
        Assert.isTrue(amountOfPlacesActual==placesExpected.size()
                ,"Количество мест проведения транзакций не совпадает\nожидаемое:" +placesExpected.size()+
                        "\nактуальное :"+amountOfPlacesActual);

        // Очистка кэш 2-го уровня и entityManager
        cacheManager.getCacheNames().forEach(cache->cacheManager.getCache(cache).clear());
        entityManager.clear();
        try {
            Thread.sleep(4000);
        } catch (InterruptedException e) {/*пустое*/}

        //Сравнение клиентов
        for (String client_id : clientsExpected.keySet()) {
            Client clientExpected = clientsExpected.get(client_id);
            Client clientActual = clientRepository.findById(clientExpected.getInn()).orElse(null);
            if (null==clientActual) {
                logger.error("Клиент не добавлен в БД: "+ clientExpected);
            } else {
                Assert.isTrue(clientExpected.deepEquals(clientActual),"Клиенты не совпадают!\n"
                        +"\nожидаемый: "+clientExpected + "актуальный:"+clientActual);
            }
        }
        // Сравнение мест
        for (String placeName : placesExpected.keySet()) {
            Place placeExpected = placesExpected.get(placeName);
            Place placeActual = entityManager.unwrap(Session.class).bySimpleNaturalId(Place.class).load(placeName);
            if (null==placeActual) {
                logger.error("Место не добавлено в БД: "+ placeExpected);
            } else {
                Assert.isTrue(placeExpected.deepEquals(placeActual),"Места не совпадают!\n"
                        +"\nожидаемое: "+placeExpected + "актуальное:"+placeActual);
            }
        }
        //Сравнение транзакций
        for (Long trans_id : transactionsExpected.keySet()) {
            Trans transactionExpected = transactionsExpected.get(trans_id);
            Trans transactionActual = transRepository.findById(transactionExpected.getId()).orElse(null);
            if (null==transactionActual) {
                logger.error("Транзакция не добавлена в БД: "+ transactionExpected);
            } else {
                Assert.isTrue(transactionExpected.deepEquals(transactionActual), "Транзакции не совпадают!\n"
                        + "\nожидаемая: " + transactionExpected + "актуальная:" + transactionActual);
            }
        }

        logger.info("Количество пакетов завершившихся с ошибками "+ReflectionTestUtils.getField(store,"countErrorsBeforeShutdown"));

        if (!store.isTerminated()) {
            logger.info("Все транзакции успешно добавлены в базу");
        } else {
            logger.error("Не все транзакции были добавлены в базу!");
        }
        watch.stop();
        logger.info("Завершение работы теста");
        logger.info("Время работы теста " + (int)watch.getTotalTimeSeconds() + " сек.");
    }

    /**
     * Генерирует и добавляет транзакции в хранилище. Имитирует работу ParseService
     */
    private void addToStore() {
        for (int serial=1;serial<=amountOfTransactionsExpected;serial++) {
            String inn = (1+(int)(Math.random()*amountOfClientsExpected)+"0000000000").substring(0,10);
            Client client = Client.newClient("Ivan","Ivanoff","Ivanoff",inn).orElse(null);
            clientsExpected.put(inn,client);
            String placeName = "A PLACE "+(int)(Math.random()*amountOfPlacesExpected);
            Place place = Place.newPlace(placeName).orElse(null);
            placesExpected.put(placeName,place);
            Trans transaction = Trans.newTrans(new BigDecimal((1+(int)(Math.random()*1000000))),"UAH","123456****1234",client,place,serial).orElse(null);
            transactionsExpected.put(transaction.getId(),transaction);
            store.add(transaction);
        }
        ReflectionTestUtils.setField(store, "parserDone", true);
    }
}
