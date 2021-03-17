package com.dnsabr.vad.ukrsib.services;

import com.dnsabr.vad.ukrsib.models.*;
import com.dnsabr.vad.ukrsib.repository.*;
import com.vladmihalcea.sql.SQLStatementCountValidator;
import org.hibernate.Session;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static com.vladmihalcea.sql.SQLStatementCountValidator.*;

/**
 * Интеграционные тесты сервиса TransService
 */
@RunWith(SpringRunner.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class TransServiceTests {

    @Autowired
    private ClientRepository clientRepository;
    @Autowired
    private PlaceRepository placeRepository;
    @Autowired
    private TransService transService;
    @Autowired
    private TransRepository transRepository;
    @Autowired
    private QueryService queryService;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @PersistenceContext
    private EntityManager entityManager;
    @Autowired
    private CacheManager cacheManager;

    /**
     * Метод для выполнения действий перед каждым тестом класса
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
     * Тест отката JPA-транзакции
     * Когда таймаут JPA-транзакции превышен, транзакция откатывается
     */
    @Test
    @Transactional
    public void whenLockedRowsThenRollbackTest() {
        // При блокировке в таблице places
        jdbcTemplate.execute("select place from places for update");
        Client clientNotExpected = Client.newClient("Ivan","Ivanoff","Ivanoff","1234567899").orElse(null);
        Place placeNotExpected = Place.newPlace("A PLACE 11").orElse(null);
        Trans transactionNotExpected = Trans.newTrans(new BigDecimal("10.01"),"UAH","123456****1234",clientNotExpected,placeNotExpected,1).orElse(null);
        ReflectionTestUtils.setField(transactionNotExpected,"id",111111);
        List<Trans> list = new ArrayList<>(1);
        list.add(transactionNotExpected);
        SQLStatementCountValidator.reset();
        Assert.isTrue(0==transService.saveAll(list)
                ,"Транзакция была добавлена в БД во время посторонней блокировки");
        assertSelectCount(3); // по 1 select на каждый тип
        assertUpdateCount(0);
        assertInsertCount(2);  // Если тест не провалился, то эти inserts откатились
        Trans transactionActual = transRepository.findById(111111L).orElse(null);
        Assert.isNull(transactionActual
                ,"Транзакция была добавлена в БД во время посторонней блокировки");
        Client clientActual = clientRepository.findById("1234567899").orElse(null);
        Assert.isNull(clientActual
                ,"Клиент был добавлен в БД во время посторонней блокировки");
        Place placeActual = entityManager.unwrap(Session.class).bySimpleNaturalId(Place.class).load("A PLACE 11");
        Assert.isNull(placeActual
                ,"Место было добавлено в БД во время посторонней блокировки");
        jdbcTemplate.execute("rollback");

        // При блокировке в таблице clients
        jdbcTemplate.execute("select first_name from clients for update");
        SQLStatementCountValidator.reset();
        Assert.isTrue(0==transService.saveAll(list)
                ,"Транзакция была добавлена в БД во время посторонней блокировки");
        assertSelectCount(3); // по 1 select на каждый тип
        assertUpdateCount(0);
        assertInsertCount(1);  // Если тест не провалился, то эти inserts откатились
        transactionActual = transRepository.findById(111111L).orElse(null);
        Assert.isNull(transactionActual
                ,"Транзакция была добавлена в БД во время посторонней блокировки");
        clientActual = clientRepository.findById("1234567899").orElse(null);
        Assert.isNull(clientActual
                ,"Клиент был добавлен в БД во время посторонней блокировки");
        placeActual = entityManager.unwrap(Session.class).bySimpleNaturalId(Place.class).load("A PLACE 11");
        Assert.isNull(placeActual
                ,"Место было добавлено в БД во время посторонней блокировки");
        jdbcTemplate.execute("rollback");

        // При блокировке в таблице transactions
        jdbcTemplate.execute("select amount from transactions for update");
        SQLStatementCountValidator.reset();
        Assert.isTrue(0==transService.saveAll(list)
                ,"Транзакция была добавлена в БД во время посторонней блокировки");
        assertSelectCount(3); // по 1 select на каждый тип
        assertUpdateCount(0);
        assertInsertCount(3);  // Если тест не провалился, то эти inserts откатились
        transactionActual = transRepository.findById(111111L).orElse(null);
        Assert.isNull(transactionActual
                ,"Транзакция была добавлена в БД во время посторонней блокировки");
        clientActual = clientRepository.findById("1234567899").orElse(null);
        Assert.isNull(clientActual
                ,"Клиент был добавлен в БД во время посторонней блокировки");
        placeActual = entityManager.unwrap(Session.class).bySimpleNaturalId(Place.class).load("A PLACE 11");
        Assert.isNull(placeActual
                ,"Место было добавлено в БД во время посторонней блокировки");
        jdbcTemplate.execute("rollback");
    }

    /**
     * Тест количества SELECT, INSERT и UPDATE при пакетной вставке и количества добавленных строк в таблицах БД
     * Когда добавляются новые транзакции, для транзакций выполняется один запрос INSERT, для клиентов один INSERT,
     *  для мест один INSERT, по одному запросу SELECT для каждой транзакции, и по одному запросу SELECT для каждого
     *  клиента и места
     * Когда повторно добавляются транзакции, выполняются по одному запросу SELECT для каждой транзакции, и не
     *  выполняются запросы SELECT для клиентов и мест, так как они уже присутствуют в кэш 2-го уровня и не выполняются
     *  запросы INSERT
     * Когда повторно добавляются транзакции, после изменения внешним источником части данных в БД, выполняется
     *  один batchUpdate для всех транзакций, чьи поля были изменены в БД, выполняются по одному запросу SELECT для
     *  каждой транзакции, и не выполняются запросы SELECT для клиентов и мест, так как они уже присутствуют в
     *  кэш 2-го уровня и не выполняются запросы INSERT и не происходят изменения в таблице clients, т.к. для клиентов
     *  работает кэш 2-го уровня, где данные клиентов не изменены внешним источником
     *  (places не рассматриваем - триггер не позволит внешнему источнику изменить данные)
     */
    @Test
    public void saveTransactionsWithBatchInsertAndRepeatAndUpdateTest() {

        List<Trans> transListActual = transRepository.findAll();
        List<Trans> transactions = new ArrayList<>();

        Client client = Client.newClient("Ivan","Ivanoff","Ivanoff","1111111111").orElse(null);
        Place place = Place.newPlace("A PLACE 12").orElse(null);
        for (int i=1;i<=4;i++) {
            Trans transaction = Trans.newTrans(new BigDecimal("10.01"),"UAH","123456****1234",client,place,i).orElse(null);
            ReflectionTestUtils.setField(transaction,"id",i);
            transactions.add(transaction);
        }

        // Когда добавляем 4 новые транзакции в БД с теми же клиентом и местом, добавляется 4 транзакции и по одному клиенту и месту,
        // количество insert составляет 3 - по 1 запросу для транзакций, клиентов, мест
        // и количество select составляет 6 - 4 для транзакций и по одному для клиента и места
        SQLStatementCountValidator.reset();
        Assert.isTrue(0!=transService.saveAll(transactions)
                ,"Транзакции не добавлены в БД!");
        assertSelectCount("Количество запросов SELECT не отвечает ожиданиям",6);
        assertUpdateCount("Количество запросов UPDATE не отвечает ожиданиям",0);
        assertInsertCount("Количество запросов INSERT не отвечает ожиданиям",3);
        int transAmountExpected = transListActual.size()+4;
        transListActual = transRepository.findAll();
        Assert.isTrue(transAmountExpected==transListActual.size() ,"Количество транзакций не совпадают"
                + "\nожидаемое: "+ transAmountExpected + "\nактуальное:" + transListActual.size() );

        // Когда повторяем вставку с теми же данними, не происходят изменения в БД, а количество select снижается до 4-х
        // т.к. для клиентов и мест работает кэш 2-го уровня
        SQLStatementCountValidator.reset();
        Assert.isTrue(0!=transService.saveAll(transactions)
                ,"Транзакции не добавлены в БД!");
        assertSelectCount("Количество запросов SELECT не отвечает ожиданиям",4);
        assertUpdateCount("Количество запросов UPDATE не отвечает ожиданиям",0);
        assertInsertCount("Количество запросов INSERT не отвечает ожиданиям",0);
        transListActual = transRepository.findAll();
        Assert.isTrue(transAmountExpected==transListActual.size(),"Количество транзакций не совпадают"
                + "\nожидаемое: "+ transAmountExpected + "\nактуальное:" + transListActual.size());

        // Имитация изменения внешним источником части данных в БД
        jdbcTemplate.execute("update transactions set amount=111 where id=2");
        jdbcTemplate.execute("update transactions set amount=111 where id=3");
        jdbcTemplate.execute("update clients set first_name='Petr' where inn=1111111111");
//        jdbcTemplate.execute("update places set place='A PLACE 9' where place='A PLACE 12'");

        // Когда повторяем вставку с теми же данними после изменения внешним источником части данных в БД, выполняется
        // один batchUpdate для 2-х транзакций, чьи поля были изменены в БД, количество select не изменяется = 4
        // и не происходят изменения в таблице clients, т.к. для клиентов работает кэш 2-го уровня
        SQLStatementCountValidator.reset();
        Assert.isTrue(0!=transService.saveAll(transactions)
                ,"Транзакции не добавлены в БД!");
        assertSelectCount("Количество запросов SELECT не отвечает ожиданиям",4);
        assertUpdateCount("Количество запросов UPDATE не отвечает ожиданиям",1);
        assertInsertCount("Количество запросов INSERT не отвечает ожиданиям",0);

        Trans transExpected = transactions.get(1);
        Client clientExpected = transExpected.getClient();
        clientExpected.setFirstName("Petr"); // так как это поле мы обновили в БД с помощью jdbcTemplate
        Place placeExpected = transExpected.getPlace();

        // Удаляем проверяемого клиента и место из кэш 2-го уровня
        cacheManager.getCache("default").evict(clientExpected);
        cacheManager.getCache("default").evict(placeExpected);
        // Ожидаем удаления объектов из кеш
        try {
            TimeUnit.SECONDS.sleep(4);
        } catch (InterruptedException e) {/*пустое*/}

        // Проверяем количество и данные транзакций
        transListActual = transRepository.findAll();
        Assert.isTrue(transAmountExpected==transListActual.size(),"Количество транзакций не совпадают"
                + "\nожидаемое: "+ transAmountExpected + "\nактуальное:" + transListActual.size());

        int placeId = jdbcTemplate.queryForObject("select id from places where place='A PLACE 12';", Integer.class);
        Trans transActual = transRepository.findById(2L).orElseThrow();
        Client clientActual = clientRepository.findById("1111111111").orElseThrow();
        Place placeActual = placeRepository.findById(placeId).orElseThrow();

        Assert.isTrue(clientExpected.deepEquals(clientActual), "Данные клиентов не совпадают!"
                + "\nожидаемые: " + clientExpected + "\nактуальные:" + clientActual);
        Assert.isTrue(placeExpected.deepEquals(placeActual),"Данные мест не совпадают!"
                +"\nожидаемые: "+placeExpected + "\nактуальные:" +placeActual);

        transActual.setClient(clientActual);
        transActual.setPlace(placeActual);
        transExpected.setClient(clientExpected);
        transExpected.setPlace(placeExpected);
        Assert.isTrue(transExpected.deepEquals(transActual),"Данные транзакции не совпадают!"
                +"\nожидаемые: "+transExpected + "\nактуальные:" +transActual);
    }

    /**
     * Тест установленного уровня изоляции и обновления полей таблицы transactions в БД
     * Когда повторно вставляем транзакцию во время наличия в БД неподтвержденных изменений, такие изменения будут
     *  проигнорированы
     * Когда повторно вставляем транзакцию во время наличия в БД подтвержденных изменений, такие изменения будут
     *  исправлены
     */
    @Test
    @Transactional
    public void doNotReadUncommitedAndUpdateNotEqualsFieldsTest() {

        Client client = Client.newClient("Ivan","Ivanoff","Ivanoff","8888888888").orElse(null);
        Place place = Place.newPlace("A PLACE 13").orElse(null);
        Trans transactionExpected = Trans.newTrans(new BigDecimal("10.01"),"UAH","123456****1234",client,place,8).orElse(null);
        ReflectionTestUtils.setField(transactionExpected,"id",8);
        List<Trans> list = new ArrayList<>(1);
        list.add(transactionExpected);
        transService.saveAll(list);

        // Внешний источник начал транзакцию и внес изменения, но еще не подтвердил их
        jdbcTemplate.execute("update transactions set amount=111 where id=8");

        // Когда повторно вставляем транзакцию во время наличия в БД неподтвержденных изменений, такие изменения будут
        // проигнорированы (1 select, 0 update)
        SQLStatementCountValidator.reset();
        transService.saveAll(list);
        assertSelectCount("Количество запросов SELECT не отвечает ожиданиям",1);
        assertUpdateCount("Количество запросов UPDATE не отвечает ожиданиям",0);
        assertInsertCount("Количество запросов INSERT не отвечает ожиданиям",0);

        // Внешний источник подтвердил свои изменения
        jdbcTemplate.execute("commit");

        // Когда повторно вставляем транзакцию во время наличия в БД подтвержденных изменений, такие изменения будут
        // исправлены (1 select, 1 update)
        SQLStatementCountValidator.reset();
        transService.saveAll(list);
        assertSelectCount(1); // данные из БД были прочитаны
        assertUpdateCount(1); // и обновлены в БД, так как amount=111 не совпадает с данными транзакции
        assertInsertCount(0);

        Trans transactionActual = transRepository.findById(8L).orElseThrow();
        Assert.isTrue(transactionExpected.getAmount().compareTo(transactionActual.getAmount())==0
                ,"Не было выполнено обновление неключевого поля amount\nожидаемое: " + transactionExpected.getAmount()
                        +"\nактуальное:" + transactionActual.getAmount());
    }

    /**
     * Тест кэш 2-го уровня и отсутствия обновления таблиц clients и places (для теста триггеры отключены)
     * Когда добавляются новые транзакции, для транзакций выполняется один запрос INSERT, для клиентов один INSERT,
     *  для мест один INSERT, по одному запросу SELECT для каждой транзакции, и по одному запросу SELECT для каждого
     *  клиента и места, т.к. они еще не в кеш
     * Когда добавляются новые транзакции с существующими клиентами и местами, для транзакций выполняется
     *  один запрос INSERT, для клиентов и мест не выполняются SELECT и INSERT т.к. они есть в кэш 2-го уровня,
     *  по одному запросу SELECT для каждой транзакции
     */
    @Test
    public void secondLevelCacheWorksAndDoNotUpdateClientAndPlaceTest() {

        queryService.dropTriggers();
        List<Trans> transactions = new ArrayList<>();
        Client clientExpected = Client.newClient("Ivan","Ivanoff","Ivanoff","3333333333").orElse(null);
        Place placeExpected = Place.newPlace("A PLACE 14").orElse(null);
        for (int i=11;i<=14;i++) {
            Trans transaction = Trans.newTrans(new BigDecimal("10.01"),"UAH","123456****1234",clientExpected,placeExpected,i).orElse(null);
            ReflectionTestUtils.setField(transaction,"id",i);
            transactions.add(transaction);
        }

        // Когда добавляются новые транзакции, для транзакций выполняется один запрос INSERT, для клиентов один INSERT,
        // для мест один INSERT, по одному запросу SELECT для каждой транзакции, и по одному запросу SELECT для каждого
        // клиента и места, т.к. они еще не в кеш
        SQLStatementCountValidator.reset();
        transService.saveAll(transactions);
        assertSelectCount("Количество запросов SELECT не отвечает ожиданиям",6);
        assertUpdateCount("Количество запросов UPDATE не отвечает ожиданиям",0);
        assertInsertCount("Количество запросов INSERT не отвечает ожиданиям",3);

        // Имитируем изменение БД из внешних источников
        jdbcTemplate.execute("update clients set first_name='Sergey' where inn='3333333333'");
        jdbcTemplate.execute("update places set place='A PLACE 111' where place='A PLACE 14'");
        jdbcTemplate.execute("commit");

        transactions.clear();
        for (int i=15;i<=18;i++) {
            Trans transaction = Trans.newTrans(new BigDecimal("10.01"),"UAH","123456****1234",clientExpected,placeExpected,i).orElse(null);
            ReflectionTestUtils.setField(transaction,"id",i);
            transactions.add(transaction);
        }

        // Когда добавляются новые транзакции с существующими клиентами и местами, для транзакций выполняется
        // один запрос INSERT, для клиентов и мест не выполняются SELECT и INSERT т.т. они есть в кэш 2-го уровня,
        // по одному запросу SELECT для каждой транзакции
        SQLStatementCountValidator.reset();
        transService.saveAll(transactions);
        assertSelectCount("Количество запросов SELECT не отвечает ожиданиям",4);
        assertUpdateCount("Количество запросов UPDATE не отвечает ожиданиям",0);
        assertInsertCount("Количество запросов INSERT не отвечает ожиданиям",1);

        clientExpected.setFirstName("Sergey"); // так как это поле мы обновили в БД с помощью jdbcTemplate
        placeExpected.setPlaceName("A PLACE 111"); // так как это поле мы обновили в БД с помощью jdbcTemplate

        // Удаляем проверяемого клиента и место из кэш 2-го уровня
        cacheManager.getCache("default").evict(clientExpected);
        cacheManager.getCache("default").evict(placeExpected);
        // Ожидаем удаления объектов из кеш
        try {
            TimeUnit.SECONDS.sleep(4);
        } catch (InterruptedException e) {/*пустое*/}
        int placeId = jdbcTemplate.queryForObject("select id from places where place='A PLACE 111';", Integer.class);
        Client clientActual = clientRepository.findById(clientExpected.getInn()).orElseThrow();
        Place placeActual = placeRepository.findById(placeId).orElseThrow();

        Assert.isTrue(clientExpected.deepEquals(clientActual), "Данные клиентов не совпадают!"
                + "\nожидаемые: " + clientExpected + "\nактуальные:" + clientActual);
        Assert.isTrue(placeExpected.deepEquals(placeActual),"Данные мест не совпадают!"
                + "\nожидаемые: "+placeExpected + "\nактуальные:" + placeActual);
    }

    /**
     * Тест количества транзакций добавленных в БД
     * Когда добавляем в БД определенное количество транзакций, количество записей в БД увеличивается на правильное количество
     */
    @Test
    public void checkAddRigthAmountTest() {

        // Количества до вставки
        long transactionsBefore = jdbcTemplate.queryForObject("select count(*) from transactions;", Long.class);
        long clientsBefore = jdbcTemplate.queryForObject("select count(*) from transactions;", Long.class);
        long placesBefore = jdbcTemplate.queryForObject("select count(*) from transactions;", Long.class);

        // Добавляем 4 транзакции в БД
        List<Trans> transactions = new ArrayList<>();
        for (int i=21;i<=24;i++) {
            Client client = Client.newClient("Ivan","Ivanoff","Ivanoff",(i+"1111111111").substring(0,10)).orElse(null);
            Place place = Place.newPlace("A PLACE 000000"+i).orElse(null);
            Trans transaction = Trans.newTrans(new BigDecimal(((int)1+(Math.random()*1000000))/100),"UAH","123456****1234",client,place,i).orElse(null);
            transactions.add(transaction);
        }

        // Количество добавленных
        int addedTransactions = transService.saveAll(transactions);

        // Количество после вставки
        long transactionsAfter = jdbcTemplate.queryForObject("select count(*) from transactions;", Long.class);
        long clientsAfter = jdbcTemplate.queryForObject("select count(*) from transactions;", Long.class);
        long placesAfter = jdbcTemplate.queryForObject("select count(*) from transactions;", Long.class);

        Assert.isTrue(4==addedTransactions,"transService.saveAll возвращает неправильное значение"
                +"\nожидаемое: 4" + "\nактуальное:" +addedTransactions);
        Assert.isTrue(transactionsBefore+4==transactionsAfter,"В базу добавлено неверное количество транзакций!"
                +"\nожидаемое: 4"+ "\nактуальное:" + (transactionsAfter-transactionsBefore));
        Assert.isTrue(clientsBefore+4==clientsAfter,"В базу добавлено неверное количество клиентов!"
                +"\nожидаемое: 4"+ "\nактуальное:" +(clientsAfter-clientsBefore));
        Assert.isTrue(placesBefore+4==placesAfter,"В базу добавлено неверное количество мест!"
                +"\nожидаемое: 4"+ "\nактуальное:" +(placesAfter-placesBefore));
    }
}