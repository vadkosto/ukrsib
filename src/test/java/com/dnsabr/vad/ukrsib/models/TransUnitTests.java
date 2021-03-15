package com.dnsabr.vad.ukrsib.models;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.springframework.test.util.ReflectionTestUtils;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.Map;

/**
 * Unit-тесты Trans
 */
@RunWith(JUnit4.class)
public class TransUnitTests {

    private Client client;
    private Place place;

    /**
     * Метод для выполнения действий перед каждым тестом класса
     */
    @Before
    public void setUp() throws Exception {
        client = Client.newClient("Ivan","Ivanoff","Ivanoff","0123456789").orElse(null);
        place = Place.newPlace("A PLACE 0").orElse(null);
    }

    /**
     * Метод для выполнения действий после каждого теста класса
     */
    @After
    public void tearDown() throws Exception {
    }

    /**
     * Тест создания объекта методом Trans.newTrans с валидными параметрами
     * Если передать допустимые параметры, то будет создан новый объект класса Trans
     */
    @Test
    public void newTransTest() {
        Trans transaction = Trans.newTrans(new BigDecimal("1"),"UAH","123456****1234",client,place,1).orElse(null);
        Assert.assertNotNull("Транзакция не была создана, хотя все данные правильные",transaction);
    }

    /**
     * Тест создания объекта методом Trans.newTrans с не валидными параметрами
     * Если передать недопустимые параметры, то не будет создан новый объект класса Trans
     */
    @Test
    public void newTransNullTest() {
        Trans transaction = Trans.newTrans(null,"UAH","123456****1234",client,place,1).orElse(null);
        Assert.assertNull("Транзакция была создана, хотя ожидался null, так как сумма не может быть null",transaction);

        transaction = Trans.newTrans(new BigDecimal("0"),"UAH","123456****1234",client,place,1).orElse(null);
        Assert.assertNull("Транзакция была создана, хотя ожидался null, так как сумма должна быть > 0",transaction);

        transaction = Trans.newTrans(new BigDecimal("1"),null,"123456****1234",client,place,1).orElse(null);
        Assert.assertNull("Транзакция была создана, хотя ожидался null, так как валюта не может быть null",transaction);

        transaction = Trans.newTrans(new BigDecimal("1")," ","123456****1234",client,place,1).orElse(null);
        Assert.assertNull("Транзакция была создана, хотя ожидался null, так как не указана валюта",transaction);

        transaction = Trans.newTrans(new BigDecimal("1"),"UAH",null,client,place,1).orElse(null);
        Assert.assertNull("Транзакция была создана, хотя ожидался null, так как карта не может быть null",transaction);

        transaction = Trans.newTrans(new BigDecimal("1"),"UAH"," ",client,place,1).orElse(null);
        Assert.assertNull("Транзакция была создана, хотя ожидался null, так как не указана карта",transaction);

        transaction = Trans.newTrans(new BigDecimal("1"),"UAH","123456****1234",null,place,1).orElse(null);
        Assert.assertNull("Транзакция была создана, хотя ожидался null, так как клиент не может быть null",transaction);

        transaction = Trans.newTrans(new BigDecimal("1"),"UAH","123456****1234",client,null,1).orElse(null);
        Assert.assertNull("Транзакция была создана, хотя ожидался null, так как место не может быть null",transaction);
    }

    /**
     * Тест сохранения данных о транзакции в Map<id,serial>
     * Если создан новый объект класса Trans, то его id и serial содержатся в Trans.transactions
     */
    @Test
    public void transactionsListContainsTest() {
        Trans transaction = Trans.newTrans(new BigDecimal("1"),"UAH","123456****1234",client,place,15).orElse(null);
        Map<Long,Integer> transactions = (Map<Long,Integer>)ReflectionTestUtils.getField(Trans.class,"transactions");
        Assert.assertTrue("Метод Trans.newTrans не сохранил id в Map для последующего сравнения", transactions.containsKey(transaction.getId()));
        Assert.assertTrue("Метод Trans.newTrans не сохранил serial в Map для последующего сравнения", 15==transactions.get(transaction.getId()));

    }

