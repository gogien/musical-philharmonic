package org.app.musical_philharmonic.controller;

import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.app.musical_philharmonic.dto.ConcertRequest;
import org.app.musical_philharmonic.dto.ConcertResponse;
import org.app.musical_philharmonic.dto.ConcertSearchRequest;
import org.app.musical_philharmonic.service.ConcertService;
import org.app.musical_philharmonic.util.PageableUtil;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/concerts")
@Tag(name = "Concerts")
public class ConcertController {

    private final ConcertService concertService;

    public ConcertController(ConcertService concertService) {
        this.concertService = concertService;
    }

    @PostMapping("/search")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "List concerts with search/sort/pagination")
    public Page<ConcertResponse> list(@RequestBody ConcertSearchRequest request) {
        Pageable pageable = PageableUtil.toPageable(request.getPage(), request.getSize(), request.getSort());
        return concertService.list(request.getDate(), request.getStartDate(), request.getEndDate(),
                request.getPerformerId(), request.getHallId(), request.getTitle(), pageable);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get concert by id")
    public ConcertResponse get(@PathVariable Integer id) {
        return concertService.get(id);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create concert")
    public ConcertResponse create(@Valid @RequestBody ConcertRequest request) {
        return concertService.create(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update concert")
    public ConcertResponse update(@PathVariable Integer id, @Valid @RequestBody ConcertRequest request) {
        return concertService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete concert")
    public void delete(@PathVariable Integer id) {
        concertService.delete(id);
    }

    @GetMapping("/{id}/stats")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "View concert ticket stats (sold/available)")
    public Object stats(@PathVariable Integer id) {
        return concertService.stats(id);
    }
}

