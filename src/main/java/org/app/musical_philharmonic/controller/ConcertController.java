package org.app.musical_philharmonic.controller;

import jakarta.validation.Valid;
import org.app.musical_philharmonic.dto.ConcertRequest;
import org.app.musical_philharmonic.dto.ConcertResponse;
import org.app.musical_philharmonic.entity.Concert;
import org.app.musical_philharmonic.entity.Hall;
import org.app.musical_philharmonic.entity.Performer;
import org.app.musical_philharmonic.repository.ConcertRepository;
import org.app.musical_philharmonic.repository.HallRepository;
import org.app.musical_philharmonic.repository.PerformerRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.Optional;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@RestController
@RequestMapping("/api/concerts")
public class ConcertController {

    private final ConcertRepository concertRepository;
    private final HallRepository hallRepository;
    private final PerformerRepository performerRepository;

    public ConcertController(ConcertRepository concertRepository,
                             HallRepository hallRepository,
                             PerformerRepository performerRepository) {
        this.concertRepository = concertRepository;
        this.hallRepository = hallRepository;
        this.performerRepository = performerRepository;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Page<ConcertResponse> list(
            @RequestParam Optional<LocalDate> date,
            @RequestParam Optional<LocalDate> startDate,
            @RequestParam Optional<LocalDate> endDate,
            @RequestParam Optional<Integer> performerId,
            @RequestParam Optional<Integer> hallId,
            @RequestParam Optional<String> title,
            Pageable pageable
    ) {
        Page<Concert> page = concertRepository.findAll(pageable);
        if (date.isPresent()) {
            page = concertRepository.findByDate(date.get(), pageable);
        } else if (startDate.isPresent() && endDate.isPresent()) {
            page = concertRepository.findByDateBetween(startDate.get(), endDate.get(), pageable);
        } else if (performerId.isPresent()) {
            page = concertRepository.findByPerformerId(performerId.get(), pageable);
        } else if (hallId.isPresent()) {
            page = concertRepository.findByHallId(hallId.get(), pageable);
        } else if (title.isPresent()) {
            page = concertRepository.findByTitleContainingIgnoreCase(title.get(), pageable);
        }
        return page.map(this::toResponse);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ConcertResponse get(@PathVariable Integer id) {
        return concertRepository.findById(id).map(this::toResponse)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Concert not found"));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ConcertResponse create(@Valid @RequestBody ConcertRequest request) {
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

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ConcertResponse update(@PathVariable Integer id, @Valid @RequestBody ConcertRequest request) {
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

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public void delete(@PathVariable Integer id) {
        if (!concertRepository.existsById(id)) {
            throw new ResponseStatusException(NOT_FOUND, "Concert not found");
        }
        concertRepository.deleteById(id);
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

