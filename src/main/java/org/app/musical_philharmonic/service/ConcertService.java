package org.app.musical_philharmonic.service;

import org.app.musical_philharmonic.dto.ConcertRequest;
import org.app.musical_philharmonic.dto.ConcertResponse;
import org.app.musical_philharmonic.entity.Concert;
import org.app.musical_philharmonic.entity.Hall;
import org.app.musical_philharmonic.entity.Performer;
import org.app.musical_philharmonic.repository.ConcertRepository;
import org.app.musical_philharmonic.repository.HallRepository;
import org.app.musical_philharmonic.repository.PerformerRepository;
import org.app.musical_philharmonic.repository.TicketRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class ConcertService {

    private final ConcertRepository concertRepository;
    private final HallRepository hallRepository;
    private final PerformerRepository performerRepository;
    private final TicketRepository ticketRepository;

    public ConcertService(ConcertRepository concertRepository,
                          HallRepository hallRepository,
                          PerformerRepository performerRepository,
                          TicketRepository ticketRepository) {
        this.concertRepository = concertRepository;
        this.hallRepository = hallRepository;
        this.performerRepository = performerRepository;
        this.ticketRepository = ticketRepository;
    }

    public Page<ConcertResponse> list(LocalDate date,
                                      LocalDate startDate,
                                      LocalDate endDate,
                                      Integer performerId,
                                      Integer hallId,
                                      String title,
                                      Pageable pageable) {
        Page<Concert> page = concertRepository.findAll(pageable);
        if (date != null) {
            page = concertRepository.findByDate(date, pageable);
        } else if (startDate != null && endDate != null) {
            page = concertRepository.findByDateBetween(startDate, endDate, pageable);
        } else if (performerId != null) {
            page = concertRepository.findByPerformerId(performerId, pageable);
        } else if (hallId != null) {
            page = concertRepository.findByHallId(hallId, pageable);
        } else if (title != null && !title.isEmpty()) {
            page = concertRepository.findByTitleContainingIgnoreCase(title, pageable);
        }
        return page.map(this::toResponse);
    }

    public Page<ConcertResponse> upcoming(LocalDate from, LocalDate to, Pageable pageable) {
        return concertRepository.findByDateBetween(from, to, pageable).map(this::toResponse);
    }

    public ConcertResponse get(Integer id) {
        return concertRepository.findById(id).map(this::toResponse)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Concert not found"));
    }

    public ConcertResponse create(ConcertRequest request) {
        Hall hall = hallRepository.findById(request.getHallId())
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Hall not found"));
        Performer performer = performerRepository.findById(request.getPerformerId())
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Performer not found"));
        Concert concert = new Concert();
        concert.setTitle(request.getTitle());
        concert.setDate(request.getDate());
        concert.setTime(request.getTime());
        concert.setHall(hall);
        concert.setPerformer(performer);
        concert.setTicketPrice(request.getTicketPrice());
        return toResponse(concertRepository.save(concert));
    }

    public ConcertResponse update(Integer id, ConcertRequest request) {
        Concert concert = concertRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Concert not found"));
        Hall hall = hallRepository.findById(request.getHallId())
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Hall not found"));
        Performer performer = performerRepository.findById(request.getPerformerId())
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Performer not found"));

        concert.setTitle(request.getTitle());
        concert.setDate(request.getDate());
        concert.setTime(request.getTime());
        concert.setHall(hall);
        concert.setPerformer(performer);
        concert.setTicketPrice(request.getTicketPrice());
        return toResponse(concertRepository.save(concert));
    }

    public void delete(Integer id) {
        if (!concertRepository.existsById(id)) {
            throw new ResponseStatusException(NOT_FOUND, "Concert not found");
        }
        concertRepository.deleteById(id);
    }

    public Object stats(Integer id) {
        if (!concertRepository.existsById(id)) {
            throw new ResponseStatusException(NOT_FOUND, "Concert not found");
        }
        long sold = ticketRepository.countSoldByConcert(id);
        long available = ticketRepository.countAvailableByConcert(id);
        return java.util.Map.of("concertId", id, "sold", sold, "available", available);
    }

    public long getAvailableTicketsCount(Integer id) {
        Concert concert = concertRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Concert not found"));
        int hallCapacity = concert.getHall().getCapacity();
        long bookedTickets = ticketRepository.countReservedOrSoldByConcert(id);
        return Math.max(0, hallCapacity - bookedTickets);
    }

    private ConcertResponse toResponse(Concert concert) {
        ConcertResponse resp = new ConcertResponse();
        resp.setId(concert.getId());
        resp.setTitle(concert.getTitle());
        resp.setDate(concert.getDate());
        resp.setTime(concert.getTime());
        resp.setHallId(concert.getHall() != null ? concert.getHall().getId() : null);
        resp.setPerformerId(concert.getPerformer() != null ? concert.getPerformer().getId() : null);
        resp.setTicketPrice(concert.getTicketPrice());
        return resp;
    }
}

