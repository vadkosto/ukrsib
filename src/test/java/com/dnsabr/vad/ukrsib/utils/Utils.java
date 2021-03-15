package com.dnsabr.vad.ukrsib.utils;

import com.dnsabr.vad.ukrsib.models.Client;
import com.dnsabr.vad.ukrsib.models.Place;
import com.dnsabr.vad.ukrsib.models.Trans;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Вспомогательный класс для тестов
 * В конце класса приведена структура входящего XML-файла
 */
public class Utils {

    /**
     * Обращается к соответствующим методам для внесения тех же данных,
     * которые содержатся во входящем XML-файле для сопоставления в тестовых методах
     * с данными полученными из базы данных.
     * @param transactions список транзакций
     * @param size количество транзакций
     */
    public static void fillLists(List<Trans> transactions, int size) {
        List<Client> clients = new ArrayList<>();
        List<Place> places = new ArrayList<>();
        transactions.clear();
        Utils.fillLists(clients,places,transactions,size);
    }

    /**
     * Обращается к соответствующим методам для внесения тех же данных,
     * которые содержатся во входящем XML-файле для сопоставления в тестовых методах
     * с данными полученными из базы данных.
     * Вносит данные транзакций и настраивает связи между транзакциями и местами и клиентами так же
     * как во входящем XML-файле
     * @param clients список клиентов
     * @param places список мест
     * @param transactions список транзакций
     * @param size количество транзакций
     */
    public static void fillLists(List<Client> clients, List<Place> places, List<Trans> transactions, int size) {
        Utils.fillClientList(clients);
        Utils.fillPlaceList(places);
        transactions.clear();
        for (int i=0;i<size;i+=12) {
            Trans trans = Trans.newTrans(new BigDecimal("10.01"),"UAH","123456****1234",clients.get((i+3)%3),places.get((i+4)%4),i+1).orElse(null);
            transactions.add(trans);
            trans = Trans.newTrans(new BigDecimal("9876.01"),"UAH","123456****1234",clients.get((i+3)%3),places.get((i+5)%4),i+2).orElse(null);
            transactions.add(trans);
            trans = Trans.newTrans(new BigDecimal("12.01"),"USD","123456****1234",clients.get((i+3)%3),places.get((i+6)%4),i+3).orElse(null);
            transactions.add(trans);
            trans = Trans.newTrans(new BigDecimal("12.01"),"EUR","123456****1234",clients.get((i+3)%3),places.get((i+7)%4),i+4).orElse(null);
            transactions.add(trans);
            trans = Trans.newTrans(new BigDecimal("10.01"),"UAH","123456****1234",clients.get((i+4)%3),places.get((i+4)%4),i+5).orElse(null);
            transactions.add(trans);
            trans = Trans.newTrans(new BigDecimal("9876.01"),"UAH","123456****1234",clients.get((i+4)%3),places.get((i+5)%4),i+6).orElse(null);
            transactions.add(trans);
            trans = Trans.newTrans(new BigDecimal("12.01"),"USD","123456****1234",clients.get((i+4)%3),places.get((i+6)%4),i+7).orElse(null);
            transactions.add(trans);
            trans = Trans.newTrans(new BigDecimal("12.01"),"EUR","123456****1234",clients.get((i+4)%3),places.get((i+7)%4),i+8).orElse(null);
            transactions.add(trans);
            trans = Trans.newTrans(new BigDecimal("10.01"),"UAH","123456****1234",clients.get((i+5)%3),places.get((i+4)%4),i+9).orElse(null);
            transactions.add(trans);
            trans = Trans.newTrans(new BigDecimal("9876.01"),"UAH","123456****1234",clients.get((i+5)%3),places.get((i+5)%4),i+10).orElse(null);
            transactions.add(trans);
            trans = Trans.newTrans(new BigDecimal("12.01"),"USD","123456****1234",clients.get((i+5)%3),places.get((i+6)%4),i+11).orElse(null);
            transactions.add(trans);
            trans = Trans.newTrans(new BigDecimal("12.01"),"EUR","123456****1234",clients.get((i+5)%3),places.get((i+7)%4),i+12).orElse(null);
            transactions.add(trans);
        }
    }

    /**
     * Вносит те же данные (только список мест), которые содержатся во входящем XML-файле
     * для сопоставления в тестовых методах с данными полученными из базы данных
     * @param list список мест
     */
    private static void fillPlaceList(List<Place> list) {
        list.clear();
        Place place = Place.newPlace("A PLACE 1").orElse(null);
        ReflectionTestUtils.setField(place,"id",1);
        list.add(place);
        place = Place.newPlace("A PLACE 2").orElse(null);
        ReflectionTestUtils.setField(place,"id",2);
        list.add(place);
        place = Place.newPlace("A PLACE 3").orElse(null);
        ReflectionTestUtils.setField(place,"id",3);
        list.add(place);
        place = Place.newPlace("A PLACE 4").orElse(null);
        ReflectionTestUtils.setField(place,"id",4);
        list.add(place);
    }

    /**
     * Вносит те же данные (только список клиентов), которые содержатся во входящем XML-файле
     * для сопоставления в тестовых методах с данными полученными из базы данных
     * @param list список клиентов
     */
    private static void fillClientList(List<Client> list) {
        list.clear();
        Client client = Client.newClient("Ivan","Ivanoff","Ivanoff","1234567890").orElse(null);
        list.add(client);
        client = Client.newClient("Ivan","Petroff","Petroff","1234567891").orElse(null);
        list.add(client);
        client = Client.newClient("Ivan","Sidoroff","Sidoroff","1234567892").orElse(null);
        list.add(client);
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