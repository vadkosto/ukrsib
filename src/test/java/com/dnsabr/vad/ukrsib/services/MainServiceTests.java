package com.dnsabr.vad.ukrsib.services;

import com.dnsabr.vad.ukrsib.models.Trans;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.UncategorizedSQLException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.Assert;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.Assert.assertThrows;

/**
 * Интеграционные тесты сервиса MainService
 */
@RunWith(SpringRunner.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@SpringBootTest
@ActiveProfiles({"default"})
public class MainServiceTests {

    @Autowired
    private MainService mainService;
    @Autowired
    private QueryService queryService;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Value("${spring.jpa.properties.app.errors.count.before.terminate}")
    private int shutdownAfter;

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
        ReflectionTestUtils.invokeMethod(StoreService.class,"doTerminate");
        ReflectionTestUtils.setField(StoreService.class, "parserDone", false);
        ReflectionTestUtils.setField(StoreService.class, "terminated", false);
        ReflectionTestUtils.setField(StoreService.class, "countErrorsBeforeShutdown", new AtomicInteger(0));
        ((Map<Long,Integer>) ReflectionTestUtils.getField(Trans.class,"transactions")).clear();
        try {
            TimeUnit.SECONDS.sleep(5);
        } catch (InterruptedException e) {/*пустое*/}
    }

    @After
    public void tearDown() {
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
     * Тест возможности другими пользователями БД изменять данные
     * Когда приложение работает, другие пользователи не могут менять ключевые поля, но могут менять неключевые
     * Когда приложение полностью добавило данные из файла в БД, другие пользователи могут менять поля согласно
     * настройкам использованным при создании БД и удалять записи таблиц
     */
    @Test
    public void tryChangeAndDeleteRowsWhileRunningApplicationAndAfterFinishedTest() {

        // Запуск основного приложения и ожидание начала его работы
        Thread main = new Thread(() -> mainService.start());
        main.start();
        try {
            TimeUnit.SECONDS.sleep(2);
        } catch (InterruptedException e) {/*пустое*/}

        // Останавливаем основное приложение, чтобы успеть провести все проверки
        try {
            main.wait(10);
        } catch (InterruptedException | IllegalMonitorStateException e) {/*пустое*/}

        // Список имен триггеров для проверки
        List<String> listExpected = Arrays.asList(
                "trans_update_trigger_7did39f3",
                "places_update_trigger_7did39f3",
                "clients_update_trigger_7did39f3",
                "trans_delete_trigger_7did39f3",
                "places_delete_trigger_7did39f3",
                "clients_delete_trigger_7did39f3");

        // Когда приложение запущено, триггеры установлены
        List<String> listActual = new ArrayList<>();

        List<Map<String,Object>> result = jdbcTemplate.queryForList("show triggers;");
        result.forEach(x->listActual.add(x.get("Trigger").toString()));

        for (String triggerExpected : listExpected) {
            Assert.isTrue(listActual.contains(triggerExpected)
                    ,"Триггер "+triggerExpected+" для БД не установлен!");
        }

        // Когда пытаемся изменить ключевые поля и удалить данные, получаем ошибки по триггерам

        // Когда пытаемся удалить записи из таблицы clients, получаем ошибку по триггеру
        Exception exception = assertThrows(UncategorizedSQLException.class, () -> {
            jdbcTemplate.execute("DELETE FROM clients WHERE inn>0;");
        });
        String expectedMessageFull = "StatementCallback; uncategorized SQLException for SQL [DELETE FROM clients;]; SQL state [45000]; error code [1644]; Cannot delete this row while time new data are inserting; nested exception is java.sql.SQLException: Cannot delete this row while time new data are inserting";
        String expectedMessage = "new data are inserting";
        String actualMessage = exception.getMessage();
        Assert.isTrue(actualMessage.contains(expectedMessage),"Ошибки не совпадают" +
                "\nожидаемая :"+expectedMessageFull+"\nактуальная:" + actualMessage);

        // Когда пытаемся удалить записи из таблицы places, получаем ошибку по триггеру
        exception = assertThrows(UncategorizedSQLException.class, () -> {
            jdbcTemplate.execute("DELETE FROM places WHERE id>0;");
        });
        expectedMessageFull = "StatementCallback; uncategorized SQLException for SQL [DELETE FROM places;]; SQL state [45000]; error code [1644]; Cannot delete this row while time new data are inserting; nested exception is java.sql.SQLException: Cannot delete this row while time new data are inserting";
        expectedMessage = "new data are inserting";
        actualMessage = exception.getMessage();
        Assert.isTrue(actualMessage.contains(expectedMessage),"Ошибки не совпадают"
                + "\nожидаемая :"+expectedMessageFull + "\nактуальная:" + actualMessage);

        // Когда пытаемся удалить записи из таблицы transactions, получаем ошибку по триггеру
        exception = assertThrows(UncategorizedSQLException.class, () -> {
            jdbcTemplate.execute("DELETE FROM transactions WHERE id>0;");
        });
        expectedMessageFull = "StatementCallback; uncategorized SQLException for SQL [DELETE FROM transactions;]; SQL state [45000]; error code [1644]; Cannot delete this row while time new data are inserting; nested exception is java.sql.SQLException: Cannot delete this row while time new data are inserting";
        expectedMessage = "new data are inserting";
        actualMessage = exception.getMessage();
        Assert.isTrue(actualMessage.contains(expectedMessage),"Ошибки не совпадают"
                + "\nожидаемая :"+expectedMessageFull + "\nактуальная:" + actualMessage);

        // Когда пытаемся изменить inn в таблице clients, получаем ошибку по триггеру
        exception = assertThrows(UncategorizedSQLException.class, () -> {
            jdbcTemplate.execute("UPDATE clients SET inn='1' WHERE inn='1234567890';");
        });
        expectedMessageFull = "StatementCallback; uncategorized SQLException for SQL [UPDATE clients SET inn='1' WHERE inn='1234567890';]; SQL state [45000]; error code [1644]; Cannot update this field while time new data are inserting; nested exception is java.sql.SQLException: Cannot update this field while time new data are inserting";
        expectedMessage = "new data are inserting";
        actualMessage = exception.getMessage();
        Assert.isTrue(actualMessage.contains(expectedMessage),"Ошибки не совпадают"
                + "\nожидаемая :"+expectedMessageFull + "\nактуальная:" + actualMessage);

        // Когда пытаемся изменить id в таблице places, получаем ошибку по триггеру
        exception = assertThrows(UncategorizedSQLException.class, () -> {
            jdbcTemplate.execute("UPDATE places SET id=1 WHERE id>1;");
        });
        expectedMessageFull = "StatementCallback; uncategorized SQLException for SQL [UPDATE places SET id=1 WHERE id>1;]; SQL state [45000]; error code [1644]; Cannot update this field while time new data are inserting; nested exception is java.sql.SQLException: Cannot update this field while time new data are inserting";
        expectedMessage = "new data are inserting";
        actualMessage = exception.getMessage();
        Assert.isTrue(actualMessage.contains(expectedMessage),"Ошибки не совпадают"
                + "\nожидаемая :"+expectedMessageFull + "\nактуальная:" + actualMessage);

        // Когда пытаемся изменить place в таблице places, получаем ошибку по триггеру
        exception = assertThrows(UncategorizedSQLException.class, () -> {
            jdbcTemplate.execute("UPDATE places SET place='1' WHERE place='A PLACE 2';");
        });
        expectedMessageFull = "StatementCallback; uncategorized SQLException for SQL [UPDATE places SET place='1' WHERE place='A PLACE 2';]; SQL state [45000]; error code [1644]; Cannot update this field while time new data are inserting; nested exception is java.sql.SQLException: Cannot update this field while time new data are inserting";
        expectedMessage = "new data are inserting";
        actualMessage = exception.getMessage();
        Assert.isTrue(actualMessage.contains(expectedMessage),"Ошибки не совпадают"
                + "\nожидаемая :"+expectedMessageFull + "\nактуальная:" + actualMessage);

        // Когда пытаемся изменить id в таблице transactions, получаем ошибку по триггеру
        exception = assertThrows(UncategorizedSQLException.class, () -> {
            jdbcTemplate.execute("UPDATE transactions SET id=1 WHERE id>1;");
        });
        expectedMessageFull = "StatementCallback; uncategorized SQLException for SQL [UPDATE transactions SET id=1 WHERE id>1;]; SQL state [45000]; error code [1644]; Cannot update this field while time new data are inserting; nested exception is java.sql.SQLException: Cannot update this field while time new data are inserting";
        expectedMessage = "new data are inserting";
        actualMessage = exception.getMessage();
        Assert.isTrue(actualMessage.contains(expectedMessage),"Ошибки не совпадают"
                + "\nожидаемая :"+expectedMessageFull + "\nактуальная:" + actualMessage);

        // Когда пытаемся изменить поля с установленными foreign keys на существующие, не должны получить ошибки
        int id_place = jdbcTemplate.queryForObject("SELECT id FROM places WHERE place='A PLACE 3';",Integer.class);
        jdbcTemplate.execute("UPDATE transactions SET place_id="+(id_place+1)+" WHERE place_id="+id_place+";");
        jdbcTemplate.execute("UPDATE transactions SET client_id='1234567890' WHERE client_id='1234567891';");

        // Когда пытаемся изменить поля с установленными foreign keys на несуществующие, должны получить ошибки по foreign keys

        // Когда пытаемся установить в поле place_id значение не существующего id в таблице places, должны получить ошибку по foreign keys
        exception = assertThrows(DataIntegrityViolationException.class, () -> {
            jdbcTemplate.execute("UPDATE transactions SET place_id=99 WHERE place_id="+(id_place-1)+";");
        });
        expectedMessageFull = "StatementCallback; SQL [UPDATE transactions SET place_id=99 WHERE place_id="+(id_place-1)+";]; Cannot add or update a child row: a foreign key constraint fails (`test`.`transactions`, CONSTRAINT `FK9ty9ssdjsl3xou5mdk2kx19wo` FOREIGN KEY (`place_id`) REFERENCES `places` (`id`)); nested exception is java.sql.SQLIntegrityConstraintViolationException: Cannot add or update a child row: a foreign key constraint fails (`test`.`transactions`, CONSTRAINT `FK9ty9ssdjsl3xou5mdk2kx19wo` FOREIGN KEY (`place_id`) REFERENCES `places` (`id`))";
        expectedMessage = "foreign key constraint fails";
        actualMessage = exception.getMessage();
        Assert.isTrue(actualMessage.contains(expectedMessage),"Ошибки не совпадают"
                + "\nожидаемая :"+expectedMessageFull + "\nактуальная:" + actualMessage);

        // Когда пытаемся установить в поле client_id значение не существующего id в таблице clients, должны получить ошибку по foreign keys
        exception = assertThrows(DataIntegrityViolationException.class, () -> {
            jdbcTemplate.execute("UPDATE transactions SET client_id='99' WHERE client_id='1234567890';");
        });
        expectedMessageFull = "StatementCallback; SQL [UPDATE transactions SET client_id='1' WHERE client_id='1234567890';]; Cannot add or update a child row: a foreign key constraint fails (`test`.`transactions`, CONSTRAINT `FKjp6w7dmqrj0h9vykk2pbtik2` FOREIGN KEY (`client_id`) REFERENCES `clients` (`inn`)); nested exception is java.sql.SQLIntegrityConstraintViolationException: Cannot add or update a child row: a foreign key constraint fails (`test`.`transactions`, CONSTRAINT `FKjp6w7dmqrj0h9vykk2pbtik2` FOREIGN KEY (`client_id`) REFERENCES `clients` (`inn`))";
        expectedMessage = "foreign key constraint fails";
        actualMessage = exception.getMessage();
        Assert.isTrue(actualMessage.contains(expectedMessage),"Ошибки не совпадают"
                + "\nожидаемая :"+expectedMessageFull + "\nактуальная:" + actualMessage);

        // Когда пытаемся изменить значения неключевых полей в таблицах, не должны получить ошибки
        jdbcTemplate.execute("UPDATE transactions SET amount=1 WHERE client_id='1234567890';");
        jdbcTemplate.execute("UPDATE transactions SET card='1' WHERE client_id='1234567890';");
        jdbcTemplate.execute("UPDATE transactions SET currency='1' WHERE client_id='1234567890'");
        jdbcTemplate.execute("UPDATE clients SET first_name='1' WHERE inn='1234567890';");
        jdbcTemplate.execute("UPDATE clients SET last_name='1' WHERE inn='1234567890';");
        jdbcTemplate.execute("UPDATE clients SET middle_name='1' WHERE inn='1234567890';");

        // Проверка работает ли до сих пор поток основного приложения
        Assert.isTrue(main.isAlive(),"Тестируемый сервис завершился раньше прохождения всех тестов");

        // Оповещаем основное приложение и ждем его завершения
        try {
            main.notify();
        } catch (IllegalMonitorStateException e) {/*пустое*/}
        while (main.isAlive()) {
            try {
                TimeUnit.SECONDS.sleep(1);
            } catch (InterruptedException e) {/*пустое*/}
        }
        try {
            TimeUnit.SECONDS.sleep(2);
        } catch (InterruptedException e) {/*пустое*/}

        // Когда приложение завершено, триггеры не установлены
        listActual.clear();
        result = jdbcTemplate.queryForList("show triggers;");
        result.forEach(x->listActual.add(x.get("Trigger").toString()));

        listExpected.forEach(triggerExpected->Assert.isTrue(!listActual.contains(triggerExpected)
                ,"Триггер "+triggerExpected+" не удален из БД!"));

        // Проверки удаления и изменения строк после того как приложение полностью добавило все данные из файла в БД

        // Когда пытаемся изменить поля с установленными foreign keys на существующие, не должны получить ошибки
        jdbcTemplate.execute("UPDATE transactions SET place_id="+id_place+" WHERE place_id="+(id_place+1)+";");
        jdbcTemplate.execute("UPDATE transactions SET client_id='1234567891' WHERE client_id='1234567890';");

        // Когда пытаемся изменить поля с установленными foreign keys на несуществующие, должны получить ошибки по foreign keys

        // Когда пытаемся установить в поле place_id значение не существующего id в таблице places, должны получить ошибку по foreign keys
        exception = assertThrows(DataIntegrityViolationException.class, () -> {
            jdbcTemplate.execute("UPDATE transactions SET place_id=99 WHERE place_id="+(id_place-1)+";");
        });
        expectedMessageFull = "StatementCallback; SQL [UPDATE transactions SET place_id=99 WHERE place_id="+(id_place-1)+";]; Cannot add or update a child row: a foreign key constraint fails (`test`.`transactions`, CONSTRAINT `FK9ty9ssdjsl3xou5mdk2kx19wo` FOREIGN KEY (`place_id`) REFERENCES `places` (`id`)); nested exception is java.sql.SQLIntegrityConstraintViolationException: Cannot add or update a child row: a foreign key constraint fails (`test`.`transactions`, CONSTRAINT `FK9ty9ssdjsl3xou5mdk2kx19wo` FOREIGN KEY (`place_id`) REFERENCES `places` (`id`))";
        expectedMessage = "foreign key constraint fails";
        actualMessage = exception.getMessage();
        Assert.isTrue(actualMessage.contains(expectedMessage),"Ошибки не совпадают"
                + "\nожидаемая :"+expectedMessageFull + "\nактуальная:" + actualMessage);

        // Когда пытаемся установить в поле client_id значение не существующего id в таблице clients, должны получить ошибку по foreign keys
        exception = assertThrows(DataIntegrityViolationException.class, () -> {
            jdbcTemplate.execute("UPDATE transactions SET client_id='99' WHERE client_id='1234567891';");
        });
        expectedMessageFull = "StatementCallback; SQL [UPDATE transactions SET client_id='1' WHERE client_id='1234567891';]; Cannot add or update a child row: a foreign key constraint fails (`test`.`transactions`, CONSTRAINT `FKjp6w7dmqrj0h9vykk2pbtik2` FOREIGN KEY (`client_id`) REFERENCES `clients` (`inn`)); nested exception is java.sql.SQLIntegrityConstraintViolationException: Cannot add or update a child row: a foreign key constraint fails (`test`.`transactions`, CONSTRAINT `FKjp6w7dmqrj0h9vykk2pbtik2` FOREIGN KEY (`client_id`) REFERENCES `clients` (`inn`))";
        expectedMessage = "foreign key constraint fails";
        actualMessage = exception.getMessage();
        Assert.isTrue(actualMessage.contains(expectedMessage),"Ошибки не совпадают"
                + "\nожидаемая :"+expectedMessageFull + "\nактуальная:" + actualMessage);

        // Когда пытаемся изменить значения неключевых полей в таблицах, не должны получить ошибки
        jdbcTemplate.execute("UPDATE transactions SET amount=100 WHERE client_id='1234567891';");
        jdbcTemplate.execute("UPDATE transactions SET card='100' WHERE client_id='1234567891';");
        jdbcTemplate.execute("UPDATE transactions SET currency='100' WHERE client_id='1234567891';");
        jdbcTemplate.execute("UPDATE clients SET first_name='1' WHERE inn='1234567891';");
        jdbcTemplate.execute("UPDATE clients SET last_name='1' WHERE inn='1234567891';");
        jdbcTemplate.execute("UPDATE clients SET middle_name='1' WHERE inn='1234567891';");
        jdbcTemplate.execute("UPDATE places SET place='1111' WHERE place='A PLACE 2';");

        // Попытка изменить id транзакции - должно быть без ошибок
        // Когда пытаемся изменить поле id в таблице transactions, не должны получить ошибку
        long id = jdbcTemplate.queryForObject("select id from transactions limit 1;", Long.class);
        jdbcTemplate.execute("UPDATE transactions SET id=100 WHERE id="+id+";");

        // Когда пытаемся удалить строки из таблицы clients, получаем ошибку по foreign key
        exception = assertThrows(DataIntegrityViolationException.class, () -> {
            jdbcTemplate.execute("DELETE FROM clients WHERE inn>0;");
        });
        expectedMessageFull = "StatementCallback; SQL [DELETE FROM clients WHERE inn>0;]; Cannot delete or update a parent row: a foreign key constraint fails (`test`.`transactions`, CONSTRAINT `FKjp6w7dmqrj0h9vykk2pbtik2` FOREIGN KEY (`client_id`) REFERENCES `clients` (`inn`)); nested exception is java.sql.SQLIntegrityConstraintViolationException: Cannot delete or update a parent row: a foreign key constraint fails (`test`.`transactions`, CONSTRAINT `FKjp6w7dmqrj0h9vykk2pbtik2` FOREIGN KEY (`client_id`) REFERENCES `clients` (`inn`))";
        expectedMessage = "foreign key constraint fails";
        actualMessage = exception.getMessage();
        Assert.isTrue(actualMessage.contains(expectedMessage),"Ошибки не совпадают\nожидаемая :"
                +expectedMessageFull+"\nактуальная:" + actualMessage);

        // Когда пытаемся удалить строки из таблицы places, получаем ошибку по foreign key
        exception = assertThrows(DataIntegrityViolationException.class, () -> {
            jdbcTemplate.execute("DELETE FROM places WHERE id>0;");
        });
        expectedMessageFull = "StatementCallback; SQL [DELETE FROM places WHERE id>0;]; Cannot delete or update a parent row: a foreign key constraint fails (`test`.`transactions`, CONSTRAINT `FK9ty9ssdjsl3xou5mdk2kx19wo` FOREIGN KEY (`place_id`) REFERENCES `places` (`id`)); nested exception is java.sql.SQLIntegrityConstraintViolationException: Cannot delete or update a parent row: a foreign key constraint fails (`test`.`transactions`, CONSTRAINT `FK9ty9ssdjsl3xou5mdk2kx19wo` FOREIGN KEY (`place_id`) REFERENCES `places` (`id`))";
        expectedMessage = "foreign key constraint fails";
        actualMessage = exception.getMessage();
        Assert.isTrue(actualMessage.contains(expectedMessage),"Ошибки не совпадают\nожидаемая :"
                +expectedMessageFull+"\nактуальная:" + actualMessage);

        // Когда пытаемся удалить строки из таблицы transactions, не должны получить ошибку
        jdbcTemplate.execute("DELETE FROM transactions WHERE id>0;");

        // Когда пытаемся удалить строки из таблицы clients, не должны получить ошибку, т.к. уже удалены все строки
        // из таблицы transactions
        jdbcTemplate.execute("DELETE FROM clients WHERE inn>0;");

        // Когда пытаемся удалить строки из таблицы places, не должны получить ошибку, т.к. уже удалены все строки
        // из таблицы transactions
        jdbcTemplate.execute("DELETE FROM places WHERE id>0;");
    }
}