    /**
     * Тест геттера для id
     * Когда запрашиваем id транзакции, получаем правильный id транзакции
     */
    @Test
    public void getIdTest() {
        Trans transaction = Trans.newTrans(new BigDecimal("1"),"UAH","123456****1234",client,place,1).orElse(null);
        Assert.assertEquals("Метод Trans.getId вернул неправильный id транзакции",12119887295L,transaction.getId());
    }

    /**
     * Тест геттера для amount
     * Когда запрашиваем сумму транзакции, получаем правильную сумму транзакции
     */
    @Test
    public void getAmountTest() {
        Trans transaction = Trans.newTrans(new BigDecimal("110.75"),"UAH","123456****1234",client,place,1).orElse(null);
        Assert.assertEquals("Метод Trans.getAmount вернул неправильную сумму транзакции"
                ,new BigDecimal("110.75"),transaction.getAmount());
    }

    /**
     * Тест сеттера для amount с валидными параметрами
     * Если передать допустимые параметры, то будет изменена сумма транзакции
     */
    @Test
    public void setAmountTest() {
        Trans transaction = Trans.newTrans(new BigDecimal("1"),"UAH","123456****1234",client,place,1).orElse(null);
        transaction.setAmount(new BigDecimal("110.75"));
        Assert.assertEquals("Метод Trans.setAmount не установил правильную сумму транзакции"
                ,new BigDecimal("110.75"),transaction.getAmount());
    }

    /**
     * Тест сеттера для amount с не валидными параметрами
     * Если передать недопустимые параметры, то не будет изменена сумма транзакции
     */
    @Test
    public void setAmountNullTest() {
        Trans transaction = Trans.newTrans(new BigDecimal("1"),"UAH","123456****1234",client,place,1).orElse(null);
        transaction.setAmount(null);
        Assert.assertEquals("Метод Trans.setAmount установил недопустимую сумму транзакции - null"
                ,new BigDecimal("1"),transaction.getAmount());

        transaction.setAmount(new BigDecimal("0"));
        Assert.assertEquals("Метод Trans.setAmount установил недопустимую сумму транзакции <= 0"
                ,new BigDecimal("1"),transaction.getAmount());
    }

    /**
     * Тест геттера для currency
     * Когда запрашиваем валюту транзакции, получаем правильную валюту транзакции
     */
    @Test
    public void getCurrencyTest() {
        Trans transaction = Trans.newTrans(new BigDecimal("110.75"),"UAH","123456****1234",client,place,1).orElse(null);
        Assert.assertEquals("Метод Trans.getCurrency вернул неправильную валюту транзакции"
                ,"UAH",transaction.getCurrency());
    }

    /**
     * Тест сеттера для currency с валидными параметрами
     * Если передать допустимые параметры, то будет изменена валюта транзакции
     */
    @Test
    public void setCurrencyTest() {
        Trans transaction = Trans.newTrans(new BigDecimal("110.75"),"UAH","123456****1234",client,place,1).orElse(null);
        transaction.setCurrency("USD");
        Assert.assertEquals("Метод Trans.setCurrency не заменил валюту транзакции"
                ,"USD",transaction.getCurrency());
    }

    /**
     * Тест сеттера для currency с не валидными параметрами
     * Если передать недопустимые параметры, то не будет изменена валюта транзакции
     */
    @Test
    public void setCurrencyNullTest() {
        Trans transaction = Trans.newTrans(new BigDecimal("1"),"UAH","123456****1234",client,place,1).orElse(null);
        transaction.setCurrency(null);
        Assert.assertEquals("Метод Trans.setCurrency установил недопустимую валюту транзакции - null"
                ,"UAH",transaction.getCurrency());

        transaction.setCurrency(" ");
        Assert.assertEquals("Метод Trans.setCurrency установил недопустимую валюту транзакции - пустое значение"
                ,"UAH",transaction.getCurrency());
    }

