package org.app.musical_philharmonic.service;

import org.app.musical_philharmonic.dto.TicketResponse;
import org.app.musical_philharmonic.entity.Concert;
import org.app.musical_philharmonic.entity.Ticket;
import org.app.musical_philharmonic.entity.TicketStatus;
import org.app.musical_philharmonic.entity.User;
import org.app.musical_philharmonic.repository.ConcertRepository;
import org.app.musical_philharmonic.repository.TicketRepository;
import org.app.musical_philharmonic.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.UUID;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class TicketService {

    private static final Logger log = LoggerFactory.getLogger(TicketService.class);

    private final TicketRepository ticketRepository;
    private final ConcertRepository concertRepository;
    private final UserRepository userRepository;

    public TicketService(TicketRepository ticketRepository,
                         ConcertRepository concertRepository,
                         UserRepository userRepository) {
        this.ticketRepository = ticketRepository;
        this.concertRepository = concertRepository;
        this.userRepository = userRepository;
    }

    public Page<TicketResponse> listTickets(java.util.Optional<Integer> concertId, java.util.Optional<UUID> buyerId, java.util.Optional<TicketStatus> status, Pageable pageable) {
        Page<Ticket> page = ticketRepository.findAll(pageable);
        if (concertId.isPresent()) {
            page = ticketRepository.findByConcertId(concertId.get(), pageable);
        } else if (buyerId.isPresent()) {
            page = ticketRepository.findByBuyerId(buyerId.get(), pageable);
        } else if (status.isPresent()) {
            page = ticketRepository.findByStatus(status.get(), pageable);
        }
        return page.map(this::toResponse);
    }

    public Page<TicketResponse> availability(Integer concertId, Pageable pageable) {
        if (!concertRepository.existsById(concertId)) {
            throw new ResponseStatusException(NOT_FOUND, "Concert not found");
        }
        return ticketRepository.findByConcertId(concertId, pageable).map(this::toResponse);
    }

    public Page<TicketResponse> ticketsByBuyer(UUID buyerId, Pageable pageable) {
        return ticketRepository.findByBuyerId(buyerId, pageable).map(this::toResponse);
    }

    public TicketResponse get(Integer id) {
        return ticketRepository.findById(id).map(this::toResponse)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Ticket not found"));
    }

    @Transactional
    public TicketResponse book(Integer concertId, String seatNumber, UUID buyerId, LocalDateTime expiration, String actorEmail) {
        Concert concert = concertRepository.findById(concertId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Concert not found"));
        if (ticketRepository.existsByConcertIdAndSeatNumberAndStatusIn(
                concertId, seatNumber, EnumSet.of(TicketStatus.SOLD, TicketStatus.RESERVED))) {
            throw new ResponseStatusException(BAD_REQUEST, "Seat already taken");
        }
        Ticket ticket = new Ticket();
        ticket.setConcert(concert);
        ticket.setSeatNumber(seatNumber);
        ticket.setStatus(TicketStatus.RESERVED);
        ticket.setReservationExpiration(expiration != null ? expiration : LocalDateTime.now().plusMinutes(30));
        if (buyerId != null) {
            User buyer = userRepository.findById(buyerId)
                    .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Buyer not found"));
            ticket.setBuyer(buyer);
        }
        Ticket saved = ticketRepository.save(ticket);
        log.info("Booked ticket seat={} concert={} by={}", seatNumber, concertId, actorEmail);
        return toResponse(saved);
    }

    @Transactional
    public TicketResponse purchase(Integer concertId, String seatNumber, UUID buyerId, String paymentMethod, String actorEmail) {
        Concert concert = concertRepository.findById(concertId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Concert not found"));
        Ticket ticket = ticketRepository.findByConcertIdAndStatus(concertId, TicketStatus.RESERVED, Pageable.unpaged())
                .getContent().stream()
                .filter(t -> seatNumber.equals(t.getSeatNumber()))
                .findFirst()
                .orElseGet(() -> {
                    Ticket t = new Ticket();
                    t.setConcert(concert);
                    t.setSeatNumber(seatNumber);
                    return t;
                });
        if (ticket.getStatus() == TicketStatus.SOLD) {
            throw new ResponseStatusException(BAD_REQUEST, "Seat already sold");
        }
        if (buyerId != null) {
            User buyer = userRepository.findById(buyerId)
                    .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Buyer not found"));
            ticket.setBuyer(buyer);
        }
        ticket.setStatus(TicketStatus.SOLD);
        ticket.setReservationExpiration(null);
        ticket.setPaymentMethod(paymentMethod);
        Ticket saved = ticketRepository.save(ticket);
        log.info("Purchased ticket seat={} concert={} by={} payment={}", seatNumber, concertId, actorEmail, paymentMethod);
        return toResponse(saved);
    }

    @Transactional
    public TicketResponse returnTicket(Integer ticketId, String reason, String actorEmail) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Ticket not found"));
        ticket.setStatus(TicketStatus.AVAILABLE);
        ticket.setBuyer(null);
        ticket.setReturnReason(reason);
        ticket.setReturnTime(LocalDateTime.now());
        Ticket saved = ticketRepository.save(ticket);
        log.info("Returned ticket id={} reason={} by={}", ticketId, reason, actorEmail);
        return toResponse(saved);
    }

    public Page<TicketResponse> salesHistory(LocalDateTime from, LocalDateTime to, Pageable pageable) {
        return ticketRepository.findByPurchaseTimestampBetween(from, to, pageable).map(this::toResponse);
    }

    public TicketResponse createAvailable(Integer concertId, String seatNumber) {
        Concert concert = concertRepository.findById(concertId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Concert not found"));
        if (ticketRepository.existsByConcertIdAndSeatNumberAndStatusIn(
                concertId, seatNumber, EnumSet.of(TicketStatus.SOLD, TicketStatus.RESERVED, TicketStatus.AVAILABLE))) {
            throw new ResponseStatusException(BAD_REQUEST, "Seat already exists");
        }
        Ticket ticket = new Ticket();
        ticket.setConcert(concert);
        ticket.setSeatNumber(seatNumber);
        ticket.setStatus(TicketStatus.AVAILABLE);
        return toResponse(ticketRepository.save(ticket));
    }

    public TicketResponse toResponse(Ticket ticket) {
        TicketResponse resp = new TicketResponse();
        resp.setId(ticket.getId());
        resp.setConcertId(ticket.getConcert() != null ? ticket.getConcert().getId() : null);
        resp.setBuyerId(ticket.getBuyer() != null ? ticket.getBuyer().getId() : null);
        resp.setSeatNumber(ticket.getSeatNumber());
        resp.setPurchaseTimestamp(ticket.getPurchaseTimestamp());
        resp.setStatus(ticket.getStatus());
        resp.setReservationExpiration(ticket.getReservationExpiration());
        resp.setPaymentMethod(ticket.getPaymentMethod());
        resp.setReturnReason(ticket.getReturnReason());
        resp.setReturnTime(ticket.getReturnTime());
        return resp;
    }
}

