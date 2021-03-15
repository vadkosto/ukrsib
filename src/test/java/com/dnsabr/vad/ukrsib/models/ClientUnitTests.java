package com.dnsabr.vad.ukrsib.models;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.springframework.test.util.ReflectionTestUtils;
import java.lang.reflect.Field;

/**
 * Unit-тесты Client
 */
@RunWith(JUnit4.class)
public class ClientUnitTests {

    /**
     * Метод для выполнения действий перед каждым тестом класса
     */
    @Before
    public void setUp() {
    }

    /**
     * Метод для выполнения действий после каждого теста класса
     */
    @After
    public void tearDown() {
    }

    /**
     * Тест создания объекта методом Client.newClient с валидными параметрами
     * Если передать допустимые параметры, то будет создан новый объект класса Client
     */
    @Test
    public void newClientTest() {
        Client client = Client.newClient("Ivan","Ivanoff","Ivanoff","0123456789").orElse(null);
        Assert.assertNotNull("Клиент не был создан, хотя все данные правильные",client);
    }

    /**
     * Тест создания объекта методом Client.newClient с не валидными параметрами
     * Если передать недопустимые параметры, то не будет создан новый объект класса Client
     */
    @Test
    public void newClientNullTest() {
        Client client = Client.newClient(" ","Ivanoff","Ivanoff","0123456789").orElse(null);
        Assert.assertNull("Клиент был создан, хотя ожидался null, так как не указано имя",client);

        client = Client.newClient("Ivan"," ","Ivanoff","0123456789").orElse(null);
        Assert.assertNull("Клиент был создан, хотя ожидался null, так как не указана фамилия",client);

        client = Client.newClient("Ivan","Ivanoff"," ","0123456789").orElse(null);
        Assert.assertNull("Клиент был создан, хотя ожидался null, так как не указано отчество",client);

        client = Client.newClient("Ivan","Ivanoff","Ivanoff"," ").orElse(null);
        Assert.assertNull("Клиент был создан, хотя ожидался null, так как не указан ИНН",client);

        client = Client.newClient("Ivan","Ivanoff","Ivanoff",null).orElse(null);
        Assert.assertNull("Клиент был создан, хотя ожидался null, так как ИНН не может быть null",client);

        client = Client.newClient("Ivan","Ivanoff","Ivanoff","010000000000").orElse(null);
        Assert.assertNull("Клиент был создан, хотя ожидался null, так как длина ИНН не должна быть менее или более 10 цифр",client);

        client = Client.newClient("Ivan","Ivanoff","Ivanoff","01").orElse(null);
        Assert.assertNull("Клиент был создан, хотя ожидался null, так как длина ИНН не должна быть менее или более 10 цифр",client);

        client = Client.newClient("Ivan","Ivanoff","Ivanoff","7sdnk4fker").orElse(null);
        Assert.assertNull("Клиент был создан, хотя ожидался null, так как ИНН должен состоять из 10 цифр",client);

        client = Client.newClient("Ivan","Ivanoff","Ivanoff","0000000000").orElse(null);
        Assert.assertNull("Клиент был создан, хотя ожидался null, так как ИНН не может быть 0000000000",client);
    }

    /**
     * Тест геттера для inn
     * Когда запрашиваем inn клиента, получаем правильный inn клиента
     */
    @Test
    public void getInnTest() {
        Client client = Client.newClient("Ivan","Ivanoff","Ivanoff","0123456789").orElse(null);
        Assert.assertEquals("Метод Client.getInn вернул неправильный ИНН клиента","0123456789",client.getInn());

    }

    /**
     * Тест сеттера для inn с валидными параметрами
     * Если передать допустимые параметры, то будет изменен inn клиента
     */
    @Test
    public void setInnTest() {
        Client client = Client.newClient("Ivan","Ivanoff","Ivanoff","0123456789").orElse(null);
        client.setInn("1111111111");
        Assert.assertEquals("Метод Client.setInn не изменил ИНН клиента","1111111111",client.getInn());
    }

    /**
     * Тест сеттера для inn с не валидными параметрами
     * Если передать недопустимые параметры, то не будет изменен inn клиента
     */
    @Test
    public void setInnNullTest() {
        Client client = Client.newClient("Ivan","Ivanoff","Ivanoff","0123456789").orElse(null);
        client.setInn(" ");
        Assert.assertEquals("Метод Client.setInn изменил ИНН клиента на недопустимое - пустой ИНН"
                ,"0123456789",client.getInn());

        client = Client.newClient("Ivan","Ivanoff","Ivanoff","0123456789").orElse(null);
        client.setInn("11111");
        Assert.assertEquals("Метод Client.setInn изменил ИНН клиента на недопустимое - длина не 10 цифр"
                ,"0123456789",client.getInn());

        client = Client.newClient("Ivan","Ivanoff","Ivanoff","0123456789").orElse(null);
        client.setInn("1d1t1k1d1n");
        Assert.assertEquals("Метод Client.setInn изменил ИНН клиента на недопустимое - содержит другие символы" +
                ", кроме цифр","0123456789",client.getInn());

        client.setInn(null);
        Assert.assertEquals("Метод Client.setInn изменил ИНН клиента на недопустимое - null"
                ,"0123456789",client.getInn());
    }

