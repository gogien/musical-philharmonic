package org.app.musical_philharmonic.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.app.musical_philharmonic.dto.TicketResponse;
import org.app.musical_philharmonic.repository.TicketRepository;
import org.app.musical_philharmonic.repository.UserRepository;
import org.app.musical_philharmonic.service.ConcertService;
import org.app.musical_philharmonic.service.TicketService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@RestController
@RequestMapping("/api/customer")
@Tag(name = "Customer")
@PreAuthorize("hasAnyRole('ADMIN','CASHIER','CUSTOMER')")
public class CustomerController {

    private final ConcertService concertService;
    private final UserRepository userRepository;
    private final TicketService ticketService;
    private final TicketRepository ticketRepository;

    public CustomerController(ConcertService concertService,
                              UserRepository userRepository,
                              TicketService ticketService,
                              TicketRepository ticketRepository) {
        this.concertService = concertService;
        this.userRepository = userRepository;
        this.ticketService = ticketService;
        this.ticketRepository = ticketRepository;
    }

    @PostMapping("/concerts/upcoming")
    @Operation(summary = "View upcoming concerts")
    public ResponseEntity<?> upcoming(@RequestBody org.app.musical_philharmonic.dto.PageableRequest request) {
        Pageable pageable = org.app.musical_philharmonic.util.PageableUtil.toPageable(
                request.getPage(), request.getSize(), request.getSort());
        LocalDate today = LocalDate.now();
        return ResponseEntity.ok(concertService.upcoming(today, today.plusYears(1), pageable));
    }

    @PostMapping("/concerts/{id}/availability")
    @Operation(summary = "View seat availability for concert")
    public ResponseEntity<?> availability(@PathVariable Integer id,
                                           @RequestBody org.app.musical_philharmonic.dto.PageableRequest request) {
        Pageable pageable = org.app.musical_philharmonic.util.PageableUtil.toPageable(
                request.getPage(), request.getSize(), request.getSort());
        return ResponseEntity.ok(ticketService.availability(id, pageable));
    }

    @PostMapping("/tickets/book")
    @Operation(summary = "Book tickets (temporary reservation)")
    public java.util.List<org.app.musical_philharmonic.dto.TicketResponse> book(@RequestBody org.app.musical_philharmonic.dto.TicketBookRequest request,
                               Authentication auth) {
        UUID buyerId = userRepository.findByEmail(auth.getName())
                .map(u -> u.getId())
                .orElse(null);
        LocalDateTime exp = request.getMinutes() != null
                ? LocalDateTime.now().plusMinutes(request.getMinutes())
                : null;
        Integer quantity = request.getQuantity() != null ? request.getQuantity() : 1;
        return ticketService.book(request.getConcertId(), request.getSeatNumber(), buyerId, exp, auth.getName(), quantity);
    }

    @PostMapping("/tickets/purchase")
    @Operation(summary = "Purchase tickets")
    public java.util.List<org.app.musical_philharmonic.dto.TicketResponse> purchase(@RequestBody org.app.musical_philharmonic.dto.TicketPurchaseRequest request,
                                   Authentication auth) {
        UUID buyerId = userRepository.findByEmail(auth.getName())
                .map(u -> u.getId())
                .orElse(null);
        Integer quantity = request.getQuantity() != null ? request.getQuantity() : 1;
        return ticketService.purchase(request.getConcertId(), request.getSeatNumber(),
                buyerId, request.getPaymentMethod(), auth.getName(), quantity);
    }

    @PostMapping("/tickets/mine")
    @Operation(summary = "View purchased tickets for current user")
    public Page<TicketResponse> myTickets(@RequestBody org.app.musical_philharmonic.dto.PageableRequest request,
                                         Authentication auth) {
        Pageable pageable = org.app.musical_philharmonic.util.PageableUtil.toPageable(
                request.getPage(), request.getSize(), request.getSort());
        UUID buyerId = userRepository.findByEmail(auth.getName())
                .map(u -> u.getId())
                .orElse(null);
        return ticketService.ticketsByBuyer(buyerId, pageable);
    }

    @PostMapping("/tickets/{id}/return")
    @Operation(summary = "Return ticket (customer can only return their own tickets)")
    public TicketResponse returnTicket(@PathVariable Integer id,
                                       @RequestBody org.app.musical_philharmonic.dto.TicketReturnRequest request,
                                       Authentication auth) {
        UUID buyerId = userRepository.findByEmail(auth.getName())
                .map(u -> u.getId())
                .orElseThrow(() -> new ResponseStatusException(UNAUTHORIZED, "User not found"));
        
        // Verify that the ticket belongs to the current user
        org.app.musical_philharmonic.entity.Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Ticket not found"));
        
        if (ticket.getBuyer() == null || !buyerId.equals(ticket.getBuyer().getId())) {
            throw new ResponseStatusException(FORBIDDEN, "You can only return your own tickets");
        }
        
        return ticketService.returnTicket(id, request.getReason(), auth.getName());
    }
}

