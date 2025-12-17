package org.app.musical_philharmonic.repository;

import org.app.musical_philharmonic.entity.Hall;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HallRepository extends JpaRepository<Hall, Integer> {
    List<Hall> findByNameContainingIgnoreCase(String namePart);
    List<Hall> findByCapacityGreaterThanEqual(Integer capacity);
}

