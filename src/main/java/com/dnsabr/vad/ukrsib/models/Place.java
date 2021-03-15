package com.dnsabr.vad.ukrsib.models;

import org.hibernate.annotations.*;
import org.hibernate.annotations.Cache;
import javax.persistence.*;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.io.Serializable;
import java.util.Objects;
import java.util.Optional;

/**
 * Класс-сущность для таблицы places
 * Данная таблица используется для хранения мест проведения транзакций - значение тегов <place> из файла данных
 * Связь с таблицей транзакций однонаправленная со стороны транзакций
 * id - уникальный идентификатор данной таблицы используется для связи с таблицей транзакций. @GeneratedValue SEQUENCE
 * для осуществления вставок в базу данных пакетами. Кроме того используется естественный ключ - название места.
 * placeName - название места проведения транзакций - значение тегов <place> из файла данных
 * Доступ к конструкторам ограничен. Новые объекты создаются с помощью метода newPlace. Setter для id отсутствует.
 * Объекты являются эквивалентными если у них совпадает placeName. Hash только по placeName.
 * Изменен стандартный SQL-запрос вставки в БД на INSERT IGNORE ..., что дает возможность не откатывать JPA-транзакцию,
 * когда конкурирующая транзакция добавила это же Место между SELECT и INSERT текущей JPA-транзакции.
 * Объекты кешируются в кэш 2-го уровня. Версионность не используется
 */
@Entity
@Cacheable
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@NaturalIdCache
@Table(name = "places")
@SQLInsert(sql = "insert ignore into places (place, id) values (?, ?)")
public class Place implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "place_generator")
    @SequenceGenerator(name="place_generator",sequenceName = "place_seq")
    @Column(unique = true, nullable = false)
    private int id;

    @NaturalId
    @Column(name="place", unique = true, nullable = false)
    private String placeName;

    Place() {
    }

    private Place(String placeName) {
            this.placeName = placeName;
    }

    /**
     * Возвращает новые объекты данного класса
     * @param placeName название места проведения транзакции
     * @return объект класса Optional с новым объектом данного класса или пустой,
     *          если параметры транзакции не удовлетворяют критериям
     */
    public static Optional<Place> newPlace(String placeName) {
        if (null != placeName && !placeName.trim().isEmpty()) {
            return Optional.of(new Place(placeName));
        } else {
            return Optional.empty();
        }
    }

    public int getId() {
        return id;
    }

    public String getPlaceName() {
        return placeName;
    }

    public void setPlaceName(String placeName) {
        if (null != placeName && !placeName.trim().isEmpty()) {
            this.placeName = placeName;
        }
    }

    /**
     * Возвращает строку с названиями и значениями полей объекта
     * исключая информацию о коллекциях объекта
     * @return строка с названиями и значениями полей этого объекта
     */
    @Override
    public String toString() {
        return "Place{" +
                "id=" + id +
                ", placeName='" + placeName + '\'' +
                '}';
    }

    /**
     * Возвращает hash-код этого объекта
     * @return hash-код
     */
    @Override
    public int hashCode() {
        return Objects.hash(placeName);
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
        if (!(obj instanceof Place)) return false;
        Place place = (Place) obj;
        return this.placeName.equals(place.placeName);
    }

    /**
     * Проверяет на эквивалентность переданный объект с этим объектом.
     * @param obj объект для проверки на эквивалентность этому объекту
     * @return {@code true} если у переданного объекта все поля и все поля объектов эквивалентены
     * всем полям и полям всех объектов этого объекта {@code false} иначе
     */
    public boolean deepEquals(Object obj) {
        return equals(obj);
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
}
