package org.app.musical_philharmonic.service;

import org.app.musical_philharmonic.repository.TicketRepository;
import org.app.musical_philharmonic.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class StatisticsService {

    private final UserRepository userRepository;
    private final TicketRepository ticketRepository;

    public StatisticsService(UserRepository userRepository, TicketRepository ticketRepository) {
        this.userRepository = userRepository;
        this.ticketRepository = ticketRepository;
    }

    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        // Total number of users
        long totalUsers = userRepository.count();
        stats.put("totalUsers", totalUsers);
        
        // Average wait time (time from reservation to purchase)
        double avgWaitTimeMinutes = calculateAverageWaitTime();
        stats.put("averageWaitTimeMinutes", avgWaitTimeMinutes);
        stats.put("averageWaitTimeHours", avgWaitTimeMinutes / 60.0);
        
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

    private double calculateAverageWaitTime() {
        List<org.app.musical_philharmonic.entity.Ticket> soldTickets = ticketRepository.findAll().stream()
            .filter(t -> t.getStatus() == org.app.musical_philharmonic.entity.TicketStatus.SOLD)
            .filter(t -> t.getReservationExpiration() != null && t.getPurchaseTimestamp() != null)
            .toList();
        
        if (soldTickets.isEmpty()) {
            return 0.0;
        }
        
        long totalMinutes = 0;
        int count = 0;
        
        for (org.app.musical_philharmonic.entity.Ticket ticket : soldTickets) {
            // Calculate time from reservation creation (estimated from expiration - 30 min default)
            // or use purchase timestamp if reservation was made
            LocalDateTime reservationTime = ticket.getReservationExpiration() != null 
                ? ticket.getReservationExpiration().minusMinutes(30) // Default reservation is 30 min
                : ticket.getPurchaseTimestamp();
            
            if (ticket.getPurchaseTimestamp() != null && reservationTime != null) {
                Duration duration = Duration.between(reservationTime, ticket.getPurchaseTimestamp());
                if (duration.toMinutes() > 0) {
                    totalMinutes += duration.toMinutes();
                    count++;
                }
            }
        }
        
        return count > 0 ? (double) totalMinutes / count : 0.0;
    }
}

