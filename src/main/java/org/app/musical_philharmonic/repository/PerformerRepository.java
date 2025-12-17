package org.app.musical_philharmonic.repository;

import org.app.musical_philharmonic.entity.Performer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PerformerRepository extends JpaRepository<Performer, Integer> {
    Page<Performer> findByNameContainingIgnoreCase(String namePart, Pageable pageable);
}

