package org.app.musical_philharmonic.controller;

import jakarta.validation.Valid;
import org.app.musical_philharmonic.dto.PerformerRequest;
import org.app.musical_philharmonic.dto.PerformerResponse;
import org.app.musical_philharmonic.entity.Performer;
import org.app.musical_philharmonic.repository.PerformerRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@RestController
@RequestMapping("/api/performers")
public class PerformerController {

    private final PerformerRepository performerRepository;

    public PerformerController(PerformerRepository performerRepository) {
        this.performerRepository = performerRepository;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Page<PerformerResponse> list(@RequestParam(required = false) String name, Pageable pageable) {
        Page<Performer> page = name == null
                ? performerRepository.findAll(pageable)
                : performerRepository.findByNameContainingIgnoreCase(name, pageable);
        return page.map(this::toResponse);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public PerformerResponse get(@PathVariable Integer id) {
        return performerRepository.findById(id).map(this::toResponse)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Performer not found"));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public PerformerResponse create(@Valid @RequestBody PerformerRequest request) {
        Performer performer = new Performer();
        performer.setName(request.getName());
        return toResponse(performerRepository.save(performer));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public PerformerResponse update(@PathVariable Integer id, @Valid @RequestBody PerformerRequest request) {
        Performer performer = performerRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Performer not found"));
        performer.setName(request.getName());
        return toResponse(performerRepository.save(performer));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        if (!performerRepository.existsById(id)) {
            throw new ResponseStatusException(NOT_FOUND, "Performer not found");
        }
        performerRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    private PerformerResponse toResponse(Performer performer) {
        PerformerResponse resp = new PerformerResponse();
        resp.setId(performer.getId());
        resp.setName(performer.getName());
        return resp;
    }
}