    /**
     * Тест геттера для firstName
     * Когда запрашиваем имя клиента, получаем правильное имя клиента
     */
    @Test
    public void getFirstNameTest() {
        Client client = Client.newClient("Ivan","Ivanoff","Ivanoff","0123456789").orElse(null);
        Assert.assertEquals("Метод Client.getFirstName вернул неправильное имя клиента"
                ,"Ivan",client.getFirstName());
    }

    /**
     * Тест сеттера для firstName с валидными параметрами
     * Если передать допустимые параметры, то будет изменено имя клиента
     */
    @Test
    public void setFirstNameTest() {
        Client client = Client.newClient("Ivan","Ivanoff","Ivanoff","0123456789").orElse(null);
        client.setFirstName("Petr");
        Assert.assertEquals("Метод Client.setFirstName не изменил имя клиента","Petr",client.getFirstName());
    }

    /**
     * Тест сеттера для firstName с не валидными параметрами
     * Если передать недопустимые параметры, то не будет изменено имя клиента
     */
    @Test
    public void setFirstNameNullTest() {
        Client client = Client.newClient("Ivan","Ivanoff","Ivanoff","0123456789").orElse(null);
        client.setFirstName(null);
        Assert.assertEquals("Метод Client.setFirstName изменил имя клиента на недопустимое - null"
                ,"Ivan",client.getFirstName());

        client = Client.newClient("Ivan","Ivanoff","Ivanoff","0123456789").orElse(null);
        client.setFirstName(" ");
        Assert.assertEquals("Метод Client.setFirstName изменил имя клиента на недопустимое - пустое имя"
                ,"Ivan",client.getFirstName());
    }

    /**
     * Тест геттера для lastName
     * Когда запрашиваем фамилию клиента, получаем правильную фамилию клиента
     */
    @Test
    public void getLastNameTest() {
        Client client = Client.newClient("Ivan","Ivanoff","Ivanoff","0123456789").orElse(null);
        Assert.assertEquals("Метод Client.getLastName вернул неправильную фамилию клиента"
                ,"Ivanoff",client.getLastName());
    }

    /**
     * Тест сеттера для lastName с валидными параметрами
     * Если передать допустимые параметры, то будет изменена фамилия клиента
     */
    @Test
    public void setLastNameTest() {
        Client client = Client.newClient("Ivan","Ivanoff","Ivanoff","0123456789").orElse(null);
        client.setLastName("Petrov");
        Assert.assertEquals("Метод Client.setLastName не изменил фамилию клиента","Petrov",client.getLastName());
    }

    /**
     * Тест сеттера для lastName с не валидными параметрами
     * Если передать недопустимые параметры, то не будет изменена фамилия клиента
     */
    @Test
    public void setLastNameNullTest() {
        Client client = Client.newClient("Ivan","Ivanoff","Ivanoff","0123456789").orElse(null);
        client.setLastName(null);
        Assert.assertEquals("Метод Client.setLastName изменил имя клиента на недопустимое - null"
                ,"Ivanoff",client.getLastName());

        client = Client.newClient("Ivan","Ivanoff","Ivanoff","0123456789").orElse(null);
        client.setLastName(" ");
        Assert.assertEquals("Метод Client.setLastName изменил имя клиента на недопустимое - пустое имя"
                ,"Ivanoff",client.getLastName());
    }

    /**
     * Тест геттера для middleName
     * Когда запрашиваем отчество клиента, получаем правильное отчество клиента
     */
    @Test
    public void getMiddleNameTest() {
        Client client = Client.newClient("Ivan","Ivanoff","Ivanoff","0123456789").orElse(null);
        Assert.assertEquals("Метод Client.getMiddleName вернул неправильную имя клиента"
                ,"Ivanoff",client.getMiddleName());
    }

    /**
     * Тест сеттера для middleName с валидными параметрами
     * Если передать допустимые параметры, то будет изменено отчество клиента
     */
    @Test
    public void setMiddleNameTest() {
        Client client = Client.newClient("Ivan","Ivanoff","Ivanoff","0123456789").orElse(null);
        client.setMiddleName("Petrov");
        Assert.assertEquals("Метод Client.setMiddleName не изменил отчество клиента"
                ,"Petrov",client.getMiddleName());
    }

    /**
     * Тест сеттера для middleName с не валидными параметрами
     * Если передать недопустимые параметры, то не будет изменено отчество клиента
     */
    @Test
    public void setMiddleNameNullTest() {
        Client client = Client.newClient("Ivan","Ivanoff","Ivanoff","0123456789").orElse(null);
        client.setMiddleName(null);
        Assert.assertEquals("Метод Client.setMiddleName изменил имя клиента на недопустимое - null",
                "Ivanoff",client.getMiddleName());

        client = Client.newClient("Ivan","Ivanoff","Ivanoff","0123456789").orElse(null);
        client.setMiddleName(" ");
        Assert.assertEquals("Метод Client.setMiddleName изменил имя клиента на недопустимое - пустое имя"
                ,"Ivanoff",client.getMiddleName());
    }

