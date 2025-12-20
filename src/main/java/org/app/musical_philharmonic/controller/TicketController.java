package org.app.musical_philharmonic.controller;

import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.app.musical_philharmonic.dto.TicketRequest;
import org.app.musical_philharmonic.dto.TicketResponse;
import org.app.musical_philharmonic.entity.Concert;
import org.app.musical_philharmonic.entity.Ticket;
import org.app.musical_philharmonic.entity.User;
import org.app.musical_philharmonic.repository.ConcertRepository;
import org.app.musical_philharmonic.repository.TicketRepository;
import org.app.musical_philharmonic.repository.UserRepository;
import org.app.musical_philharmonic.service.TicketService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@RestController
@RequestMapping("/api/tickets")
@Tag(name = "Tickets")
public class TicketController {

    private final TicketRepository ticketRepository;
    private final ConcertRepository concertRepository;
    private final UserRepository userRepository;
    private final TicketService ticketService;

    public TicketController(TicketRepository ticketRepository,
                            ConcertRepository concertRepository,
                            UserRepository userRepository,
                            TicketService ticketService) {
        this.ticketRepository = ticketRepository;
        this.concertRepository = concertRepository;
        this.userRepository = userRepository;
        this.ticketService = ticketService;
    }

    @PostMapping("/search")
    @PreAuthorize("hasAnyRole('ADMIN','CASHIER')")
    @Operation(summary = "List tickets with filters and pagination")
    public Page<TicketResponse> list(@RequestBody org.app.musical_philharmonic.dto.TicketSearchRequest request) {
        Pageable pageable = org.app.musical_philharmonic.util.PageableUtil.toPageable(
                request.getPage(), request.getSize(), request.getSort());
        return ticketService.listTickets(
                java.util.Optional.ofNullable(request.getConcertId()),
                java.util.Optional.ofNullable(request.getBuyerId()),
                java.util.Optional.ofNullable(request.getConcertName()),
                java.util.Optional.ofNullable(request.getBuyerEmail()),
                java.util.Optional.ofNullable(request.getStatus()),
                pageable);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','CASHIER')")
    @Operation(summary = "Get ticket by id")
    public TicketResponse get(@PathVariable Integer id) {
        return ticketService.get(id);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','CASHIER')")
    @Operation(summary = "Create ticket (available/reserved/sold)")
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
        ticket.setPaymentMethod(request.getPaymentMethod());
        ticket.setReturnReason(request.getReturnReason());
        ticket.setReturnTime(request.getReturnTime());
        return ticketService.toResponse(ticketRepository.save(ticket));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','CASHIER')")
    @Operation(summary = "Update ticket")
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
        ticket.setPaymentMethod(request.getPaymentMethod());
        ticket.setReturnReason(request.getReturnReason());
        ticket.setReturnTime(request.getReturnTime());
        return ticketService.toResponse(ticketRepository.save(ticket));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','CASHIER')")
    @Operation(summary = "Delete ticket")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        if (!ticketRepository.existsById(id)) {
            throw new ResponseStatusException(NOT_FOUND, "Ticket not found");
        }
        ticketRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/sell")
    @PreAuthorize("hasAnyRole('ADMIN','CASHIER')")
    @Operation(summary = "Sell tickets with payment method")
    public java.util.List<org.app.musical_philharmonic.dto.TicketResponse> sell(@RequestBody org.app.musical_philharmonic.dto.TicketSellRequest request) {
        if (request.getBuyerEmail() == null || request.getBuyerEmail().trim().isEmpty()) {
            throw new ResponseStatusException(
                    org.springframework.http.HttpStatus.BAD_REQUEST, 
                    "Customer email is required");
        }
        String email = request.getBuyerEmail().trim();
        User buyer = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(
                        org.springframework.http.HttpStatus.BAD_REQUEST, 
                        "Customer with email " + email + " not found. Email must be registered."));
        // Seat number not needed for dance floor model - capacity check is performed in purchase()
        Integer quantity = request.getQuantity() != null ? request.getQuantity() : 1;
        return ticketService.purchase(request.getConcertId(), null,
                buyer.getId(), request.getPaymentMethod(),
                request.getActorEmail() != null ? request.getActorEmail() : "cashier", quantity);
    }

    @PostMapping("/{id}/return")
    @PreAuthorize("hasAnyRole('ADMIN','CASHIER')")
    @Operation(summary = "Return ticket with reason")
    public TicketResponse returnTicket(@PathVariable Integer id,
                                       @RequestBody org.app.musical_philharmonic.dto.TicketReturnRequest request) {
        return ticketService.returnTicket(id, request.getReason(),
                request.getActorEmail() != null ? request.getActorEmail() : "cashier");
    }

    @PostMapping("/sales")
    @PreAuthorize("hasAnyRole('ADMIN','CASHIER')")
    @Operation(summary = "Sales history by date range")
    public Page<TicketResponse> sales(@RequestBody org.app.musical_philharmonic.dto.SalesHistoryRequest request) {
        Pageable pageable = org.app.musical_philharmonic.util.PageableUtil.toPageable(
                request.getPage(), request.getSize(), request.getSort());
        return ticketService.salesHistory(request.getFrom(), request.getTo(), pageable);
    }

}