    /**
     * Тест геттера для card
     * Когда запрашиваем карту транзакции, получаем правильную карту транзакции
     */
    @Test
    public void getCardTest() {
        Trans transaction = Trans.newTrans(new BigDecimal("110.75"),"UAH","123456****1234",client,place,1).orElse(null);
        Assert.assertEquals("Метод Trans.getCard вернул неправильную карту транзакции"
                ,"123456****1234",transaction.getCard());
    }

    /**
     * Тест сеттера для card с валидными параметрами
     * Если передать допустимые параметры, то будет изменена карта транзакции
     */
    @Test
    public void setCardTest() {
        Trans transaction = Trans.newTrans(new BigDecimal("110.75"),"UAH","123456****1234",client,place,1).orElse(null);
        transaction.setCard("654321****4321");
        Assert.assertEquals("Метод Trans.setCard не заменил карту транзакции"
                ,"654321****4321",transaction.getCard());
    }

    /**
     * Тест сеттера для card с не валидными параметрами
     * Если передать недопустимые параметры, то не будет изменена карта транзакции
     */
    @Test
    public void setCardNullTest() {
        Trans transaction = Trans.newTrans(new BigDecimal("1"),"UAH","123456****1234",client,place,1).orElse(null);
        transaction.setCard(null);
        Assert.assertEquals("Метод Trans.setCard установил недопустимую карту транзакции - null"
                ,"123456****1234",transaction.getCard());

        transaction.setCard(" ");
        Assert.assertEquals("Метод Trans.setCard установил недопустимую карту транзакции - пустое значение"
                ,"123456****1234",transaction.getCard());
    }

    /**
     * Тест геттера для client
     * Когда запрашиваем клиента транзакции, получаем правильного клиента транзакции
     */
    @Test
    public void getClientTest() {
        Trans transaction = Trans.newTrans(new BigDecimal("110.75"),"UAH","123456****1234",client,place,1).orElse(null);
        Assert.assertTrue("Метод Trans.getClient вернул неправильного клиента транзакции"
                ,client.deepEquals(transaction.getClient()));
    }

    /**
     * Тест сеттера для client с валидными параметрами
     * Если передать допустимые параметры, то будет изменен клиент транзакции
     */
    @Test
    public void setClientTest() {
        Trans transaction = Trans.newTrans(new BigDecimal("110.75"),"UAH","123456****1234",client,place,1).orElse(null);
        Client client1 = Client.newClient("Ivan","Ivanoff","Ivanoff","1111111111").orElse(null);
        transaction.setClient(client1);
        Assert.assertEquals("Метод Trans.setClient не заменил клиента транзакции"
                ,client1,transaction.getClient());
    }

    /**
     * Тест сеттера для client с не валидными параметрами
     * Если передать недопустимые параметры, то не будет изменен клиент транзакции
     */
    @Test
    public void setClientNullTest() {
        Trans transaction = Trans.newTrans(new BigDecimal("1"),"UAH","123456****1234",client,place,1).orElse(null);
        transaction.setClient(null);
        Assert.assertEquals("Метод Trans.setClient установил недопустимое значение клиента - null"
                ,client,transaction.getClient());
    }

    /**
     * Тест геттера для place
     * Когда запрашиваем место транзакции, получаем правильное место транзакции
     */
    @Test
    public void getPlaceTest() {
        Trans transaction = Trans.newTrans(new BigDecimal("110.75"),"UAH","123456****1234",client,place,1).orElse(null);
        Assert.assertTrue("Метод Trans.getPlace вернул неправильное место транзакции"
                ,place.deepEquals(transaction.getPlace()));
    }

    /**
     * Тест сеттера для place с валидными параметрами
     * Если передать допустимые параметры, то будет изменено место транзакции
     */
    @Test
    public void setPlaceTest() {
        Trans transaction = Trans.newTrans(new BigDecimal("110.75"),"UAH","123456****1234",client,place,1).orElse(null);
        Place place1 = Place.newPlace("A PLACE 10").orElse(null);
        transaction.setPlace(place1);
        Assert.assertEquals("Метод Trans.setPlace не заменил место транзакции",place1,transaction.getPlace());
    }

