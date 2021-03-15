package com.dnsabr.vad.ukrsib.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * Сервис установки SQL-триггеров для предотвращения удаления или изменения ключевых полей во время обновления
 * Триггеры удаляются после полного добавления информации из XML-файла
 * Эти триггеры необходимы в том случае, если таблицы базы SQL создаются с параметрами:
 * ON UPDATE CASCADE и/или ON DELETE CASCADE
 * Кроме того, триггер places_update_trigger_7did39f3 предотвращает изменения полей таблицы places чтобы сторонние
 * пользователи не изменили ключевое поле place во время работы или между запусками после сбоя приложения.
 * ToDo Если использование таких триггеров неприемлимо исходя из бизнес-логики, можно использовать
 * ToDo дополнительную таблицу аудита содержащую foreign keys для ключевых полей
 */
@Service
public class QueryService {
    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    QueryService() {
    }

    /**
     * Метод установки триггеров
     */
    public void addTriggers() {

        try {
            Integer result = jdbcTemplate.queryForObject("select count(*) from information_schema.triggers where trigger_name='trans_update_trigger_7did39f3';", Integer.class);
            if (null==result || result == 0) {
                jdbcTemplate.execute("CREATE TRIGGER trans_update_trigger_7did39f3 BEFORE UPDATE ON transactions FOR EACH ROW\n" +
                        "BEGIN\n" +
                        "    if (old.id<>new.id) then\n" +
                        "        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Cannot update this field while time new data are inserting';\n" +
                        "    end if;\n" +
                        "END;");
            }

            result = jdbcTemplate.queryForObject("select count(*) from information_schema.triggers where trigger_name='places_update_trigger_7did39f3';", Integer.class);
            if (null==result || result == 0) {
                jdbcTemplate.execute("CREATE TRIGGER places_update_trigger_7did39f3 BEFORE UPDATE ON places FOR EACH ROW\n" +
                        "BEGIN\n" +
                        "    if (old.id<>new.id OR old.place<>new.place) then\n" +
                        "        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Cannot update this field while time new data are inserting';\n" +
                        "    end if;\n" +
                        "END;");
            }

            result = jdbcTemplate.queryForObject("select count(*) from information_schema.triggers where trigger_name='clients_update_trigger_7did39f3';", Integer.class);
            if (null==result || result == 0) {
                jdbcTemplate.execute("CREATE TRIGGER clients_update_trigger_7did39f3 BEFORE UPDATE ON clients FOR EACH ROW\n" +
                        "BEGIN\n" +
                        "    if (old.inn<>new.inn) then\n" +
                        "        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Cannot update this field while time new data are inserting';\n" +
                        "    end if;\n" +
                        "END;");
            }

            result = jdbcTemplate.queryForObject("select count(*) from information_schema.triggers where trigger_name='trans_delete_trigger_7did39f3';", Integer.class);
            if (null==result || result == 0) {
                jdbcTemplate.execute("CREATE TRIGGER trans_delete_trigger_7did39f3 BEFORE DELETE ON transactions FOR EACH ROW\n" +
                        "BEGIN\n" +
                        "        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Cannot delete this row while time new data are inserting';\n" +
                        "END;");
            }

            result = jdbcTemplate.queryForObject("select count(*) from information_schema.triggers where trigger_name='clients_delete_trigger_7did39f3';", Integer.class);
            if (null==result || result == 0) {
                jdbcTemplate.execute("CREATE TRIGGER clients_delete_trigger_7did39f3 BEFORE DELETE ON clients FOR EACH ROW\n" +
                        "BEGIN\n" +
                        "        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Cannot delete this row while time new data are inserting';\n" +
                        "END;");
            }

            result = jdbcTemplate.queryForObject("select count(*) from information_schema.triggers where trigger_name='places_delete_trigger_7did39f3';", Integer.class);
            if (null==result || result == 0) {
                jdbcTemplate.execute("CREATE TRIGGER places_delete_trigger_7did39f3 BEFORE DELETE ON places FOR EACH ROW\n" +
                        "BEGIN\n" +
                        "        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Cannot delete this row while time new data are inserting';\n" +
                        "END;");
            }
        } catch (Exception e) {
            logger.warn("Не удалось установить некоторые/все триггеры для предотвращения изменения ключевых полей во время добавления транзакций!" +
                    "\nСуществует риск удаления данных или изменения ключевых полей транзакций в БД другими приложениями.");
        }
    }

    /**
     * Метод удаления триггеров
     */
    public void dropTriggers() {
        try {
            jdbcTemplate.execute("DROP TRIGGER IF EXISTS trans_update_trigger_7did39f3;");
            jdbcTemplate.execute("DROP TRIGGER IF EXISTS places_update_trigger_7did39f3;");
            jdbcTemplate.execute("DROP TRIGGER IF EXISTS clients_update_trigger_7did39f3;");
            jdbcTemplate.execute("DROP TRIGGER IF EXISTS trans_delete_trigger_7did39f3;");
            jdbcTemplate.execute("DROP TRIGGER IF EXISTS places_delete_trigger_7did39f3;");
            jdbcTemplate.execute("DROP TRIGGER IF EXISTS clients_delete_trigger_7did39f3;");
        } catch (Exception e) {
            logger.error("Не удалось удалить некоторые/все триггеры предотвращающие изменения ключевых полей во время добавления транзакций!" +
                    "\nНеобходимо удалить триггеры вручную с помощью SQL-запросов:" +
                    "\nDROP TRIGGER IF EXISTS trans_update_trigger_7did39f3;"+
                    "\nDROP TRIGGER IF EXISTS places_update_trigger_7did39f3;"+
                    "\nDROP TRIGGER IF EXISTS clients_update_trigger_7did39f3;"+
                    "\nDROP TRIGGER IF EXISTS trans_delete_trigger_7did39f3;"+
                    "\nDROP TRIGGER IF EXISTS places_delete_trigger_7did39f3;"+
                    "\nDROP TRIGGER IF EXISTS clients_delete_trigger_7did39f3;"
            );
        }
    }
}
