package com.dnsabr.vad.ukrsib.services;

import com.dnsabr.vad.ukrsib.models.*;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.Assert;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Unit-тесты StoreService
 */
@RunWith(JUnit4.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class StoreServiceUnitTests {

    private StoreService store = new StoreService();
    private int batchSize = 4;

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
        ReflectionTestUtils.setField(store, "batchSize", 4);
        ReflectionTestUtils.setField(store, "batchAmount", 100);
        ReflectionTestUtils.setField(store, "errorsBeforeTerminate", 10000);
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
     * Тест метода добавления транзакций в хранилище StoreService.add
     * Когда добавляем транзакцию в хранилище с помощью метода StoreService.add, она там находится
     */
    @Test
    public void addTest() {
        Client client = Client.newClient("Ivan","Ivanoff","Ivanoff","6161616161").orElse(null);
        Place place = Place.newPlace("A PLACE 000061").orElse(null);
        Trans transactionExpected = Trans.newTrans(new BigDecimal("10.01"),"UAH","123456****1234",client,place,60).orElse(null);
        store.add(transactionExpected);
        Trans transactionActual = ReflectionTestUtils.invokeMethod(store,"get");

        Assert.isTrue(transactionActual.deepEquals(transactionExpected)
                ,"Транзакции не совпадают\nожидаемая: "+transactionExpected+"\nактуальная:"+transactionActual);
    }

    /**
     * Тест прекращения приема новых транзакций в хранилище методом StoreService.add при достижении максимальной
     * емкости хранилища
     * Когда хранилище переполнено, в него невозможно добавить новую транзакцию с помощью метода StoreService.add
     */
    @Test
    public void addLimitTest() {
        Client client = Client.newClient("Ivan","Ivanoff","Ivanoff","6161616161").orElse(null);
        Place place = Place.newPlace("A PLACE 000061").orElse(null);
        Trans transactionExpected = Trans.newTrans(new BigDecimal("10.01"),"UAH","123456****1234",client,place,61).orElse(null);
        Thread thread = new Thread(()-> {
            for (int i=0;i<batchSize*1000;i++) {
                store.add(transactionExpected);
            }
        });
        thread.setDaemon(true);
        thread.start();
        try {
            TimeUnit.MILLISECONDS.sleep(100);
        } catch (InterruptedException e) {/*пустое*/}
        int storeSizeExpected = batchSize*100+1;
        int storeSizeActual = store.getSize();
        Assert.isTrue(storeSizeActual<=storeSizeExpected
                ,"В хранилище добавлено транзакций больше, чем может вместить\nмаксимальное:"+storeSizeExpected
                        +"\nтекущее :    " +storeSizeActual);
        thread.interrupt();
    }

    /**
     * Тест метода добавления транзакций в хранилище StoreService.add при получении неправильных данных
     * Если передать недопустимые параметры, то в хранилище не прибавится новых объектов
     */
    @Test
    public void addNullTest() {
        store.add(null);
        int storeSizeExpected = 0;
        int storeSizeActual = store.getSize();
        Assert.isTrue(storeSizeActual==storeSizeExpected
                ,"В хранилище вместо транзакции добавлен null");
    }

    /**
     * Тест метода добавления списка транзакций в хранилище StoreService.addDueToError
     * Когда добавляем список транзакций в хранилище с помощью метода StoreService.addDueToErrorTest,
     * транзакции из списка там находятся
     */
    @Test
    public void addDueToErrorTest() {
        Client client = Client.newClient("Ivan","Ivanoff","Ivanoff","6262626262").orElse(null);
        Place place = Place.newPlace("A PLACE 000062").orElse(null);
        Trans transactionExpected = Trans.newTrans(new BigDecimal("10.01"),"UAH","123456****1234",client,place,62).orElse(null);
        List<Trans> transactionsExpected = new ArrayList<>();
        transactionsExpected.add(transactionExpected);
        store.addDueToError(transactionsExpected);
        Queue<Trans> transactionsActual = (Queue<Trans>)ReflectionTestUtils.getField(store,"transactions");
        Trans transactionActual = transactionsActual.poll();

        Assert.isTrue(transactionActual.deepEquals(transactionExpected)
                ,"Транзакции не совпадают\nожидаемая: "+transactionExpected+"\nактуальная:"+transactionActual);
    }

    /**
     * Тест метода добавления списка транзакций в хранилище StoreService.addDueToError независимо от достижения
     * максимальной емкости хранилище
     * Когда добавляем список транзакций в хранилище при его переполнении с помощью метода StoreService.addDueToErrorTest,
     * транзакции из списка там находятся
     */
    @Test
    public void addDueToErrorNoLimitTest() {
        Client client = Client.newClient("Ivan","Ivanoff","Ivanoff","6363636363").orElse(null);
        Place place = Place.newPlace("A PLACE 000063").orElse(null);
        Trans transactionExpected = Trans.newTrans(new BigDecimal("10.01"),"UAH","123456****1234",client,place,63).orElse(null);
        List<Trans> transactionsExpected = new ArrayList<>();
        transactionsExpected.add(transactionExpected);
        Thread thread = new Thread(()-> {
            for (int i=0;i<150*batchSize;i++) {
                store.addDueToError(transactionsExpected);
            }
        });
        thread.setDaemon(true);
        thread.start();
        try {
            TimeUnit.MILLISECONDS.sleep(100);
        } catch (InterruptedException e) {/*пустое*/}
        int storeMinSizeExpected = 100*batchSize+1;
        int storeSizeActual = store.getSize();
        Assert.isTrue(storeSizeActual>storeMinSizeExpected
                ,"В хранилище не добавлено транзакций больше, чем оно может вместить\nмаксимальное:"
                        +storeMinSizeExpected + "\nтекущее :    "+storeSizeActual);
        thread.interrupt();
    }

    /**
     * Тест метода добавления транзакций в хранилище StoreService.addDueToError при получении неправильных данных
     * Если передать недопустимые параметры, то в хранилище не прибавится новых объектов
     */
    @Test
    public void addDueToErrorNullTest() {
        store.addDueToError(null);
        int storeSizeExpected = 0;
        int storeSizeActual = store.getSize();
        Assert.isTrue(storeSizeActual==storeSizeExpected
                ,"В хранилище что-то было добавлено, несмотря на недопустимые параметры");
    }

    /**
     * Тест метода StoreService.get - получения транзакций из очереди
     * Когда запрашиваем транзакцию из хранилища, получаем правильную транзакцию
     */
    @Test
    public void getTest() {

        Client client = Client.newClient("Ivan","Ivanoff","Ivanoff","6464646464").orElse(null);
        Place place = Place.newPlace("A PLACE 000064").orElse(null);
        Trans transactionExpected = Trans.newTrans(new BigDecimal("10.01"),"UAH","123456****1234",client,place,64).orElse(null);

        store.add(transactionExpected);
        ReflectionTestUtils.setField(store, "parserDone", true);
        Trans transactionActual = (Trans)ReflectionTestUtils.invokeMethod(store,"get");
        ReflectionTestUtils.setField(store, "parserDone", false);

        Assert.isTrue(transactionExpected.deepEquals(transactionActual), "Хранилище возвратило неверную транзакцию!"
                +"\nожидаемая: "+transactionExpected + "\nактуальная:"+transactionActual);
    }

    /**
     * Тест метода StoreService.get(List<Trans> list) - получения транзакций из очереди
     * Когда запрашиваем список транзакций из хранилища, получаем список правильных транзакций
     */
    @Test
    public void getListTest() {

        Client client = Client.newClient("Ivan","Ivanoff","Ivanoff","6565656565").orElse(null);
        Place place = Place.newPlace("A PLACE 000065").orElse(null);
        Trans transactionExpected = Trans.newTrans(new BigDecimal("10.01"),"UAH","123456****1234",client,place,65).orElse(null);
        List<Trans> transactionsExpected = new ArrayList<>();
        transactionsExpected.add(transactionExpected);
        store.add(transactionExpected);

        // Пока работает парсер хранилище не выдаст транзакции если их меньше чем batchSize
        ReflectionTestUtils.setField(store, "parserDone", true);

        List<Trans> transListActual = new ArrayList<>();
        store.get(transListActual);

        ReflectionTestUtils.setField(store, "parserDone", false);

        Assert.isTrue(transListActual.size()==1, "Хранилище возвратило неверное количество транзакций!"
                +"\nожидаемое: 1" + "\nактуальное:"+transListActual.size());

        Trans transactionActual = transListActual.get(0);

        Assert.isTrue(transactionExpected.deepEquals(transactionActual), "Хранилище возвратило неверную транзакцию!"
                +"\nожидаемая: "+transactionExpected.toString() + "\nактуальная:"+transactionActual.toString());
    }

    /**
     * Тест метода StoreService.get - получения транзакций из очереди, при передаче методу некорректных данных
     * Когда запрашиваем список транзакций из хранилища, но передаем недопустимые данные, объектов в хранилище
     * не становится меньше
     */
    @Test
    public void getNullTest() {

        Client client = Client.newClient("Ivan","Ivanoff","Ivanoff","6767676767").orElse(null);
        Place place = Place.newPlace("A PLACE 000067").orElse(null);
        Trans transactionExpected = Trans.newTrans(new BigDecimal("10.01"),"UAH","123456****1234",client,place,67).orElse(null);
        store.add(transactionExpected);

        int storeSizeExpected = store.getSize();
        List<Trans> list = null;
        store.get(list);
        int storeSizeActual = store.getSize();
        Assert.isTrue(storeSizeExpected==storeSizeActual
                ,"Хранилище вернуло транзакции в null. Данные потеряны! Количество транзакций в хранилище не " +
                        "соответствует ожиданиям\nожидаемое: "+storeSizeExpected+ "\nактуальное:"+storeSizeActual);
    }

    /**
     * Тест метода StoreService.getSize
     * Когда запрашиваем количество объектов в хранилище, получаем правильные данные
     */
    @Test
    public void getSizeTest() {
        Assert.isTrue(store.getSize()==0
                ,"В хранилище находится неверное количество транзакций\nожидаемое: 0\nактуальное:"+store.getSize());
        Client client = Client.newClient("Ivan","Ivanoff","Ivanoff","6565656565").orElse(null);
        Place place = Place.newPlace("A PLACE 000065").orElse(null);
        Trans transactionExpected = Trans.newTrans(new BigDecimal("10.01"),"UAH","123456****1234",client,place,66).orElse(null);
        store.add(transactionExpected);
        Assert.isTrue(store.getSize()==1
                ,"В хранилище находится неверное количество транзакций\nожидаемое: 1\nактуальное:"+store.getSize());
        store.add(transactionExpected);
        Assert.isTrue(store.getSize()==2
                ,"В хранилище находится неверное количество транзакций\nожидаемое: 2\nактуальное:"+store.getSize());
    }

    /**
     * Тест метода StoreService.parserDone
     * Когда сообщаем хранилищу о завершении работы парсера, хранилище устанавливает флаг в состояние true
     */
    @Test
    public void parserDoneTest() {
        boolean parserDone = (boolean)ReflectionTestUtils.getField(store,"parserDone");
        Assert.isTrue(!parserDone
                ,"Неверное значение поля StoreService.parserDone\nожидаемое: false\nактуальное:true");
        store.parserDone();
        parserDone = (boolean)ReflectionTestUtils.getField(store,"parserDone");
        Assert.isTrue(parserDone
                ,"Неверное значение поля StoreService.parserDone\nожидаемое: true\nактуальное:false");
    }

    /**
     * Тест метода StoreService.isParserDone
     * Когда запрашиваем работает ли еще парсер, получаем правильный результат
     */
    @Test
    public void isParserDoneTest() {
        boolean parserDoneExpected = (boolean)ReflectionTestUtils.getField(store,"parserDone");
        boolean parserDoneActual = store.isParserDone();
        Assert.isTrue(parserDoneActual==parserDoneExpected
                ,"Неверное значение вернул метод StoreService.isParserDone\nожидаемое: "+parserDoneExpected
                        +"\nактуальное:"+parserDoneActual);
        store.parserDone();
        parserDoneExpected = (boolean)ReflectionTestUtils.getField(store,"parserDone");
        parserDoneActual = store.isParserDone();
        Assert.isTrue(parserDoneActual==parserDoneExpected
                ,"Неверное значение вернул метод StoreService.isParserDone\nожидаемое: "+parserDoneExpected
                        +"\nактуальное:"+parserDoneActual);
    }

    /**
     * Тест метода StoreService.doTerminate
     * Когда сигнализируем хранилищу о необходимости закрыться, все флаги состояния устанавливаются правильно
     * и хранилище очищается от данных
     */
    @Test
    public void doTerminateTest() {

        ReflectionTestUtils.invokeMethod(store,"doTerminate");

        boolean parserDone = (boolean)ReflectionTestUtils.getField(store,"parserDone");
        boolean terminated = (boolean)ReflectionTestUtils.getField(store,"terminated");
        int transactionsSize = ((Queue<Trans>)ReflectionTestUtils.getField(store,"transactions")).size();

        Assert.isTrue(parserDone
                ,"Неправильно отработал метод StoreService.doTerminate(). Неверное значение поля " +
                        "StoreService.parserDone\nожидаемое: true\nактуальное:false");

        Assert.isTrue(terminated
                ,"Неправильно отработал метод StoreService.doTerminate(). Неверное значение поля " +
                        "StoreService.terminated\nожидаемое: true\nактуальное:false");

        Assert.isTrue(transactionsSize==0
                ,"Неправильно отработал метод StoreService.doTerminate(). Неверный размер коллекции " +
                        "StoreService.transactions\nожидаемый: 0\nактуальный:"+transactionsSize);
    }

    /**
     * Тест метода StoreService.isTerminated
     * Когда запрашиваем закрыто ли хранилище, получаем правильный результат
     */
    @Test
    public void isTerminatedTest() {

        ReflectionTestUtils.setField(store,"terminated",true);
        Assert.isTrue(store.isTerminated()
                ,"Неправильное значение вернул метод StoreService.isTerminated\nожидаемое: true\nактуальное:false");
        ReflectionTestUtils.setField(store,"terminated",false);
        Assert.isTrue(!store.isTerminated()
                ,"Неправильное значение вернул метод StoreService.isTerminated\nожидаемое: false\nактуальное:true");
    }
}
