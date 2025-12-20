package org.app.musical_philharmonic.repository;

import org.app.musical_philharmonic.entity.Ticket;
import org.app.musical_philharmonic.entity.TicketStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface TicketRepository extends JpaRepository<Ticket, Integer>, JpaSpecificationExecutor<Ticket> {
    Page<Ticket> findByConcertId(Integer concertId, Pageable pageable);
    Page<Ticket> findByBuyerId(java.util.UUID buyerId, Pageable pageable);
    Page<Ticket> findByStatus(TicketStatus status, Pageable pageable);

    @Query("select count(t) from Ticket t where t.concert.id = :concertId and t.status = 'SOLD'")
    long countSoldByConcert(@Param("concertId") Integer concertId);

    @Query("select count(t) from Ticket t where t.concert.id = :concertId and t.status = 'AVAILABLE'")
    long countAvailableByConcert(@Param("concertId") Integer concertId);

    @Query("select count(t) from Ticket t where t.concert.id = :concertId and t.status in ('RESERVED', 'SOLD')")
    long countReservedOrSoldByConcert(@Param("concertId") Integer concertId);

    @Query("select t from Ticket t where t.status = 'RESERVED' and t.reservationExpiration < :cutoff")
    List<Ticket> findExpiredReservations(@Param("cutoff") LocalDateTime cutoff);

    boolean existsByConcertIdAndSeatNumberAndStatusIn(Integer concertId, String seatNumber, java.util.Collection<TicketStatus> statuses);

    Page<Ticket> findByConcertIdAndStatus(Integer concertId, TicketStatus status, Pageable pageable);

    Page<Ticket> findByPurchaseTimestampBetween(LocalDateTime from, LocalDateTime to, Pageable pageable);

    @Query("SELECT t FROM Ticket t WHERE LOWER(t.concert.title) LIKE LOWER(CONCAT('%', :concertName, '%'))")
    Page<Ticket> findByConcertNameContainingIgnoreCase(@Param("concertName") String concertName, Pageable pageable);

    @Query("SELECT t FROM Ticket t WHERE t.buyer.email LIKE CONCAT('%', :buyerEmail, '%')")
    Page<Ticket> findByBuyerEmailContainingIgnoreCase(@Param("buyerEmail") String buyerEmail, Pageable pageable);
}

