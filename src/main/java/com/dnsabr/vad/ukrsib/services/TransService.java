package com.dnsabr.vad.ukrsib.services;

import com.dnsabr.vad.ukrsib.repository.TransRepository;
import com.dnsabr.vad.ukrsib.models.*;
import org.hibernate.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.*;

/**
 * Сервис записи данных в БД
 * Поля:
 *  transRepository - интерфейс взаимодействия hibernate с таблицей transactions базы данных
 *  entityManager - менеджер сущностей Hibernate
 */
@Service
public class TransService {

    @Autowired
    private TransRepository transRepository;
    @PersistenceContext
    private EntityManager entityManager;

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    /**
     * Сохраняет транзакции в БД.
     * 1. Получает от SaverService пакет транзакций
     * 2. Начинает новую JPA-транзакцию с таймаутом
     * 3. Для Place проверяет наличие данных в кеше или БД по натуральному ключу
     * 4. Сохраняет данные с использованием пакетной вставки
     * 5. Завершает JPA-транзакцию
     * 6. При любом исключении откатывает JPA-транзакцию.
     * Isolation.READ_COMMITTED в сочетании с триггерами в конечном результате гарантируют наличие данных в БД
     * Измененные SQL-запросы (INSERT IGNORE) для Клиентов и Мест позволяют не откатывать JPA-транзакцию,
     *  когда конкурирующая транзакция добавила этого же Клиента или Место между SELECT и INSERT текущей JPA-транзакции.
     * @param transactions список транзакций для пакетного добавления
     * @return 0 в случае неудачи, иначе количество добавленных транзакций
     */
    @Transactional(isolation = Isolation.READ_COMMITTED, propagation = Propagation.REQUIRES_NEW,timeout = 4,rollbackFor = {Throwable.class})
    public int saveAll(List<Trans> transactions) {
        if (null==transactions || transactions.isEmpty()) {
            return 0;
        }
        try {
            Session session = entityManager.unwrap(Session.class);
            for (Trans transaction : transactions) {
                Place place = session.bySimpleNaturalId(Place.class)
                        .load(transaction.getPlace().getPlaceName());
                if (null != place) {
                    transaction.setPlace(place);
                }
                transRepository.save(transaction);
            }
            transRepository.flush();
            return transactions.size();
        } catch (Throwable e) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            logger.warn(e.getMessage());
            return 0;
        }
    }
}
