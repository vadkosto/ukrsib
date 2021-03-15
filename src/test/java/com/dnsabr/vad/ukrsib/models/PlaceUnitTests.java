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
 * Unit-тесты Place
 */
@RunWith(JUnit4.class)
public class PlaceUnitTests {

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
     * Тест создания объекта методом Place.newPlace с валидными параметрами
     * Если передать допустимые параметры, то будет создан новый объект класса Place
     */
    @Test
    public void newPlaceTest() {
        Place place = Place.newPlace("A PLACE 0").orElse(null);
        Assert.assertNotNull("Место проведения транзакции не было создано, хотя все данные правильные",place);
    }

    /**
     * Тест создания объекта методом Place.newPlace с не валидными параметрами
     * Если передать недопустимые параметры, то не будет создан новый объект класса Place
     */
    @Test
    public void newPlaceNullTest() {
        Place place = Place.newPlace(" ").orElse(null);
        Assert.assertNull("Место проведения транзакции было создано, хотя ожидался null, так как не указано" +
                " название места проведения транзакции",place);

        place = Place.newPlace(null).orElse(null);
        Assert.assertNull("Место проведения транзакции было создано, хотя ожидался null, так как название " +
                "места проведения транзакции не может быть null",place);
    }

    /**
     * Тест геттера для id
     * Когда запрашиваем id места, получаем правильный id места
     */
    @Test
    public void getIdTest() {
        Place place = Place.newPlace("A PLACE 0").orElse(null);
        Assert.assertEquals("Метод Place.getId вернул неправильный id места проведения транзакций"
                ,0,place.getId());

        ReflectionTestUtils.setField(place,"id",46756);
        Assert.assertEquals("Метод Place.getId вернул неправильный id места проведения транзакций"
                ,46756,place.getId());
    }

    /**
     * Тест геттера для placeName
     * Когда запрашиваем placeName места, получаем правильный placeName места
     */
    @Test
    public void getPlaceNameTest() {
        Place place = Place.newPlace("A PLACE 0").orElse(null);
        Assert.assertEquals("Метод Place.getPlaceName вернул неправильный id места проведения транзакций"
                ,"A PLACE 0",place.getPlaceName());
    }

    /**
     * Тест сеттера для placeName с валидными параметрами
     * Если передать допустимые параметры, то будет изменен placeName клиента
     */
    @Test
    public void setPlaceNameTest() {
        Place place = Place.newPlace("A PLACE 0").orElse(null);
        place.setPlaceName("A PLACE 1");
        Assert.assertEquals("Метод Place.setPlaceName не изменил название места проведения транзакции"
                ,"A PLACE 1",place.getPlaceName());
    }

    /**
     * Тест сеттера для placeName с не валидными параметрами
     * Если передать недопустимые параметры, то не будет изменен placeName клиента
     */
    @Test
    public void setPlaceNameNullTest() {
        Place place = Place.newPlace("A PLACE 0").orElse(null);
        place.setPlaceName(null);
        Assert.assertEquals("Метод Place.setPlaceName изменил название места проведения транзакции на " +
                "неправильное - название null","A PLACE 0",place.getPlaceName());

        place.setPlaceName(" ");
        Assert.assertEquals("Метод Place.setPlaceName изменил название места проведения транзакции на " +
                "неправильное - название не указано","A PLACE 0",place.getPlaceName());
    }

    /**
     * Тест метода toString
     * Результат метода должен содержать все поля объекта, кроме serialVersionUID
     */
    @Test
    public void toStringTest() {
        String tostring = Place.newPlace("A PLACE 0").orElse(null).toString();
        Field[] fields = Place.class.getDeclaredFields();
        for (Field field : fields) {
            if (!"serialVersionUID".equals(field.getName())) {
                Assert.assertTrue("Метод Place.toString не содержит поле " + field.getName(), tostring.contains(field.getName()));
            }
        }
    }

    /**
     * Тест метода hashCode
     * Когда запрашиваем hashCode, получаем правильный hashCode
     */
    @Test
    public void hashCodeTest() {
        Place place = Place.newPlace("A PLACE 0").orElse(null);
        int hashExpected = -1361309065;
        int hashActual = place.hashCode();
        Assert.assertEquals("Значение hash не совпадает с ожидаемым",hashActual,hashExpected);
        place.setPlaceName("6");
        hashExpected = 85;
        hashActual = place.hashCode();

        Assert.assertEquals("Значение hash не совпадает с ожидаемым",hashActual,hashExpected);
        place.setPlaceName("in the wild wild west");
        hashExpected = -1059009032;
        hashActual = place.hashCode();
        Assert.assertEquals("Значение hash не совпадает с ожидаемым",hashActual,hashExpected);

        ReflectionTestUtils.setField(place,"placeName"," ");
        hashExpected = 63;
        hashActual = place.hashCode();
        Assert.assertEquals("Значение hash не совпадает с ожидаемым",hashActual,hashExpected);

        ReflectionTestUtils.setField(place,"placeName",null);
        hashExpected = 31;
        hashActual = place.hashCode();
        Assert.assertEquals("Значение hash не совпадает с ожидаемым",hashActual,hashExpected);
    }

    /**
     * Тест метода equals
     * Если сравниваем объекты класса, сравнение происходит только по placeName
     */
    @Test
    public void equalsTest() {
        Place place = Place.newPlace("A PLACE 0").orElse(null);
        Place place1 = Place.newPlace("A PLACE 0").orElse(null);
        ReflectionTestUtils.setField(place,"id",7);
        Assert.assertEquals("Метод Place.equals вернул неверный результат - места с одинаковыми названиями" +
                " эквивалентны",place,place1);

        ReflectionTestUtils.setField(place,"id",0);
        place.setPlaceName("A PLACE 7");
        Assert.assertNotEquals("Метод Place.equals вернул неверный результат - места с разными названиями не" +
                " эквивалентны",place,place1);
    }

    /**
     * Тест метода deepEquals
     * Если подробно сравниваем объекты класса, сравнение происходит по всем полям, кроме id
     */
    @Test
    public void deepEqualsTest() {
        Place place = Place.newPlace("A PLACE 0").orElse(null);
        Place place1 = Place.newPlace("A PLACE 0").orElse(null);
        Assert.assertTrue("Метод Place.deepEquals вернул неверный результат - места со всеми одинаковыми полями" +
                " deep эквивалентны",place.deepEquals(place1));

        place1 = Place.newPlace("A PLACE 8").orElse(null);
        Assert.assertNotEquals("Метод Place.deepEquals вернул неверный результат - места с разными названиями мест" +
                " не deep эквивалентны",place,place1);

        place1.setPlaceName("A PLACE 0");
        ReflectionTestUtils.setField(place,"id",9);
        Assert.assertTrue("Метод Place.deepEquals вернул неверный результат - места с одинаковым названием, " +
                "но разными id deep эквивалентны",place.deepEquals(place1));
    }

    /**
     * Тест метода clone
     * Когда создаем клон объекта, получаем исключение
     */
    @Test
    public void cloneTest() {
        Place place = Place.newPlace("A PLACE 0").orElse(null);
        Assert.assertThrows("Метод Place.clone клонировал объект, хотя должен был бросить исключение", CloneNotSupportedException.class,()->place.clone());
    }
}