package com.dnsabr.vad.ukrsib.services;

import com.dnsabr.vad.ukrsib.models.Client;
import com.dnsabr.vad.ukrsib.models.Place;
import com.dnsabr.vad.ukrsib.models.Trans;
import com.dnsabr.vad.ukrsib.utils.StaxStreamProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.events.XMLEvent;
import java.io.*;
import java.math.BigDecimal;
import java.util.*;

/**
 * Класс-сервис с методом парсером XML-файла входящих данных
 * Данные извлекаются с помощью Streaming API for XML (StAX)
 * Поля:
 *  fileName - абсолютный или относительно проекта путь к XML-файлу для разбора
 *  doCheck - указывает проводить ли предварительную проверку XML-файла на корректность данных (пустые значения).
 *            устанавливается в application.properties
 *  store - хранилище-очередь транзакций полученных от parseService и еще не затребованых saveService
 */
@Service
public class ParseService implements Runnable{

    @Value("${spring.jpa.properties.app.source.file}")
    private String fileName;
    @Value("${spring.jpa.properties.app.parser.errors.check}")
    private boolean doCheck;

    @Autowired
    private StoreService store;

    ParseService() {
    }

    /**
     * Возвращает текущий режим работы сервиса
     * @return {@code true} режим проверки данных {@code false} режим разбора данных и добавления в хранилище
     */
    boolean isDoCheck() {
        return doCheck;
    }

    /**
     * Парсит XML-файл входящих данных.
     * Принцип работы построен на прохождении по XML-документу и выборе нужных данных частями.
     * Таким образом данные не переполняют память, что важно при обработке файлов большого объема.
     * Размер файла ограничен только возможностями операционной системы и JVM.
     * В режиме проверки: проверяет данные и выводит результат в журнал. При нахождении ошибок завершает работу
     *  после проверки всего файла. Если ошибок нет, отключает режим проверки и вызывает себя рекурсивно.
     * В режиме разбора: сразу передает каждую транзакцию в хранилище сервиса StoreService.
     * Метод в цикле перебирает все теги XML-документа (цикл while).
     *  При нахождении тега <transaction> в новом цикле (цикл do)
     *  перебирает все теги до закрывающего тега </transaction>,
     *  сохраняя значения в соответствующие переменные в конструкции switch case.
     *  Переходит к следующей итеррации цикла while
     * Структура XML-файла указана в классе com.dnsabr.vad.ukrsib.utils.Utils)
     * Сервис прекращает работу после полной обработки входящего файла, при закрытии хранилища StoreService
     * или при наличии ошибок. Оповещает StoreService о завершении своей работы. Ведет журнал действий.
     */
    public void run() {

        Logger logger = LoggerFactory.getLogger(this.getClass());
        boolean fileDoNotHaveMistakes = true;

        if (doCheck) {
            // Режим проверки
            logger.info("Запущен процесс предварительного разбора входящего XML-файла");
        } else {
            // Режим разбора и передачи в хранилище
            logger.info("Запущен сервис разбора входящего XML-файла");
        }

        int serial = 0;
        try (StaxStreamProcessor processor = new StaxStreamProcessor(new FileInputStream(fileName))) {

            XMLStreamReader reader = processor.getReader();

            while (reader.hasNext() && !store.isTerminated()) {

                int event = reader.next();
                if (event == XMLEvent.START_ELEMENT && "transaction".equals(reader.getLocalName())) {
                    String placeName = "", currency = "", card = "", firstName = "", lastName = "", middleName = "", inn = "", amount1 = "";
                    BigDecimal amount = null;
                    Client client;
                    Place place;
                    Trans transaction;

                    do {
                        event = reader.next();

                        if (event == XMLEvent.START_ELEMENT) {
                            switch (reader.getLocalName()) {
                                case "place":
                                    placeName = reader.getElementText();
                                    break;
                                case "amount":
                                    amount1 = reader.getElementText();
                                    break;
                                case "currency":
                                    currency = reader.getElementText();
                                    break;
                                case "card":
                                    card = reader.getElementText();
                                    break;
                                case "firstName":
                                    firstName = reader.getElementText();
                                    break;
                                case "lastName":
                                    lastName = reader.getElementText();
                                    break;
                                case "middleName":
                                    middleName = reader.getElementText();
                                    break;
                                case "inn":
                                    inn = reader.getElementText();
                                    break;
                                default:
                            }
                        }
                    } while (event != XMLEvent.END_ELEMENT || !"transaction".equals(reader.getLocalName()));

                    serial++;
                    if (doCheck) {
                        // Режим проверки
                        client = Client.newClient(firstName, lastName, middleName, inn).orElse(null);
                        place = Place.newPlace(placeName).orElse(null);
                        try {
                            amount = new BigDecimal(amount1);
                        } catch (NumberFormatException nf) {/*пустое*/}
                        transaction = Trans.newTrans(amount, currency, card, client, place, serial).orElse(null);

                        if (null == client || null == place || null == transaction) {
                            fileDoNotHaveMistakes = false;
                            logger.error("Недопустимое значение в транзакции в файле " + fileName + " порядковый номер "
                                    + (serial) + " : " + placeName + "," + amount1 + "," + currency + "," + card + "," + firstName + ","
                                    + lastName + "," + middleName + "," + inn);
                        }
                    } else {
                        // Режим разбора и передачи в хранилище
                        client = Client.newClient(firstName, lastName, middleName, inn).orElseThrow();
                        place = Place.newPlace(placeName).orElseThrow();
                        transaction = Trans.newTrans(new BigDecimal(amount1), currency, card, client, place, serial).orElseThrow();
                        store.add(transaction);
                    }
                }
            }
        } catch (NoSuchElementException | NumberFormatException e) {
            logger.error("Недопустимое значение в транзакции в файле "+fileName+" порядковый номер "
                    +(serial)+". Для выявления всех ошибок и вывода подробной информации о транзакции " +
                    "запустите приложение с параметром spring.jpa.properties.app.parser.errors.check=true");
            fileDoNotHaveMistakes = false;
            store.doTerminate();
        } catch (XMLStreamException e) {
            logger.error("Невозможно обработать файл "+fileName+". Проверьте соответствие структуры XML-файла" +
                    " примеру в jUnit-тестах");
            fileDoNotHaveMistakes = false;
            store.doTerminate();
        } catch (IOException e) {
            logger.error("Невозможно обработать файл "+fileName+". Проверьте наличие файла в корне проекта " +
                    "или по указанному пути, права доступа и диск на котором он расположен!");
            fileDoNotHaveMistakes = false;
            store.doTerminate();
        }

        if (doCheck) {
            // Режим проверки
            doCheck = false;
            logger.info("Завершил работу процесс предварительного разбора входящего XML-файла. Прочитано транзакций: " + serial);
            if (fileDoNotHaveMistakes) {
                run();
            } else {
                logger.error("Файл "+fileName+" не прошел проверку! Транзакции для загрузки в БД не будут переданы!");
                store.doTerminate();
            }
        } else {
            // Режим разбора и передачи в хранилище
            store.parserDone();
            if (store.isTerminated()) {
                logger.warn("Принудительно остановлен сервис разбора входящего XML-файла. Прочитано транзакций: " + serial);
            } else {
                logger.info("Завершил работу сервис разбора входящего XML-файла. Прочитано транзакций: " + serial);
            }
        }
    }
}