    /**
     * Тест сеттера для place с не валидными параметрами
     * Если передать недопустимые параметры, то не будет изменено место транзакции
     */
    @Test
    public void setPlaceNullTest() {
        Trans transaction = Trans.newTrans(new BigDecimal("1"),"UAH","123456****1234",client,place,1).orElse(null);
        transaction.setPlace(null);
        Assert.assertEquals("Метод Trans.setPlace установил недопустимое значение места - null"
                ,place,transaction.getPlace());
    }

    /**
     * Тест метода toString
     * Результат метода должен содержать все поля объекта, кроме serialVersionUID и transactions
     */
    @Test
    public void toStringTest() {
        String tostring = Client.newClient("Ivan","Ivanoff","Ivanoff","0123456789").orElse(null).toString();
        Field[] fields = Client.class.getDeclaredFields();
        for (Field field : fields) {
            if (!"serialVersionUID".equals(field.getName())) {
                Assert.assertTrue("Метод Trans.toString не содержит поле " + field.getName()
                        , tostring.contains(field.getName()));
            }
        }
    }

    /**
     * Тест метода hashCode
     * Когда запрашиваем hashCode, получаем правильный hashCode
     */
    @Test
    public void hashCodeTest() {
        Trans transaction = Trans.newTrans(new BigDecimal("1"),"UAH","123456****1234",client,place,1).orElse(null);
        int hashExpected = -765014595;
        int hashActual = transaction.hashCode();
        Assert.assertEquals("Значение hash не совпадает с ожидаемым",hashActual,hashExpected);

        transaction = Trans.newTrans(new BigDecimal("1"),"UAH","123456****1234",client,place,184616).orElse(null);
        hashExpected = 492570284;
        hashActual = transaction.hashCode();
        Assert.assertEquals("Значение hash не совпадает с ожидаемым",hashActual,hashExpected);

        transaction = Trans.newTrans(new BigDecimal("1"),"UAH","123456****1234",client,place,7105684).orElse(null);
        hashExpected = 1234618477;
        hashActual = transaction.hashCode();
        Assert.assertEquals("Значение hash не совпадает с ожидаемым",hashActual,hashExpected);

        ReflectionTestUtils.setField(transaction,"id",0);
        hashExpected = 0;
        hashActual = transaction.hashCode();
        Assert.assertEquals("Значение hash не совпадает с ожидаемым",hashActual,hashExpected);
    }

    /**
     * Тест метода equals
     * Если сравниваем объекты класса, сравнение происходит только по id
     */
    @Test
    public void equalsTest() {
        Client client1 = Client.newClient("Petr","Petroff","Petroff","1111111111").orElse(null);
        Place place1 = Place.newPlace("A PLACE 10").orElse(null);
        Trans transaction = Trans.newTrans(new BigDecimal("1"),"UAH","123456****1234",client,place,1).orElse(null);
        Trans transaction1 = Trans.newTrans(new BigDecimal("2"),"USD","654321****4321",client1,place1,1).orElse(null);
        ReflectionTestUtils.setField(transaction1,"id",12119887295L);
        Assert.assertEquals("Метод Trans.equals вернул неверный результат - транзакции с одинаковыми id " +
                "эквивалентны независимо от других полей",transaction,transaction1);

        client1 = Client.newClient("Ivan","Ivanoff","Ivanoff","0123456789").orElse(null);
        place1 = Place.newPlace("A PLACE 0").orElse(null);
        transaction1 = Trans.newTrans(new BigDecimal("1"),"UAH","123456****1234",client1,place1,2).orElse(null);
        Assert.assertNotEquals("Метод Trans.equals вернул неверный результат - транзакции с разными id " +
                "не эквивалентны",transaction,transaction1);
    }

