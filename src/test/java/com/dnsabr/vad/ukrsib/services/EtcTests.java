package com.dnsabr.vad.ukrsib.services;

import com.dnsabr.vad.ukrsib.models.Client;
import com.dnsabr.vad.ukrsib.models.Place;
import com.dnsabr.vad.ukrsib.models.Trans;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.Assert;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Прочие интеграционные тесты
 */
@RunWith(SpringRunner.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@SpringBootTest
@ActiveProfiles({"showsql"})
public class EtcTests {

    @Autowired
    TransService transService;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * Метод для выполнения действий перед каждым тестом класса
     * Приведение состояния базы данных и необходимых для тестов полей сервисов в состояние как перед первым запуском
     */
    @Before
    public void setUp() {
        jdbcTemplate.execute("DELETE FROM transactions WHERE id>0;");
        jdbcTemplate.execute("DELETE FROM clients WHERE inn>0;");
        jdbcTemplate.execute("DELETE FROM places WHERE id>0;");
        // Добавляем Место в БД, чтобы оградиться от ошибок, связанных с ним
        jdbcTemplate.execute("insert ignore into places (place, id) values ('A PLACE 0', 1);");
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
     * Тест вставки при измененном SQL-запросе insert ignore
     * Когда несколько потоков пытается одновременно добавить транзакции с одинаковыми Клиентами,
     * все транзакции и один Клиент добавляются в БД
     * и логи SQL-сервера не содержат Duplicate entry '0123456789' for key 'PRIMARY'
     */
    @Test
    public void insertIgnoreTest() {

        int amountOfTransactionsExpected = 450;
        int amountOfClientsExpected = 1;

        Client client = Client.newClient("Ivan","Ivanoff","Ivanoff","0123456789").orElse(null);
        Place place = Place.newPlace("A PLACE 0").orElse(null);

        ExecutorService executor = Executors.newFixedThreadPool(3);

        // Переопределяем системный вывод для чтения сообщений SQL-сервера
        final PrintStream standardOut = System.out;
        final ByteArrayOutputStream outputStreamCaptor = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outputStreamCaptor));

        // Генерация пакетов транзакций
        List<Trans> transactions = new ArrayList<>();
        List<Trans> transactions1 = new ArrayList<>();
        List<Trans> transactions2 = new ArrayList<>();
        addToStore(transactions,client,place,1);
        addToStore(transactions1,client,place,151);
        addToStore(transactions2,client,place,301);

        // Одновременное добавление данных в БД в трех потоках
        executor.execute(()->transService.saveAll(transactions));
        executor.execute(()->transService.saveAll(transactions1));
        executor.execute(()->transService.saveAll(transactions2));

        // Ожидание завершения основного приложения
        executor.shutdown();
        while (!executor.isTerminated()) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {/*пустое*/}
        }

        // Возврат вывода в консоль
        System.setOut(standardOut);
        String logs = outputStreamCaptor.toString();
        System.out.println(logs);

        boolean gotMessageNotExpected = logs.contains("Duplicate entry '0123456789' for key 'PRIMARY");

        Assert.isTrue(!gotMessageNotExpected
                ,"Не работает INSERT IGNORE - Получено сообщение от SQL сервера: " +
                        "Duplicate entry '0123456789' for key 'PRIMARY'");

        long amountOfTransactionsActual = jdbcTemplate.queryForObject("select count(*) from transactions;", Long.class);
        Assert.isTrue(amountOfTransactionsActual==amountOfTransactionsExpected
                ,"Количество транзакций не совпадает\nожидаемое:" +amountOfTransactionsExpected+
                        "\nактуальное :"+amountOfTransactionsActual);

        long amountOfClientsActual = jdbcTemplate.queryForObject("select count(*) from clients;", Long.class);
        Assert.isTrue(amountOfClientsActual==amountOfClientsExpected
                ,"Количество клиентов не совпадает\nожидаемое:" +amountOfClientsExpected+
                        "\nактуальное :"+amountOfClientsActual);
    }

    /**
     * Генерирует и добавляет транзакции в хранилище. Имитирует работу ParseService
     */
    private void addToStore(List<Trans> transactions, Client client, Place place, int serial) {
        for (int i = serial; i < serial+150; i++) {
            Trans transaction = Trans.newTrans(new BigDecimal(1), "UAH", "123456****1234", client, place, i).orElse(null);
            transactions.add(transaction);
        }
    }
}
