package org.app.musical_philharmonic.repository;

import org.app.musical_philharmonic.entity.Concert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface ConcertRepository extends JpaRepository<Concert, Integer> {
    List<Concert> findByHallId(Integer hallId);
    List<Concert> findByPerformerId(Integer performerId);
    List<Concert> findByTitleContainingIgnoreCase(String titlePart);
    List<Concert> findByDate(LocalDate date);
    List<Concert> findByDateBetween(LocalDate start, LocalDate end);

    @Query("select count(c) from Concert c where c.performer.id = :performerId")
    long countByPerformer(@Param("performerId") Integer performerId);
}

