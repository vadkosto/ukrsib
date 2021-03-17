package com.dnsabr.vad.ukrsib.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StopWatch;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Класс запуска и проверки работы сервисов обработки данных
 * Поля:
 *  amountOfThreads - желательное количество потоков записи данных в БД - устанавливается в application.properties
 *  parseService - объект сервиса разбора входящего XML-файла
 *  saveService - объект сервиса управления записью данных в БД
 *  queryService - объект сервиса установки триггеров предотвращающих удаление/изменение данных ключевых полей
 *  store - хранилище-очередь транзакций полученных от parseService и еще не затребованых saveService
 */
@Service
public class MainService {

    @Value("${spring.jpa.properties.app.sql.threads}")
    private int amountOfThreads;

    @Autowired
    private ParseService parseService;
    @Autowired
    private SaveService saveService;
    @Autowired
    private QueryService queryService;
    @Autowired
    private StoreService store;

    MainService() {
    }

    /**
     * Осуществляет последовательные действия по запуску необходимых сервисов и проверку их работы
     * Ведет журнал действий. Использует таймер работы.
     * 1. Устанавливает SQL-триггеры предотвращающие удаление строк или изменение ключевых полей используемых таблиц
     * 2. Запускает parseService - парсер файла данных в отдельном потоке. Если парсер запускается в режиме
     *    предварительной проверки, ожидает завершение проверки.
     * 3. Запускает вычисленное (не менее 1-го и не более количества выделенных логических процессоров минус один)
     *    количество потоков сохранения данных saveService с разницей в 1 секунду, чтобы не запускать множество потоков
     *    для небольших файлов.
     * 4. Ожидает завершение работы всех сервисов. Сервисы могут быть остановлены принудительно при привышении
     *    критического порога ошибок (значение устанавливается в application.properties)
     * 5. Удаляет SQL-триггеры (пункт 1), только если все транзакции добавлены в БД
     * 6. Предлагает запустить приложение еще раз, если не все транзакции добавлены в БД
     */
    public void start() {
        Logger logger = LoggerFactory.getLogger(this.getClass());

        StopWatch watch = new StopWatch();
        watch.start();
        logger.info("Начало работы приложения");
        queryService.addTriggers();

        int threadsAvailable = Math.max(2, Math.min(amountOfThreads + 1, Runtime.getRuntime().availableProcessors()));

        // Запуск задач разбора входящего файла данных и добавления в БД
        // (подробнее смотреть com.dnsabr.vad.ukrsib.services.ParseService и SaveService)
        ExecutorService executor = Executors.newFixedThreadPool(threadsAvailable);
        executor.execute(parseService);
        while (parseService.isDoCheck()){
            try {
                TimeUnit.MILLISECONDS.sleep(100);
            } catch (InterruptedException e) {/*пустое*/}
        }
        executor.execute(saveService);
        for (int i = 2; i < threadsAvailable; i++) {
            try {
                TimeUnit.MILLISECONDS.sleep(1000);
            } catch (InterruptedException e) {/*пустое*/}
            if (!store.isParserDone()) {
                executor.execute(saveService);
            }
        }
        executor.shutdown();

        // Ожидание завершения работы всех сервисов
        while (!executor.isTerminated()) {
            try {
                if (store.isTerminated()) {
                    executor.shutdownNow();
                }
                TimeUnit.SECONDS.sleep(1);

            } catch (InterruptedException e) {/*пустое*/}
        }

        watch.stop();
        logger.info("Время работы приложения " + (int) watch.getTotalTimeSeconds() + " сек.");

        if (store.isTerminated()) {
            logger.warn("Принудительно остановлено приложение");
            logger.error("Не все транзакции были добавлены в базу! Запустите приложение еще раз. Будет предпринята попытка добавить отсутствующие транзакции.");
        } else {
            queryService.dropTriggers();
            logger.info("Все транзакции успешно добавлены в базу данных");
            logger.info("Завершение работы приложения");
        }
    }
}
