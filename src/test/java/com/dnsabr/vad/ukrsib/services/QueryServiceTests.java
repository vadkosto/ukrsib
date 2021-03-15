package com.dnsabr.vad.ukrsib.services;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Интеграционные тесты сервиса QueryService
 */
@RunWith(SpringRunner.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@SpringBootTest
@ActiveProfiles({"default"})
public class QueryServiceTests {

    @Autowired
    private QueryService queryService;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * Метод для выполнения действий перед каждым тестом класса
     * Приведение состояния базы данных и полей сервисов в необходимое состояние
     */
    @Before
    public void setUp() {
    }

    /**
     * Метод для выполнения действий после каждого теста класса
     */
    @After
    public void tearDown() {
    }

    /**
     * Тест установки и удаления триггеров в БД
     * Когда триггеры установлены, данные о них содержатся в ответе на специальный запрос к БД
     * Когда триггеры не установлены, данные о них отсутствуют в ответе на специальный запрос к БД
     */
    @Test
    public void addAndDropTriggersTest() {

        final List<String> listExpected = Arrays.asList(
                "trans_update_trigger_7did39f3",
                "places_update_trigger_7did39f3",
                "clients_update_trigger_7did39f3",
                "trans_delete_trigger_7did39f3",
                "places_delete_trigger_7did39f3",
                "clients_delete_trigger_7did39f3");

        // Когда триггеры установлены, данные о них содержатся в ответе на специальный запрос к БД

        List<String> listActual = new ArrayList<>();
        queryService.addTriggers();
        List<Map<String,Object>> result = jdbcTemplate.queryForList("show triggers;");
        result.forEach(x->listActual.add(x.get("Trigger").toString()));

        for (String triggerExpected : listExpected) {
            Assert.isTrue(listActual.contains(triggerExpected)
                    ,"Триггер "+triggerExpected+" для БД не установлен!");
        }

        try {
            TimeUnit.SECONDS.sleep(1);
        } catch (InterruptedException e) {/*пустое*/}

        // Когда триггеры удалены, данные о них отсутствуют в ответе на специальный запрос к БД

        listActual.clear();
        queryService.dropTriggers();
        result = jdbcTemplate.queryForList("show triggers;");
        result.forEach(x->listActual.add(x.get("Trigger").toString()));

        listExpected.forEach(triggerExpected->Assert.isTrue(!listActual.contains(triggerExpected)
                ,"Триггер "+triggerExpected+" не удален из БД!"));
    }
}