    /**
     * Тест метода deepEquals
     * Если подробно сравниваем объекты класса, сравнение происходит по всем полям
     */
    @Test
    public void deepEqualsTest() {
        Trans transaction = Trans.newTrans(new BigDecimal("1"),"UAH","123456****1234",client,place,1).orElse(null);
        Trans transaction1 = Trans.newTrans(new BigDecimal("1"),"UAH","123456****1234",client,place,1).orElse(null);
        Assert.assertTrue("Метод Trans.deepEquals вернул неверный результат - транзакции со всеми эквивалентными" +
                " полями deep эквивалентны",transaction.deepEquals(transaction1));

        transaction1 = Trans.newTrans(new BigDecimal("2"),"UAH","123456****1234",client,place,1).orElse(null);
        ReflectionTestUtils.setField(transaction1,"id",12119887295L);
        Assert.assertFalse("Метод Trans.deepEquals вернул неверный результат - транзакции с разными" +
                " суммами не deep эквивалентны",transaction.deepEquals(transaction1));

        transaction1 = Trans.newTrans(new BigDecimal("1"),"USD","123456****1234",client,place,1).orElse(null);
        ReflectionTestUtils.setField(transaction1,"id",12119887295L);
        Assert.assertFalse("Метод Trans.deepEquals вернул неверный результат - транзакции с разными" +
                " суммами не deep эквивалентны",transaction.deepEquals(transaction1));

        transaction1 = Trans.newTrans(new BigDecimal("1"),"UAH","654321****4321",client,place,1).orElse(null);
        ReflectionTestUtils.setField(transaction1,"id",12119887295L);
        Assert.assertFalse("Метод Trans.deepEquals вернул неверный результат - транзакции с разными" +
                " картами не deep эквивалентны",transaction.deepEquals(transaction1));

        Client client1 = Client.newClient("Petr","Ivanoff","Ivanoff","0123456789").orElse(null);
        transaction1 = Trans.newTrans(new BigDecimal("1"),"UAH","123456****1234",client1,place,1).orElse(null);
        ReflectionTestUtils.setField(transaction1,"id",12119887295L);
        Assert.assertFalse("Метод Trans.deepEquals вернул неверный результат - транзакции с не deep эквивалентными" +
                " клиентами не deep эквивалентны",transaction.deepEquals(transaction1));

        Place place1 = Place.newPlace("A PLACE 10").orElse(null);
        transaction1 = Trans.newTrans(new BigDecimal("1"),"UAH","123456****1234",client,place1,1).orElse(null);
        ReflectionTestUtils.setField(transaction1,"id",12119887295L);
        Assert.assertFalse("Метод Trans.deepEquals вернул неверный результат - транзакции с не deep эквивалентными" +
                " местами не deep эквивалентны",transaction.deepEquals(transaction1));
    }

    /**
     * Тест метода clone
     * Когда создаем клон объекта, получаем исключение
     */
    @Test
    public void cloneTest() {
        Trans transaction = Trans.newTrans(new BigDecimal("1"),"UAH","123456****1234",client,place,1).orElse(null);
        Assert.assertThrows("Метод Trans.clone клонировал объект, хотя должен был бросить исключение"
                , CloneNotSupportedException.class,()->transaction.clone());
    }

    /**
     * Тест неизменности результата метода calculateKey
     * Когда создаем новый объект, для него рассчитывается ожидаемый id
     */
    @Test
    public void calculateKeyTest() {
        Trans transaction = Trans.newTrans(new BigDecimal("1"),"UAH","123456****1234",client,place,10).orElse(null);
        long idExpected = 12119887295L;
        long idActual = ReflectionTestUtils.invokeMethod(transaction,"calculateKey", 1);
        Assert.assertEquals("Алгоритм или данные расчета ключей методом Trans.calculateKey изменились",idExpected,idActual);
    }

    /**
     * Тест метода calculateKey на недопущение возврата отрицательных значений
     * Когда создаем новый объект с параметрами подобранными для получения отрицательного id из-за переполнения,
     * то получаем положительное значение.
     */
    @Test
    public void calculateKeyNegativeValuesTest() {
        Trans transaction = Trans.newTrans(new BigDecimal("1"), "UAH", "123456****1234", client, place, 922337204).orElse(null);
        Assert.assertTrue("Расчет id транзакции вернул отрицательное значение или 0",transaction.getId()>0);
    }
}