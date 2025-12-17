package org.app.musical_philharmonic.repository;

import org.app.musical_philharmonic.entity.Performer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PerformerRepository extends JpaRepository<Performer, Integer> {
    List<Performer> findByNameContainingIgnoreCase(String namePart);
}

