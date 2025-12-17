package org.app.musical_philharmonic.repository;

import org.app.musical_philharmonic.entity.Role;
import org.app.musical_philharmonic.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmail(String email);
    List<User> findByRole(Role role);
    List<User> findByNameContainingIgnoreCase(String name);
}

