package com.dnsabr.vad.ukrsib.models;

import javax.persistence.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.*;

/**
 * Класс-сущность для таблицы transactions
 * Данная таблица используется для хранения информации о транзакциях
 * и содержит некоторые значения блоков <transaction> из файла данных
 * Связь с таблицей клиентов однонаправленная - ManyToOne - множество транзакций может быть связано с одним лицом
 * Связь с таблицей мест транзакций однонаправленная - ManyToOne - множество транзакций может быть осуществлено в одном месте
 * id - уникальный идентификатор данной таблицы. Не отмечен анноацией @GeneratedValue и
 * будет формироваться при создании объктов в методе calculateKey. Это необходимо для
 * осуществления вставок в базу данных пакетами.
 * amount - сумма транзакции - значение тега <amount> из файла данных
 * currency - обозначение валюты транзакций - значение тега <currency> из файла данных
 * card - часть номера банковской карты связанной с транзакций - значение тега <card> из файла данных
 * Содержит Map<id,serial> transactions для предотвращения коллизии ключей.
 * Доступ к конструкторам ограничен. Новые объекты создаются с помощью метода newTrans
 * Setter для id отсутствует. Объекты являются эквивалентными если у них совпадает id.
 * Hash только по id. Версионность не используется
 */
@Entity
@Table(name = "transactions")
public class Trans implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final Map<Long,Integer> transactions = new HashMap<>();

    @Id
    @Column(unique = true, nullable = false)
    private long id;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(nullable = false, length = 14)
    private String card;

    @ManyToOne(fetch = FetchType.LAZY, cascade = {CascadeType.PERSIST,CascadeType.MERGE})
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    @ManyToOne(fetch = FetchType.LAZY, cascade = {CascadeType.PERSIST,CascadeType.MERGE})
    @JoinColumn(name = "place_id", nullable = false)
    private Place place;

    Trans() {
    }

    private Trans(BigDecimal amount, String currency, String card, Client client, Place place, int serial) {
            this.amount = amount;
            this.currency = currency;
            this.card = card;
            this.client = client;
            this.place = place;
            this.id = calculateKey(serial);
    }

    /**
     * Возвращает новые объекты данного класса
     * @param amount сумма транзакции > 0
     * @param currency обозначение валюты транзакции - не null и не пусто
     * @param card часть номера банковской карты связанной с транзакций - не null и не пусто
     * @param client клиент связанный с транзакцией - не null
     * @param place место проведения транзакции - не null
     * @param serial порядковый номер транзакции в файле > 0
     * @return объект класса Optional с новым объектом данного класса или пустой,
     *          если параметры транзакции не удовлетворяют критериям
     */
    public static Optional<Trans> newTrans(BigDecimal amount, String currency, String card, Client client, Place place, int serial) {
        if (null!=amount && amount.compareTo(new BigDecimal(0))>0 && null!=currency && !currency.trim().isEmpty()
                && null!=card && !card.trim().isEmpty() && null!=client && null!=place && serial>0) {
            return Optional.of(new Trans(amount, currency, card, client, place, serial));
        } else {
            return Optional.empty();
        }
    }

    public long getId() {
        return id;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        if (null!=amount && amount.compareTo(new BigDecimal(0))>0) {
            this.amount = amount;
        }
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        if (null!=currency && !currency.trim().isEmpty()) {
            this.currency = currency;
        }
    }

    public String getCard() {
        return card;
    }

    public void setCard(String card) {
        if (null!=card && !card.trim().isEmpty()) {
            this.card = card;
        }
    }

    public Client getClient() {
        return client;
    }

    public void setClient(Client client) {
        if (null!=client) {
            this.client = client;
        }
    }

    public Place getPlace() {
        return place;
    }

    public void setPlace(Place place) {
        if (null!=place) {
            this.place = place;
        }
    }

    /**
     * Возвращает строку с названиями и значениями полей объекта
     * исключая информацию о коллекциях объекта
     * @return строка с названиями и значениями полей объекта
     */
    @Override
    public String toString() {
        return "Trans{" +
                "id=" + id +
                ", amount=" + amount +
                ", currency='" + currency + '\'' +
                ", card='" + card + '\'' +
                ", client=" + client.toString() +
                ", place=" + place.toString() +
                '}';
    }

    /**
     * Возвращает hash-код объекта
     * @return hash-код
     */
    @Override
    public int hashCode() {
        return id != 0 ? Objects.hashCode(id) : 0;
    }

    /**
     * Проверяет на эквивалентность переданный объект с этим объектом
     * @param obj объект для проверки на эквивалентность этому объекту
     * @return {@code true} если ключевое поле переданного объекта эквивалентно такому полю у текущего
     *         {@code false} иначе
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Trans trans1 = (Trans) obj;
        return this.id ==trans1.id;
    }

    /**
     * Проверяет на эквивалентность переданный объект с этим объектом.
     * @param obj объект для проверки на эквивалентность этому объекту
     * @return {@code true} если у переданного объекта все поля и все поля объектов эквивалентены
     * всем полям и полям всех объектов этого объекта {@code false} иначе
     */
    public boolean deepEquals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Trans trans1 = (Trans) obj;
        return this.id==trans1.id && this.amount.compareTo(trans1.amount)==0 && this.currency.equals(trans1.currency)
                && this.card.equals(trans1.card) && this.client.deepEquals(trans1.client) && this.place.deepEquals(trans1.place);
    }

    /**
     * Выбрасывает ошибку при попытке клонирования этого объекта
     * @return CloneNotSupportedException
     * @throws CloneNotSupportedException Exception
     */
    @Override
    protected Object clone() throws CloneNotSupportedException {
        throw new CloneNotSupportedException("Объект нельзя клонировать");
    }

    /**
     * Возвращает id транзакции, расчитанный на основании значений всех полей и порядкового номера в файле
     * @param serial порядковый номер транзакции в файле
     * @return id транзакции
     */
    private long calculateKey(int serial) {
        int key = (int)(amount.doubleValue()*100) + currency.hashCode() + card.hashCode() + client.hashCode() + place.hashCode();
        long key1 = 10000000000L * serial + Math.abs(key);

        // Защита от коллизии ключей
        while (transactions.containsKey(key1) && serial!=transactions.get(key1) || key1<=0) {
            if (key1<=0) {
                key1 = key1 + Long.MAX_VALUE;
            } else {
                key1 += 141414141414L;
            }
        }
        transactions.put(key1,serial);

        return key1;
    }
}
