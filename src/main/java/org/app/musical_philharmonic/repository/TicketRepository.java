package org.app.musical_philharmonic.repository;

import org.app.musical_philharmonic.entity.Ticket;
import org.app.musical_philharmonic.entity.TicketStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface TicketRepository extends JpaRepository<Ticket, Integer> {
    List<Ticket> findByConcertId(Integer concertId);
    List<Ticket> findByBuyerId(java.util.UUID buyerId);
    List<Ticket> findByStatus(TicketStatus status);

    @Query("select count(t) from Ticket t where t.concert.id = :concertId and t.status = 'SOLD'")
    long countSoldByConcert(@Param("concertId") Integer concertId);

    @Query("select count(t) from Ticket t where t.concert.id = :concertId and t.status = 'AVAILABLE'")
    long countAvailableByConcert(@Param("concertId") Integer concertId);

    @Query("select t from Ticket t where t.status = 'RESERVED' and t.reservationExpiration < :cutoff")
    List<Ticket> findExpiredReservations(@Param("cutoff") LocalDateTime cutoff);
}

