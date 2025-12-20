package org.app.musical_philharmonic.service;

import org.app.musical_philharmonic.entity.UserSession;
import org.app.musical_philharmonic.repository.TicketRepository;
import org.app.musical_philharmonic.repository.UserRepository;
import org.app.musical_philharmonic.repository.UserSessionRepository;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class StatisticsService {

    private final UserRepository userRepository;
    private final TicketRepository ticketRepository;
    private final UserSessionRepository userSessionRepository;

    public StatisticsService(UserRepository userRepository, 
                            TicketRepository ticketRepository,
                            UserSessionRepository userSessionRepository) {
        this.userRepository = userRepository;
        this.ticketRepository = ticketRepository;
        this.userSessionRepository = userSessionRepository;
    }

    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        // Total number of users
        long totalUsers = userRepository.count();
        stats.put("totalUsers", totalUsers);
        
        // Average session duration for customer users (time they remain logged in)
        double avgSessionMinutes = calculateAverageSessionDuration();
        stats.put("averageSessionMinutes", avgSessionMinutes);
        stats.put("averageSessionHours", avgSessionMinutes / 60.0);
        
        // Additional statistics
        long totalTickets = ticketRepository.count();
        long soldTickets = ticketRepository.findByStatus(
            org.app.musical_philharmonic.entity.TicketStatus.SOLD, 
            org.springframework.data.domain.Pageable.unpaged()
        ).getTotalElements();
        long reservedTickets = ticketRepository.findByStatus(
            org.app.musical_philharmonic.entity.TicketStatus.RESERVED, 
            org.springframework.data.domain.Pageable.unpaged()
        ).getTotalElements();
        long availableTickets = ticketRepository.findByStatus(
            org.app.musical_philharmonic.entity.TicketStatus.AVAILABLE, 
            org.springframework.data.domain.Pageable.unpaged()
        ).getTotalElements();
        
        stats.put("totalTickets", totalTickets);
        stats.put("soldTickets", soldTickets);
        stats.put("reservedTickets", reservedTickets);
        stats.put("availableTickets", availableTickets);
        
        // User distribution by role
        Map<String, Long> usersByRole = new HashMap<>();
        usersByRole.put("CUSTOMER", userRepository.findByRole(
            org.app.musical_philharmonic.entity.Role.CUSTOMER, 
            org.springframework.data.domain.Pageable.unpaged()
        ).getTotalElements());
        usersByRole.put("CASHIER", userRepository.findByRole(
            org.app.musical_philharmonic.entity.Role.CASHIER, 
            org.springframework.data.domain.Pageable.unpaged()
        ).getTotalElements());
        usersByRole.put("ADMIN", userRepository.findByRole(
            org.app.musical_philharmonic.entity.Role.ADMIN, 
            org.springframework.data.domain.Pageable.unpaged()
        ).getTotalElements());
        stats.put("usersByRole", usersByRole);
        
        // Ticket status distribution
        Map<String, Long> ticketsByStatus = new HashMap<>();
        ticketsByStatus.put("SOLD", soldTickets);
        ticketsByStatus.put("RESERVED", reservedTickets);
        ticketsByStatus.put("AVAILABLE", availableTickets);
        stats.put("ticketsByStatus", ticketsByStatus);
        
        return stats;
    }

    private double calculateAverageSessionDuration() {
        // Get all completed sessions for customer users
        List<UserSession> completedSessions = userSessionRepository.findCompletedCustomerSessions();
        
        if (completedSessions.isEmpty()) {
            return 0.0;
        }
        
        long totalMinutes = 0;
        int count = 0;
        
        for (UserSession session : completedSessions) {
            if (session.getLoginTime() != null && session.getLogoutTime() != null) {
                Duration duration = Duration.between(session.getLoginTime(), session.getLogoutTime());
                if (duration.toMinutes() > 0) {
                    totalMinutes += duration.toMinutes();
                    count++;
                }
            }
        }
        
        return count > 0 ? (double) totalMinutes / count : 0.0;
    }
}

