package com.dnsabr.vad.ukrsib.repository;

import com.dnsabr.vad.ukrsib.models.Client;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Интерфейс взаимодействия с таблицей clients базы данных
 * Используются унаследованные стандартные методы
 */
public interface ClientRepository extends JpaRepository<Client, String> {}