package org.app.musical_philharmonic.controller;

import jakarta.validation.Valid;
import org.app.musical_philharmonic.dto.HallRequest;
import org.app.musical_philharmonic.dto.HallResponse;
import org.app.musical_philharmonic.entity.Hall;
import org.app.musical_philharmonic.repository.HallRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@RestController
@RequestMapping("/api/halls")
public class HallController {

    private final HallRepository hallRepository;

    public HallController(HallRepository hallRepository) {
        this.hallRepository = hallRepository;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Page<HallResponse> list(Pageable pageable) {
        return hallRepository.findAll(pageable).map(this::toResponse);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public HallResponse get(@PathVariable Integer id) {
        return hallRepository.findById(id).map(this::toResponse)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Hall not found"));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public HallResponse create(@Valid @RequestBody HallRequest request) {
        Hall hall = new Hall();
        hall.setName(request.getName());
        hall.setCapacity(request.getCapacity());
        hall.setLocation(request.getLocation());
        return toResponse(hallRepository.save(hall));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public HallResponse update(@PathVariable Integer id, @Valid @RequestBody HallRequest request) {
        Hall hall = hallRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Hall not found"));
        hall.setName(request.getName());
        hall.setCapacity(request.getCapacity());
        hall.setLocation(request.getLocation());
        return toResponse(hallRepository.save(hall));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        if (!hallRepository.existsById(id)) {
            throw new ResponseStatusException(NOT_FOUND, "Hall not found");
        }
        hallRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    private HallResponse toResponse(Hall hall) {
        HallResponse resp = new HallResponse();
        resp.setId(hall.getId());
        resp.setName(hall.getName());
        resp.setCapacity(hall.getCapacity());
        resp.setLocation(hall.getLocation());
        return resp;
    }
}

