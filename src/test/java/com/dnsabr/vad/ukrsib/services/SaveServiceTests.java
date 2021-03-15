package com.dnsabr.vad.ukrsib.services;

import com.dnsabr.vad.ukrsib.models.*;
import com.dnsabr.vad.ukrsib.repository.*;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoSettings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.*;
import org.springframework.stereotype.Service;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.Assert;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Интеграционные тесты сервиса SaveService
 * Создан наследник TransService и переопределен его метод saveAll для считывания данных полученных от SaveService и
 *  подключен в конфигурации для этого класса вместо оригинального TransService
 */
@RunWith(SpringRunner.class)
@SpringBootTest
@ActiveProfiles({"mock"})
public class SaveServiceTests {

    @Value("${spring.jpa.properties.hibernate.jdbc.batch_size}")
    private int batchSize;
    @Value("${spring.jpa.properties.app.try.attempts}")
    private int attempts;
    @Autowired
    private TransServiceT transService;
    @Autowired
    private StoreService store;
    @Autowired
    private SaveService saveService;

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
     * Тест взаимодействия SaveService и StoreService
     * Когда работает ParserService и в хранилище нет транзакций, SaveService не завершает работу
     * Когда в хранилище находится достаточное количество транзакций, SaveService забирает их и передает в TransService
     *  для добавления в БД
     * Когда в хранилище не находится достаточное количество транзакций, SaveService забирает имеющиеся и ожидает
     *  пока появится достаточное количество или завершит работу ParserService
     * Когда ParserService завершил работу, SaveService передает в TransService на обработку то количество транзакций,
     *  которое есть
     * Когда в хранилище нет транзакций и ParserService завершил работу, SaveService завершает работу
     */
    @Test
    public void getAllFromStoreAndWaitingForMoreWhileParserWorkingAndFinishWhenParserFinishedTest() {

        ReflectionTestUtils.setField(store, "parserDone", false);

        // Когда работает ParserService и в хранилище нет транзакций, SaveService не завершает работу
        Thread saver = new Thread(saveService);
        saver.start();
        try {
            TimeUnit.MILLISECONDS.sleep(100);
        } catch (InterruptedException e) {/*пустое*/}
        Assert.isTrue(saver.isAlive()
                ,"SaveService завершается ранее, чем запланировано, не ожидая завершения ParseService");

        // Добавляем batchSize+1 транзакций в хранилище
        Client client = Client.newClient("Ivan","Ivanoff","Ivanoff","1111111111").orElse(null);
        Place place = Place.newPlace("A PLACE 0").orElse(null);
        ReflectionTestUtils.setField(place,"id",1);
        for (int i=1;i<=batchSize;i++) {
            Trans transaction = Trans.newTrans(new BigDecimal("10.01"),"UAH","123456****1234",client,place,i).orElse(null);
            store.add(transaction);
        }
        try {
            TimeUnit.MILLISECONDS.sleep(100);
        } catch (InterruptedException e) {/*пустое*/}
        Assert.isTrue(saver.isAlive()
                ,"SaveService завершается ранее, чем запланировано, не ожидая завершения ParseService");

        // Когда в хранилище находится достаточное количество транзакций, SaveService забирает их и передает в TransService
        // для добавления в БД
        int amountActual = transService.amount;
        Assert.isTrue(amountActual==batchSize, "TransService получил от SaveService неверное количество" +
                " транзакций!" +"\nожидаемое: "+batchSize + "\nактуальное:"+amountActual);

        // Обнуляем transService.amount
        transService.amount = 0;

        // Когда в хранилище не находится достаточное количество транзакций, SaveService забирает имеющиеся и ожидает
        // пока появится достаточное количество или завершит работу ParserService
        Trans transaction = Trans.newTrans(new BigDecimal("10.01"),"UAH","123456****1234",client,place,1).orElse(null);
        store.add(transaction);
        try {
            TimeUnit.MILLISECONDS.sleep(100);
        } catch (InterruptedException e) {/*пустое*/}
        Assert.isTrue(store.getSize()==0, "В хранилище находится неверное количество транзакций!"
                +"\nожидаемое: 0" + "\nактуальное:"+store.getSize());
        amountActual = transService.amount;
        Assert.isTrue(amountActual==0, "TransService получил от SaveService неверное количество транзакций!"
                +"\nожидаемое: 0" + "\nактуальное:"+amountActual);
        Assert.isTrue(saver.isAlive()
                ,"SaveService завершается ранее, чем запланировано, не ожидая завершения ParseService");

        // Когда ParserService завершил работу, SaveService передает в TransService на обработку то количество транзакций,
        // которое есть
        store.parserDone();
        try {
            TimeUnit.MILLISECONDS.sleep(300);
        } catch (InterruptedException e) {/*пустое*/}

        amountActual = transService.amount;
        Assert.isTrue(amountActual==1, "TransService получил от SaveService неверное количество транзакций!"
                +"\nожидаемое: 1"+ "\nактуальное:"+amountActual);

        Assert.isTrue(store.getSize()==0,"SaveService забрал не все данные из хранилища!");

        // Когда в хранилище нет транзакций и ParserService завершил работу, SaveService завершает работу
        Assert.isTrue(!saver.isAlive(),"SaveService не завершается после завершения ParseService " +
                "и при отсутствии данных в хранилище!");
    }

