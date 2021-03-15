package com.dnsabr.vad.ukrsib.repository;

import com.dnsabr.vad.ukrsib.models.*;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Интерфейс взаимодействия hibernate с таблицей transactions базы данных
 * Используются унаследованные стандартные методы
 */
public interface TransRepository extends JpaRepository<Trans, Long> {}