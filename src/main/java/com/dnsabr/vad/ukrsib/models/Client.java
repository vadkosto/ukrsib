package com.dnsabr.vad.ukrsib.models;

import org.hibernate.annotations.NaturalId;
import org.hibernate.annotations.SQLInsert;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Класс-сущность для таблицы clients
 * Данная таблица используется для хранения информации о лице связанном с транзакцией
 * и содержит значения блоков <client> из файла данных
 * Связь с таблицей транзакций однонаправленная со стороны транзакций
 * id_client - уникальный идентификатор данной таблицы. Не отмечен анноацией @GeneratedValue
 * В качестве id выбран естественный ключ inn. Это необходимо для осуществления вставок в базу данных пакетами.
 * firstName - имя лица связанного с транзакций - значение тега <firstName> из файла данных
 * lastName - фамилия лица связанного с транзакций - значение тега <lastName> из файла данных
 * middleName - отчество лица связанного с транзакций - значение тега <middleName> из файла данных
 * inn - индивидуальный налоговый номер лица связанного с транзакций - значение тега <inn> из файла данных
 * Доступ к конструкторам ограничен. Новые объекты создаются с помощью метода newClient
 * Объекты являются эквивалентными если у них совпадает id. Hash только по id.
 * Изменен стандартный SQL-запрос вставки в БД на INSERT IGNORE ..., что дает возможность не откатывать JPA-транзакцию,
 * когда конкурирующая транзакция добавила этого же Клиента между SELECT и INSERT текущей JPA-транзакции.
 * Объекты кешируются в кэш 2-го уровня. Версионность не используется
 */
@Entity
@Cacheable
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@Table(name = "clients")
@SQLInsert(sql = "insert ignore into clients (first_name, last_name, middle_name, inn) values (?, ?, ?, ?)")
public class Client implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @NaturalId
    @Column(unique = true, nullable = false, length = 10)
    private String inn;

    @Column(nullable = false)
    private String firstName;

    @Column(nullable = false)
    private String lastName;

    @Column(nullable = false)
    private String middleName;

    Client() {
    }

    private Client(String firstName, String lastName, String middleName, String inn) {
            this.firstName = firstName;
            this.lastName = lastName;
            this.middleName = middleName;
            this.inn = inn;
    }

    /**
     * Возвращает новые объекты данного класса
     * @param firstName имя клиента - не null и не пусто
     * @param lastName фамилия клиента - не null и не пусто
     * @param middleName отчество клиента - не null и не пусто
     * @param inn ИНН клиента - не null и 10 цифр и не все цифры 0
     * @return объект класса Optional с новым объектом данного класса или пустой,
     *          если параметры транзакции не удовлетворяют критериям
     */
    public static Optional<Client> newClient(String firstName, String lastName, String middleName, String inn) {
        if (null!=inn && inn.length()==10 && !inn.equals("0000000000") && Pattern.matches("\\d*",inn) && null!=firstName
                && !firstName.trim().isEmpty() && null!=lastName && !lastName.trim().isEmpty() && null!=middleName
                && !middleName.trim().isEmpty()) {
            return Optional.of(new Client(firstName,lastName,middleName,inn));
        } else {
            return Optional.empty();
        }
    }

    public String getInn() {
        return inn;
    }

    public void setInn(String inn) {
        if (null!=inn && inn.length()==10 && !inn.equals("0000000000") && Pattern.matches("\\d*",inn)) {
            this.inn = inn;
        }
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        if (null!=firstName && !firstName.trim().isEmpty()) {
            this.firstName = firstName;
        }
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        if (null!=lastName && !lastName.trim().isEmpty()) {
            this.lastName = lastName;
        }
    }

    public String getMiddleName() {
        return middleName;
    }

    public void setMiddleName(String middleName) {
        if (null!=middleName && !middleName.trim().isEmpty()) {
            this.middleName = middleName;
        }
    }

    /**
     * Возвращает строку с названиями и значениями полей объекта
     * исключая информацию о коллекциях объекта
     * @return строка с названиями и значениями полей этого объекта
     */
    @Override
    public String toString() {
        return "Client{" +
                "inn='" + inn + '\'' +
                ", firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", middleName='" + middleName + '\'' +
                '}';
    }

    /**
     * Возвращает hash-код этого объекта
     * @return hash-код
     */
    @Override
    public int hashCode() {
        return null != inn ? inn.hashCode() : 0;
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
        Client client1 = (Client) obj;
        return this.inn.equals(client1.inn);
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
        Client client1 = (Client) obj;
        return this.firstName.equals(client1.firstName)
                && this.lastName.equals(client1.lastName) && this.middleName.equals(client1.middleName)
                && this.inn.equals(client1.inn);
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
