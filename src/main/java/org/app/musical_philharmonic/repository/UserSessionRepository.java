package org.app.musical_philharmonic.repository;

import org.app.musical_philharmonic.entity.Role;
import org.app.musical_philharmonic.entity.UserSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface UserSessionRepository extends JpaRepository<UserSession, UUID> {
    
    List<UserSession> findByUserIdOrderByLoginTimeDesc(UUID userId);
    
    @Query("SELECT s FROM UserSession s WHERE s.user.role = :role AND s.logoutTime IS NOT NULL")
    List<UserSession> findCompletedSessionsByRole(@Param("role") Role role);
    
    @Query("SELECT s FROM UserSession s WHERE s.user.role = 'CUSTOMER' AND s.logoutTime IS NOT NULL")
    List<UserSession> findCompletedCustomerSessions();
}