    /**
     * Тест метода toString
     * Результат метода должен содержать все поля объекта, кроме serialVersionUID
     */
    @Test
    public void toStringTest() {
        String tostring = Client.newClient("Ivan","Ivanoff","Ivanoff","0123456789").orElse(null).toString();
        Field[] fields = Client.class.getDeclaredFields();
        for (Field field : fields) {
            if (!"serialVersionUID".equals(field.getName())) {
                Assert.assertTrue("Метод Client.toString не содержит поле " + field.getName()
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
        Client client = Client.newClient("Ivan","Ivanoff","Ivanoff","0123456789").orElse(null);
        int hashExpected = 1584875013;
        int hashActual = client.hashCode();
        Assert.assertEquals("Значение hash не совпадает с ожидаемым",hashActual,hashExpected);

        client = Client.newClient("Ivan","Ivanoff","Ivanoff","0000000009").orElse(null);
        hashExpected = 1419845129;
        hashActual = client.hashCode();
        Assert.assertEquals("Значение hash не совпадает с ожидаемым",hashActual,hashExpected);

        client = Client.newClient("Ivan","Ivanoff","Ivanoff","8000000009").orElse(null);
        hashExpected = -152262911;
        hashActual = client.hashCode();
        Assert.assertEquals("Значение hash не совпадает с ожидаемым",hashActual,hashExpected);

        ReflectionTestUtils.setField(client,"inn","0000000000");
        hashExpected = 1419845120;
        hashActual = client.hashCode();
        Assert.assertEquals("Значение hash не совпадает с ожидаемым",hashActual,hashExpected);

        ReflectionTestUtils.setField(client,"inn",null);
        hashExpected = 0;
        hashActual = client.hashCode();
        Assert.assertEquals("Значение hash не совпадает с ожидаемым",hashActual,hashExpected);
    }

    /**
     * Тест метода equals
     * Если сравниваем объекты класса, сравнение происходит только по inn
     */
    @Test
    public void equalsTest() {
        Client client = Client.newClient("Ivan","Ivanoff","Ivanoff","0123456789").orElse(null);
        Client client1 = Client.newClient("Petr","Petroff","Petroff","0123456789").orElse(null);
        Assert.assertEquals("Метод Client.equals вернул неверный результат - клиенты с одинаковыми ИНН " +
                "эквивалентны",client,client1);

        client1 = Client.newClient("Ivan","Ivanoff","Ivanoff","1111111111").orElse(null);
        Assert.assertNotEquals("Метод Client.equals вернул неверный результат - клиенты с разными ИНН " +
                "не эквивалентны",client,client1);
    }

    /**
     * Тест метода deepEquals
     * Если подробно сравниваем объекты класса, сравнение происходит по всем полям
     */
    @Test
    public void deepEqualsTest() {
        Client client = Client.newClient("Ivan","Ivanoff","Ivanoff","0123456789").orElse(null);
        Client client1 = Client.newClient("Ivan","Ivanoff","Ivanoff","0123456789").orElse(null);
        Assert.assertTrue("Метод Client.deepEquals вернул неверный результат - клиенты со всеми эквивалентными" +
                " полями deep эквивалентны",client.deepEquals(client1));

        client1 = Client.newClient("Petr","Ivanoff","Ivanoff","0123456789").orElse(null);
        Assert.assertFalse("Метод Client.deepEquals вернул неверный результат - клиенты с разными именами deep" +
                " не эквивалентны",client.deepEquals(client1));

        client1 = Client.newClient("Ivan","Petroff","Ivanoff","0123456789").orElse(null);
        Assert.assertFalse("Метод Client.deepEquals вернул неверный результат - клиенты с разными фамилиями deep" +
                " не эквивалентны",client.deepEquals(client1));

        client1 = Client.newClient("Ivan","Ivanoff","Petroff","0123456789").orElse(null);
        Assert.assertFalse("Метод Client.deepEquals вернул неверный результат - клиенты с разными отчествами deep" +
                " не эквивалентны",client.deepEquals(client1));

        client1 = Client.newClient("Ivan","Ivanoff","Ivanoff","1111111111").orElse(null);
        Assert.assertFalse("Метод Client.deepEquals вернул неверный результат - клиенты с разными ИНН deep" +
                " не эквивалентны",client.deepEquals(client1));
    }

    /**
     * Тест метода clone
     * Когда создаем клон объекта, получаем исключение
     */
    @Test
    public void cloneTest() {
        Client client = Client.newClient("Ivan","Ivanoff","Ivanoff","0123456789").orElse(null);
        Assert.assertThrows("Метод Client.clone клонировал объект, хотя должен был бросить исключение"
                , CloneNotSupportedException.class,()->client.clone());
    }
}