    /**
     * Тест остановки SaveService при закрытии хранилища
     * Когда хранилище закрывается, SaveService прекращает работу
     */
    @Test
    public void stopIfStoreIsClosedTest() {

        // Запускаем поток SaveService и ждем немного
        Thread saver = new Thread(saveService);
        saver.start();
        try {
            TimeUnit.MILLISECONDS.sleep(100);
        } catch (InterruptedException e) {/*пустое*/}

        Assert.isTrue(saver.isAlive()
                ,"Сервис разбора входящего файла завершился самопроизвольно");

        // Останавливаем хранилище и ждем немного
        ReflectionTestUtils.setField(store,"terminated", true);
        try {
            TimeUnit.MILLISECONDS.sleep(100);
        } catch (InterruptedException e) {/*пустое*/}

        Assert.isTrue(!saver.isAlive()
                ,"Сервис сохранения данных в БД не завершился при закрытом хранилище");
    }

    /**
     * Тест метода SaveService.save
     * При вызове этого метода, поле amount переопределенного класса TransService должно содержать значение
     * размера списка переданного методу save, а поле calls - значение количества вызовов
     *
     * Когда SaveService достигает максимального количества неудачных попыток добавить пакет в БД,
     *  попытки добавить этот пакет прекращаются
     */
    @Test
    public void amountOfUnsuccessfulAttemptsTest() {

        // Проверяем достижение SaveService значения максимального количества последовательных неудачных попыток
        // добавления транзакций. Для этого передаем список транзакций у которых значение поля id==-1

        // Устанавливаем количество попыток для пакета
        ReflectionTestUtils.setField(saveService,"attempts", 7);
        // Установка количества ошибок прежде завершения приложения = 0, иначе SaveService будет брать,
        // пытаться добавить в БД, возвращать в хранилище по кругу одни и те же транзакции с id==-1
        ReflectionTestUtils.setField(store, "errorsBeforeTerminate", 0);

        transService.calls = 0;

        Thread saver = new Thread(saveService);
        saver.start();

        Client client = Client.newClient("Ivan","Ivanoff","Ivanoff","5151515151").orElse(null);
        Place place = Place.newPlace("A PLACE 000051").orElse(null);
        for (int i=51;i<=54;i++) {
            Trans transaction = Trans.newTrans(new BigDecimal("10.01"),"UAH","123456****1234",client,place,i).orElse(null);
            ReflectionTestUtils.setField(transaction,"id", -1);
            store.add(transaction);
        }

        try {
            TimeUnit.MILLISECONDS.sleep(100);
        } catch (InterruptedException e) {/*пустое*/}

        int callsExpected = 7;
        int callsActual = transService.calls;
        Assert.isTrue(callsActual==callsExpected
                ,"TransService был вызван неверное количество раз!"
                        +"\nожидаемое: "+callsExpected + "\nактуальное:"+callsActual);
    }

    /**
     * Наследник TransService с переопределенным методом saveAll подключен в конфигурации для этого тестового класса
     * вместо оригинального TransService для считывания данных полученных от SaveService
     */
    @Service
    public static class TransServiceT extends TransService {

        int calls = 0; // Количество удачных вызовов метода saveAll
        int amount;    // Количество переданных транзакций
        public int saveAll(List<Trans> transactions) {
            if (null==transactions || transactions.isEmpty()) {
                calls++;
                return 0;
            }
            // Специальное условие для одного из тестов
            if (transactions.get(0).getId()==-1) {
                calls++;
                return -1;
            }
            amount = transactions.size();
            calls++;
            return amount;
        }
    }

    /**
     * Отдельная конфигурация с заглушками вместо неиспользуемых компонентов приложения и заменой TransService
     * на его наследника с переопределенным методом saveAll
     */
    @Configuration
    @Profile({"mock"})
    static class ContextConfiguration {

        @Bean
        public EntityManagerFactory getEntityManagerFactory() {
            @MockitoSettings
            EntityManagerFactory entityManagerFactory = Mockito.mock(EntityManagerFactory.class);
            return entityManagerFactory;
        }
        @Bean
        public EntityManager getEntityManager() {
            @MockitoSettings
            EntityManager entityManager = Mockito.mock(EntityManager.class);
            return entityManager;
        }
        @Bean
        public TransServiceT transService() {
            TransServiceT transService = new TransServiceT();
            return transService;
        }
        @Bean
        public TransRepository transRepository() {
            @MockitoSettings
            TransRepository transRepository = Mockito.mock(TransRepository.class);
            return transRepository;
        }
        @Bean
        public ClientRepository clientRepository() {
            @MockitoSettings
            ClientRepository clientRepository = Mockito.mock(ClientRepository.class);
            return clientRepository;
        }
        @Bean
        public PlaceRepository placeRepository() {
            @MockitoSettings
            PlaceRepository placeRepository = Mockito.mock(PlaceRepository.class);
            return placeRepository;
        }
        @Bean
        public StoreService storeService() {
            return new StoreService();
        }
        @Bean
        public SaveService saveService() {
            return new SaveService();
        }
    }
}