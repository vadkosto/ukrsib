package com.dnsabr.vad.ukrsib.repository;

import com.dnsabr.vad.ukrsib.models.Place;
import org.springframework.data.jpa.repository.JpaRepository;


/**
 * Интерфейс взаимодействия с таблицей places базы данных
 * Используются унаследованные стандартные методы
 */
public interface PlaceRepository extends JpaRepository<Place, Integer> {}
