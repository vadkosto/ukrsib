package com.dnsabr.vad.ukrsib.services;

import com.dnsabr.vad.ukrsib.models.Trans;
import com.dnsabr.vad.ukrsib.utils.Utils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.Assert;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Интеграционные тесты сервиса ParseService
 */
@RunWith(SpringRunner.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@SpringBootTest
@ActiveProfiles({"mock"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class ParseServiceTests {

    @Value("${spring.jpa.properties.hibernate.jdbc.batch_size}")
    private int batchSize;
    @Value("${spring.jpa.properties.app.source.file}")
    private String fileName;
    @Autowired
    private StoreService store;
    @Autowired
    private ParseService parseService;

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
     * Тест передачи данных в хранилище
     * Когда ParseService передает данные в хранилище, там оказываются правильные данные в ожидаемом количестве
     */
    @Test
    public void transactionsCountAndDataAccuracyTest() {

        int amountExpected = 3000;
        Thread parser = new Thread(parseService);
        parser.start();

        // Забираем все транзакции из хранилища
        List<Trans> transActual = new ArrayList<>(amountExpected);
        List<Trans> list = new ArrayList<>(batchSize);
        while (!store.isParserDone() || 0<store.getSize()) {
            store.get(list);
            transActual.addAll(list);
            list.clear();
        }
        int amountActual = transActual.size();

        Assert.isTrue(amountActual==amountExpected,"ParseService передал в хранилище неверное " +
                "количество транзакций!" +"\nожидаемое: "+amountExpected + "\nактуальное:"+amountActual);

        List<Trans> transExpected = new ArrayList<>(amountExpected);
        Utils.fillLists(transExpected,amountExpected);

        for (int i = 0; i<transExpected.size(); i++) {
            Assert.isTrue(transExpected.get(i).deepEquals(transActual.get(i))
                    ,"Данные транзакций переданных ParseService в хранилище не совпадают!\nожидаемые: "
                            +transExpected.get(i)+"\nактуальные:" +transActual.get(i));
        }
    }

    /**
     * Тест принудительной остановки парсера при закрытом хранилище
     * Когда хранилище закрыто, ParseService прекращает работу
     */
    @Test
    public void stopIfStoreIsClosedTest() {
        // Останавливаем хранилище
        ReflectionTestUtils.setField(store,"terminated", true);
        int storeSizeExpected = store.getSize();
        // Запускаем сервис разбора входящего файла
        Thread parser = new Thread(parseService);
        parser.start();
        try {
            TimeUnit.MILLISECONDS.sleep(200);
        } catch (InterruptedException e) {/*пустое*/}
        Assert.isTrue(!parser.isAlive()
                ,"Сервис разбора входящего файла не завершился при закрытом хранилище");
        int storeSizeActual = store.getSize();
        Assert.isTrue(storeSizeActual==storeSizeExpected
                ,"В хранилище были переданы данные после его закрытия - увеличилось количество\nожидаемое: "
                        +storeSizeExpected+"\nактуальное:" +storeSizeActual);
    }

    /**
     * Отдельная конфигурация для тестов класса
     */
    @Configuration
    @Profile({"mock"})
    static class ContextConfiguration {

        @Bean
        public ParseService parseService() {
            return new ParseService();
        }
        @Bean
        public StoreService storeService() {
            return new StoreService();
        }
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