package org.app.musical_philharmonic.controller;

import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.app.musical_philharmonic.dto.PerformerRequest;
import org.app.musical_philharmonic.dto.PerformerResponse;
import org.app.musical_philharmonic.service.PerformerService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/performers")
@Tag(name = "Performers")
public class PerformerController {

    private final PerformerService performerService;

    public PerformerController(PerformerService performerService) {
        this.performerService = performerService;
    }

    @PostMapping("/search")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "List performers (filter by name)")
    public Page<PerformerResponse> list(@RequestBody org.app.musical_philharmonic.dto.PerformerSearchRequest request) {
        Pageable pageable = org.app.musical_philharmonic.util.PageableUtil.toPageable(
                request.getPage(), request.getSize(), request.getSort());
        return performerService.list(request.getName(), pageable);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get performer by id")
    public PerformerResponse get(@PathVariable Integer id) {
        return performerService.get(id);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create performer")
    public PerformerResponse create(@Valid @RequestBody PerformerRequest request) {
        return performerService.create(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update performer")
    public PerformerResponse update(@PathVariable Integer id, @Valid @RequestBody PerformerRequest request) {
        return performerService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete performer")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        performerService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/public/list")
    @Operation(summary = "List performers (public access)")
    public Page<PerformerResponse> publicList(@RequestBody org.app.musical_philharmonic.dto.PerformerSearchRequest request) {
        Pageable pageable = org.app.musical_philharmonic.util.PageableUtil.toPageable(
                request.getPage(), request.getSize(), request.getSort());
        return performerService.list(request.getName(), pageable);
    }
}

