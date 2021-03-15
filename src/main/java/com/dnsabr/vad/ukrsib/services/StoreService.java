package com.dnsabr.vad.ukrsib.services;

import com.dnsabr.vad.ukrsib.models.Trans;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Сервис-хранилище транзакций
 * Поля:
 *  batchSize - размер пакета вставки (количество транзакций)
 *  batchAmount - максимальное количество пакетов для хранения
 *  errorsBeforeTerminate - количество ошибок при достижении которого принудительно завершать приложение
 *  transactions - потокобезопасная очередь для хранения транзакций
 *  countErrorsBeforeShutdown - счетчик ошибок
 *  parserDone - флаг завершения обработки файда сервисом разбора
 *  terminated - флаг закрытия хранилища
 */
@Service
public class StoreService {

    @Value("${spring.jpa.properties.hibernate.jdbc.batch_size}")
    private int batchSize;
    @Value("${spring.jpa.properties.app.store.batch.amount}")
    private int batchAmount;
    @Value("${spring.jpa.properties.app.errors.count.before.terminate}")
    private int errorsBeforeTerminate;

    private static Queue<Trans> transactions = new ConcurrentLinkedQueue<>();
    private static AtomicInteger countErrorsBeforeShutdown = new AtomicInteger();
    private static boolean parserDone = false;
    private static boolean terminated = false;

    StoreService() {
    }

    /**
     * Добавляет транзакцию в конец очереди хранения
     * При достижении лимита очереди ожидает пока из очереди не извлекут объекты
     * При закрытии хранилища предотвращает добавление новых объектов
     * @param transaction транзакция
     */
    public void add(Trans transaction) {
        if (null==transaction) {
            // Увеличение счетчика ошибок и проверка не пора ли закрыть хранилище
            if (errorsBeforeTerminate < countErrorsBeforeShutdown.incrementAndGet()) {
                doTerminate();
            }
        } else {
            while (!terminated && getSize() > batchSize * batchAmount) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {/*пустое*/}
            }
            while (!terminated && !transactions.offer(transaction)) {
                try {
                    // Увеличение счетчика ошибок и проверка не пора ли закрыть хранилище
                    if (errorsBeforeTerminate < countErrorsBeforeShutdown.incrementAndGet()) {
                        doTerminate();
                    }
                    Thread.sleep(100);
                } catch (InterruptedException e) {/*пустое*/}
            }
        }
    }

    /**
     * Добавляет список транзакций в конец очереди независимо от лимита хранилища
     * При закрытии хранилища предотвращает добавление новых объектов
     * @param list список транзакций
     */
    void addDueToError(List<Trans> list) {
        // Увеличение счетчика ошибок и проверка не пора ли закрыть хранилище
        if (errorsBeforeTerminate < countErrorsBeforeShutdown.incrementAndGet()) {
            doTerminate();
        }
        if (null!=list && list.size()!=0) {
            while (!terminated && !transactions.addAll(list)) {
                try {
                    // Увеличение счетчика ошибок и проверка не пора ли закрыть хранилище
                    if (errorsBeforeTerminate < countErrorsBeforeShutdown.incrementAndGet()) {
                        doTerminate();
                    }
                    Thread.sleep(100);
                } catch (InterruptedException e) {/*пустое*/}
            }
        }
    }

    /**
     * Извлекает и возвращает 1 транзакцию из начала очереди
     * Ожидает если очередь пуста
     * При закрытии хранилища не выдает объекты
     * @return транзакция
     */
    private Trans get() {
        Trans trans;
        while (null==(trans=transactions.poll()) && !parserDone && !terminated) {
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {/*пустое*/}
        }
        return trans;
    }

    /**
     * Добавляет в полученный пустой список транзакции из хранилища в размере batchSize если хранилище не закрыто
     * @param list список для добавления в него транзакций из хранилища
     */
    void get(List<Trans> list) {
        if (null!=list && !terminated) {
            for (int i = 0; i < batchSize; i++) {
                Trans transaction = get();
                if (null != transaction) {
                    list.add(transaction);
                } else {
                    return;
                }
            }
        }
    }

    /**
     * Возвращает текущее количество транзакций в хранилище
     * @return текущее количество транзакций в хранилище
     */
    public int getSize() {
        return transactions.size();
    }

    /**
     * Устанавливает флаг сигнализирующий о завершении работы сервиса разбора файла двнных
     */
    void parserDone() {
        parserDone = true;
    }

    /**
     * Возвращает состояние флага сигнализирующий о завершении работы сервиса разбора файла двнных
     * @return состояние флага сигнализирующий о завершении работы сервиса разбора файла двнных
     */
    public boolean isParserDone() {
        return parserDone;
    }

    /**
     * Закрывает хранилище на прием и выдачу объектов из очереди и очищает очередь
     * Устанавливает флаг сигнализирующий о завершении работы сервиса разбора файла двнных
     * Устанавливает флаг закрытия хранилища
     */
    static void doTerminate() {
        parserDone = true;
        terminated=true;
        transactions.clear();
    }

    /**
     * Возвращает состояние флага закрытия хранилища
     * @return состояние флага закрытия хранилища
     */
    public boolean isTerminated() {
        return terminated;
    }
}
