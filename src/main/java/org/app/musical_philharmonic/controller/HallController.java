package org.app.musical_philharmonic.controller;

import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.app.musical_philharmonic.dto.HallRequest;
import org.app.musical_philharmonic.dto.HallResponse;
import org.app.musical_philharmonic.service.HallService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/halls")
@Tag(name = "Halls")
public class HallController {

    private final HallService hallService;

    public HallController(HallService hallService) {
        this.hallService = hallService;
    }

    @PostMapping("/search")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "List halls")
    public Page<HallResponse> list(@RequestBody org.app.musical_philharmonic.dto.PageableRequest request) {
        Pageable pageable = org.app.musical_philharmonic.util.PageableUtil.toPageable(
                request.getPage(), request.getSize(), request.getSort());
        return hallService.list(pageable);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get hall by id")
    public HallResponse get(@PathVariable Integer id) {
        return hallService.get(id);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create hall")
    public HallResponse create(@Valid @RequestBody HallRequest request) {
        return hallService.create(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update hall")
    public HallResponse update(@PathVariable Integer id, @Valid @RequestBody HallRequest request) {
        return hallService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete hall")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        hallService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/public/{id}")
    @Operation(summary = "Get hall by id (public access)")
    public HallResponse publicGet(@PathVariable Integer id) {
        return hallService.get(id);
    }
}

