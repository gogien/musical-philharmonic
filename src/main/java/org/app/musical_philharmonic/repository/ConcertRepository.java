package org.app.musical_philharmonic.repository;

import org.app.musical_philharmonic.entity.Concert;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;

public interface ConcertRepository extends JpaRepository<Concert, Integer> {
    Page<Concert> findByHallId(Integer hallId, Pageable pageable);
    Page<Concert> findByPerformerId(Integer performerId, Pageable pageable);
    Page<Concert> findByTitleContainingIgnoreCase(String titlePart, Pageable pageable);
    Page<Concert> findByDate(LocalDate date, Pageable pageable);
    Page<Concert> findByDateBetween(LocalDate start, LocalDate end, Pageable pageable);

    @Query("select count(c) from Concert c where c.performer.id = :performerId")
    long countByPerformer(@Param("performerId") Integer performerId);
}

