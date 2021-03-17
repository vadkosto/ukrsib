package com.dnsabr.vad.ukrsib.services;

import com.dnsabr.vad.ukrsib.models.Client;
import com.dnsabr.vad.ukrsib.models.Place;
import com.dnsabr.vad.ukrsib.models.Trans;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.Assert;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Интеграционные тесты сервиса StoreService
 */
@RunWith(SpringRunner.class)
@SpringBootTest
@ActiveProfiles({"mock"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class StoreServiceTests {
    @Value("${spring.jpa.properties.hibernate.jdbc.batch_size}")
    private int batchSize;
    @Value("${spring.jpa.properties.app.errors.count.before.terminate}")
    private int shutdownAfter;
    @Autowired
    private StoreService store;

    /**
     * Метод для выполнения действий перед каждым тестом класса
     * Приведение состояния необходимых для тестов полей сервисов в состояние как перед первым запуском
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
     * Тестирование добавления и извлечения из хранилища различного количества транзакций
     * Когда добавляем транзакции, хранилище содержит верное количество транакций
     * Когда запрашиваем транзакции, хранилище возвращает верное количество транзакций
     */
    @Test
    public void addAndGetTest() {

        // Добавляем 1 транзакцию в хранилище
        Client client = Client.newClient("Ivan","Ivanoff","Ivanoff","1234567899").orElse(null);
        Place place = Place.newPlace("A PLACE 0").orElse(null);
        Trans transactionExpected = Trans.newTrans(new BigDecimal("10.01"),"UAH","123456****1234",client,place,1).orElse(null);
        store.add(transactionExpected);
        Assert.isTrue(store.getSize()==1,"Хранилище содержит неверное количество транзакций!"
                +"\nожидаемое: 1" + "\nактуальное:"+store.getSize());


        List<Trans> transListActual = new ArrayList<>(1);
        // Имитируем остановку парсера, так как пока он работает хранилище не выдаст транзакции если их меньше чем batchSize
        ReflectionTestUtils.setField(store, "parserDone", true);
        // Запрашиваем транзакции из хранилища
        store.get(transListActual);

        Assert.isTrue(transListActual.size()==1, "Хранилище возвратило неверное количество транзакций!"
                +"\nожидаемое: 1" + "\nактуальное:"+transListActual.size());
        Trans transactionActual = transListActual.get(0);
        Assert.isTrue(transactionExpected.deepEquals(transactionActual), "Хранилище возвратило неверную транзакцию!"
                +"\nожидаемая: "+transactionExpected + "\nактуальная:"+transactionActual);

        // Добавляем batchSize+1 транзакций в хранилище
        List<Trans> transListExpected = new ArrayList<>();
        for (int i=1;i<=batchSize+1;i++) {
            transactionExpected = Trans.newTrans(new BigDecimal("10.01"),"UAH","123456****1234",client,place,i).orElse(null);
            store.add(transactionExpected);
            transListExpected.add(transactionExpected);
        }

        // Когда запрашиваем транзакции из хранилища, получаем транзакции в количестве batchSize
        transListActual.clear();
        store.get(transListActual);
        Assert.isTrue(transListActual.size()==batchSize, "Хранилище возвратило неверное количество транзакций!"
                +"\nожидаемое: "+batchSize + "\nактуальное:"+transListActual.size());
        for (int i = 0; i<transListActual.size(); i++) {
            Assert.isTrue(transListExpected.get(i).deepEquals(transListActual.get(i))
                    ,"Данные транзакций переданных в хранилище не совпадают!\nожидаемая: "
                            +transListExpected.get(i)+"\nактуальная:" +transListActual.get(i));
        }

        // Проверяем правильное ли количество транзакций осталось в хранилище
        Assert.isTrue(store.getSize()==1,"В хранилище осталось неверное количество транзакций!"
                +"\nожидаемое: 1" + "\nактуальное:"+store.getSize());

        // Запрашиваем оставшуюся транзакцию
        transListActual.clear();
        store.get(transListActual);

        Assert.isTrue(transListActual.size()==1, "Хранилище возвратило неверное количество транзакций!"
                +"\nожидаемое: 1" + "\nактуальное:"+transListActual.size());
        for (int i = 0; i<transListActual.size(); i++) {
            Assert.isTrue(transListExpected.get(i+batchSize).deepEquals(transListActual.get(i))
                    ,"Данные транзакций переданных в хранилище не совпадают!\nожидаемая: "
                            +transListExpected.get(i+batchSize)+"\nактуальная:" +transListActual.get(i));
        }

        // Когда добавляем в хранилище транзакции списком, количество транзакций в хранилище увеличивается
        // на количество элементов списка
        int amountActual = store.getSize();
        int amountExpected = transListExpected.size();
        store.addDueToError(transListExpected);
        amountActual = store.getSize()-amountActual;
        Assert.isTrue(amountActual==amountExpected, "Хранилище содержит неверное количество транзакций!"
                +"\nожидаемое: "+amountExpected + "\nактуальное:"+amountActual);
    }

    /**
     * Тестирование счетчика ошибок
     * Когда счетчик ошибок превышает допустимое установленное значение ошибок, хранилище закрывается
     * и прекращает принимать и выдавать данные
     */
    @Test
    public void countErrorsBeforeShutdownTest() {

        ReflectionTestUtils.setField(store,"countErrorsBeforeShutdown", new AtomicInteger(shutdownAfter-5));

        List<Trans> list = new ArrayList<>(1);
        Client client = Client.newClient("Ivan","Ivanoff","Ivanoff","3030303030").orElse(null);
        Place place = Place.newPlace("A PLACE 000030").orElse(null);
        Trans transactionExpected = Trans.newTrans(new BigDecimal("10.01"),"UAH","123456****1234",client,place,1).orElse(null);
        list.add(transactionExpected);

        // Когда обращаемся к методу StoreService.addDueToError(List), счетчик должен увеличиться на единицу
        // или на большее значение, если конкурентная очередь не может принять новые данные
        int countErrorsBeforeShutdown = ((AtomicInteger) ReflectionTestUtils.getField(store,"countErrorsBeforeShutdown")).intValue();
        store.addDueToError(list);
        int countErrorsBeforeShutdownExpected = countErrorsBeforeShutdown+1;
        int countErrorsBeforeShutdownActual = ((AtomicInteger) ReflectionTestUtils.getField(store,"countErrorsBeforeShutdown")).intValue();

        Assert.isTrue(countErrorsBeforeShutdownActual>=countErrorsBeforeShutdownExpected
                , "Счетчик неудачных пакетов не достиг ожидаемого значения!" +"\nожидаемое: "
                        +countErrorsBeforeShutdownExpected + "\nактуальное:"+countErrorsBeforeShutdownActual);

        // Когда счетчик ошибок превышает допустимое установленное значение ошибок, хранилище закрывается
        // и прекращает принимать и выдавать данные
        for (Thread thread : new Thread[10]) {
            (thread=new Thread(() -> store.addDueToError(list))).start();
        }
        try {
            TimeUnit.MILLISECONDS.sleep(300);
        } catch (InterruptedException e) {/*пустое*/}

        Assert.isTrue(store.isTerminated(),"Хранилище продолжает работу после достижения счетчиком ошибок " +
                "countErrorsBeforeShutdown порогового значения");

        int storeSizeExpected = store.getSize();
        store.add(transactionExpected);
        int storeSizeActual = store.getSize();

        Assert.isTrue(storeSizeActual==storeSizeExpected,"Хранилище продолжает принимать данные " +
                "после достижения счетчиком ошибок countErrorsBeforeShutdown порогового значения");

        store.get(list);
        storeSizeActual = store.getSize();

        Assert.isTrue(storeSizeActual==storeSizeExpected,"Хранилище продолжает выдавать данные " +
                "после достижения счетчиком ошибок countErrorsBeforeShutdown порогового значения");

        ReflectionTestUtils.setField(store,"countErrorsBeforeShutdown", new AtomicInteger(0));
    }

    /**
     * Отдельная конфигурация для тестового класса
     */
    @Configuration
    @Profile({"mock"})
    static class ContextConfiguration {

        @Bean
        public StoreService storeService() {
            return new StoreService();
        }
    }
}