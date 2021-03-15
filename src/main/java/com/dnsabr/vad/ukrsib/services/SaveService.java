package com.dnsabr.vad.ukrsib.services;

import com.dnsabr.vad.ukrsib.models.Trans;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;

/**
 * Сервис управления записью данных в БД
 * Поля:
 *  batchSize - размер пакета вставки (количество транзакций)
 *  attempts - количество последовательных попыток вставки пакета транзакций при неудаче
 *  transService - объект сервиса добавления данных в БД
 *  store - хранилище-очередь транзакций полученных от parseService и еще не затребованых этим сервисом
 */
@Service
public class SaveService implements Runnable {

    @Value("${spring.jpa.properties.hibernate.jdbc.batch_size}")
    private int batchSize;
    @Value("${spring.jpa.properties.app.try.attempts}")
    private int attempts;
    @Autowired
    private TransService transService;
    @Autowired
    private StoreService store;

    SaveService() {
    }

    /**
     * 1. Запрашивает транзакции у хранилища в размере batchSize пока хранилище не закрыто или пока
     *  хранилище содержит данные или ParseService еще не завершил работу
     * 2. Предлагает transService добавить в БД транзакции полученные из хранилища. При получении от transService
     *  сообщения о неудачной попытке, пытается пока не будет исчерпано максимальное значение последовательных
     *  неудачных попыток.
     * 3. Если последняя попытка оказалась неудачной, возвращает пакет транзакций в хранилище
     * 4. После удачной попытки запрашивает у хранилища новый пакет транзакций, а прошлый удаляет
     * Ведет журнал действий.
     */
    @Override
    public void run() {
        Logger logger = LoggerFactory.getLogger(this.getClass());

        logger.info("Запущен новый поток сервиса сохранения данных в БД");
        List<Trans> transactions = new ArrayList<>(batchSize);

        while (!(store.isParserDone() && store.getSize()==0) && !store.isTerminated()) {
            store.get(transactions);

                boolean isCurrentTransactionDone=false;
                for (int attempt=1; attempt<=attempts; attempt++) {

                    if (store.isTerminated()) {
                        break;
                    }
                    if (transactions.size()==transService.saveAll(transactions)) {
                        logger.trace("В БД записано транзакций: "+ transactions.size());
                        isCurrentTransactionDone = true;
                        break;
                    } else {
                        logger.warn("Не удалась загрузка в базу данных при попытке "+attempt+" из "+attempts);
                    }
                }

                if (!isCurrentTransactionDone) {
                    store.addDueToError(transactions);
                    logger.warn("Транзакции возвращены в хранилище. Новые попытки будут предприняты позже.");
                }
            transactions.clear();
        }

        if (store.isTerminated()) {
            logger.warn("Принудительно остановлен поток сервиса сохранения данных в БД");
        } else {
            logger.info("Завершил работу поток сервиса сохранения данных в БД");
        }
    }
}
