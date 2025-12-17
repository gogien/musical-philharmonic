package org.app.musical_philharmonic.controller;

import jakarta.validation.Valid;
import org.app.musical_philharmonic.dto.TicketRequest;
import org.app.musical_philharmonic.dto.TicketResponse;
import org.app.musical_philharmonic.entity.Concert;
import org.app.musical_philharmonic.entity.Ticket;
import org.app.musical_philharmonic.entity.TicketStatus;
import org.app.musical_philharmonic.entity.User;
import org.app.musical_philharmonic.repository.ConcertRepository;
import org.app.musical_philharmonic.repository.TicketRepository;
import org.app.musical_philharmonic.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;
import java.util.UUID;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@RestController
@RequestMapping("/api/tickets")
public class TicketController {

    private final TicketRepository ticketRepository;
    private final ConcertRepository concertRepository;
    private final UserRepository userRepository;

    public TicketController(TicketRepository ticketRepository,
                            ConcertRepository concertRepository,
                            UserRepository userRepository) {
        this.ticketRepository = ticketRepository;
        this.concertRepository = concertRepository;
        this.userRepository = userRepository;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','CASHIER')")
    public Page<TicketResponse> list(
            @RequestParam Optional<Integer> concertId,
            @RequestParam Optional<UUID> buyerId,
            @RequestParam Optional<TicketStatus> status,
            Pageable pageable
    ) {
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

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','CASHIER')")
    public TicketResponse get(@PathVariable Integer id) {
        return ticketRepository.findById(id).map(this::toResponse)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Ticket not found"));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','CASHIER')")
    public TicketResponse create(@Valid @RequestBody TicketRequest request) {
        Concert concert = concertRepository.findById(request.getConcertId())
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Concert not found"));
        Ticket ticket = new Ticket();
        ticket.setConcert(concert);
        ticket.setSeatNumber(request.getSeatNumber());
        if (request.getBuyerId() != null) {
            User buyer = userRepository.findById(request.getBuyerId())
                    .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Buyer not found"));
            ticket.setBuyer(buyer);
        }
        if (request.getStatus() != null) {
            ticket.setStatus(request.getStatus());
        }
        ticket.setReservationExpiration(request.getReservationExpiration());
        return toResponse(ticketRepository.save(ticket));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','CASHIER')")
    public TicketResponse update(@PathVariable Integer id, @Valid @RequestBody TicketRequest request) {
        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Ticket not found"));
        if (request.getConcertId() != null) {
            Concert concert = concertRepository.findById(request.getConcertId())
                    .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Concert not found"));
            ticket.setConcert(concert);
        }
        if (request.getBuyerId() != null) {
            User buyer = userRepository.findById(request.getBuyerId())
                    .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Buyer not found"));
            ticket.setBuyer(buyer);
        }
        ticket.setSeatNumber(request.getSeatNumber());
        if (request.getStatus() != null) {
            ticket.setStatus(request.getStatus());
        }
        ticket.setReservationExpiration(request.getReservationExpiration());
        return toResponse(ticketRepository.save(ticket));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','CASHIER')")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        if (!ticketRepository.existsById(id)) {
            throw new ResponseStatusException(NOT_FOUND, "Ticket not found");
        }
        ticketRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    private TicketResponse toResponse(Ticket ticket) {
        TicketResponse resp = new TicketResponse();
        resp.setId(ticket.getId());
        resp.setConcertId(ticket.getConcert() != null ? ticket.getConcert().getId() : null);
        resp.setBuyerId(ticket.getBuyer() != null ? ticket.getBuyer().getId() : null);
        resp.setSeatNumber(ticket.getSeatNumber());
        resp.setPurchaseTimestamp(ticket.getPurchaseTimestamp());
        resp.setStatus(ticket.getStatus());
        resp.setReservationExpiration(ticket.getReservationExpiration());
        return resp;
    }
}

