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

    @Transactional(readOnly = true)
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

    public long getAvailableTicketsCount(Integer concertId) {
        Concert concert = concertRepository.findById(concertId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Concert not found"));
        int hallCapacity = concert.getHall().getCapacity();
        long bookedTickets = ticketRepository.countReservedOrSoldByConcert(concertId);
        return Math.max(0, hallCapacity - bookedTickets);
    }

    public Page<TicketResponse> ticketsByBuyer(UUID buyerId, Pageable pageable) {
        return ticketRepository.findByBuyerId(buyerId, pageable).map(this::toResponse);
    }

    public TicketResponse get(Integer id) {
        return ticketRepository.findById(id).map(this::toResponse)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Ticket not found"));
    }

    @Transactional
    public java.util.List<TicketResponse> book(Integer concertId, String seatNumber, UUID buyerId, LocalDateTime expiration, String actorEmail, Integer quantity) {
        Concert concert = concertRepository.findById(concertId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Concert not found"));
        
        if (quantity == null || quantity <= 0) {
            quantity = 1;
        }
        
        // Check hall capacity BEFORE creating any tickets - ensure we can book all requested tickets
        long existingTickets = ticketRepository.countReservedOrSoldByConcert(concertId);
        int hallCapacity = concert.getHall().getCapacity();
        
        if (existingTickets + quantity > hallCapacity) {
            long available = hallCapacity - existingTickets;
            throw new ResponseStatusException(BAD_REQUEST, 
                String.format("Cannot book %d tickets. Hall capacity: %d, Already booked/sold: %d, Available: %d", 
                    quantity, hallCapacity, existingTickets, available));
        }
        
        User buyer = null;
        if (buyerId != null) {
            buyer = userRepository.findById(buyerId)
                    .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Buyer not found"));
        }
        
        java.util.List<Ticket> savedTickets = new java.util.ArrayList<>();
        LocalDateTime exp = expiration != null ? expiration : LocalDateTime.now().plusMinutes(30);
        String seatNum = seatNumber != null && !seatNumber.trim().isEmpty() ? seatNumber : "N/A";
        
        // Create all requested tickets
        for (int i = 0; i < quantity; i++) {
            Ticket ticket = new Ticket();
            ticket.setConcert(concert);
            ticket.setSeatNumber(seatNum);
            ticket.setStatus(TicketStatus.RESERVED);
            ticket.setReservationExpiration(exp);
            if (buyer != null) {
                ticket.setBuyer(buyer);
            }
            savedTickets.add(ticketRepository.save(ticket));
        }
        
        log.info("Booked {} tickets concert={} by={} (capacity: {}/{})", quantity, concertId, actorEmail, existingTickets + quantity, hallCapacity);
        return savedTickets.stream().map(this::toResponse).collect(java.util.stream.Collectors.toList());
    }

    @Transactional
    public java.util.List<TicketResponse> purchase(Integer concertId, String seatNumber, UUID buyerId, String paymentMethod, String actorEmail, Integer quantity) {
        Concert concert = concertRepository.findById(concertId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Concert not found"));
        
        if (quantity == null || quantity <= 0) {
            quantity = 1;
        }
        
        User buyer = null;
        if (buyerId != null) {
            buyer = userRepository.findById(buyerId)
                    .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Buyer not found"));
        }
        
        java.util.List<Ticket> ticketsToSell = new java.util.ArrayList<>();
        
        // If buyer is provided, find their reserved tickets first (convert reserved to sold)
        if (buyer != null) {
            java.util.List<Ticket> reservedTickets = ticketRepository.findByConcertIdAndStatus(concertId, TicketStatus.RESERVED, Pageable.unpaged())
                    .getContent().stream()
                    .filter(t -> t.getBuyer() != null && buyerId.equals(t.getBuyer().getId()))
                    .limit(quantity)
                    .collect(java.util.stream.Collectors.toList());
            
            ticketsToSell.addAll(reservedTickets);
        }
        
        // Calculate how many new tickets we need to create
        int newTicketsNeeded = quantity - ticketsToSell.size();
        
        if (newTicketsNeeded > 0) {
            // Check capacity BEFORE creating new tickets
            long existingTickets = ticketRepository.countReservedOrSoldByConcert(concertId);
            int hallCapacity = concert.getHall().getCapacity();
            
            if (existingTickets + newTicketsNeeded > hallCapacity) {
                long available = hallCapacity - existingTickets;
                throw new ResponseStatusException(BAD_REQUEST, 
                    String.format("Cannot purchase %d tickets. Hall capacity: %d, Already booked/sold: %d, Available: %d", 
                        quantity, hallCapacity, existingTickets, available));
            }
            
            // Create new tickets for the remaining quantity
            String seatNum = seatNumber != null && !seatNumber.trim().isEmpty() ? seatNumber : "N/A";
            for (int i = 0; i < newTicketsNeeded; i++) {
                Ticket ticket = new Ticket();
                ticket.setConcert(concert);
                ticket.setSeatNumber(seatNum);
                ticket.setStatus(TicketStatus.SOLD);
                ticket.setPaymentMethod(paymentMethod);
                if (buyer != null) {
                    ticket.setBuyer(buyer);
                }
                ticketsToSell.add(ticketRepository.save(ticket));
            }
        }
        
        // Convert reserved tickets to sold
        for (Ticket ticket : ticketsToSell) {
            if (ticket.getStatus() == TicketStatus.RESERVED) {
                ticket.setStatus(TicketStatus.SOLD);
                ticket.setReservationExpiration(null);
                ticket.setPaymentMethod(paymentMethod);
                ticketRepository.save(ticket);
            }
        }
        
        log.info("Purchased {} tickets concert={} by={} payment={}", quantity, concertId, actorEmail, paymentMethod);
        return ticketsToSell.stream().map(this::toResponse).collect(java.util.stream.Collectors.toList());
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
        if (ticket.getConcert() != null) {
            resp.setConcertId(ticket.getConcert().getId());
            resp.setConcertName(ticket.getConcert().getTitle());
        }
        if (ticket.getBuyer() != null) {
            resp.setBuyerId(ticket.getBuyer().getId());
            resp.setBuyerEmail(ticket.getBuyer().getEmail());
        }
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

