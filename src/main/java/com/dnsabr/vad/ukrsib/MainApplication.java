package com.dnsabr.vad.ukrsib;

import com.dnsabr.vad.ukrsib.services.MainService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;

/**
 * Стандартный класс запуска приложения Spring Boot
 * Приложение разработано для выполнения тестового задания Укрсиббанка
 * Включено кеширование
 * Управление передается методу start() класса MainService
 */
@SpringBootApplication
@EnableCaching
public class MainApplication {

    @Autowired
    MainService mainService;

    public static void main(String[] args) {
        SpringApplication.run(MainApplication.class, args);
    }

    @Bean
    public ApplicationRunner init() {
        return args -> {
            mainService.start();
        };
    }
}