package org.app.musical_philharmonic.service;

import org.app.musical_philharmonic.dto.PerformerRequest;
import org.app.musical_philharmonic.dto.PerformerResponse;
import org.app.musical_philharmonic.entity.Performer;
import org.app.musical_philharmonic.repository.PerformerRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class PerformerService {

    private final PerformerRepository performerRepository;

    public PerformerService(PerformerRepository performerRepository) {
        this.performerRepository = performerRepository;
    }

    public Page<PerformerResponse> list(String name, Pageable pageable) {
        Page<Performer> page = name == null
                ? performerRepository.findAll(pageable)
                : performerRepository.findByNameContainingIgnoreCase(name, pageable);
        return page.map(this::toResponse);
    }

    public PerformerResponse get(Integer id) {
        return performerRepository.findById(id).map(this::toResponse)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Performer not found"));
    }

    public PerformerResponse create(PerformerRequest request) {
        Performer performer = new Performer();
        performer.setName(request.getName());
        return toResponse(performerRepository.save(performer));
    }

    public PerformerResponse update(Integer id, PerformerRequest request) {
        Performer performer = performerRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Performer not found"));
        performer.setName(request.getName());
        return toResponse(performerRepository.save(performer));
    }

    public void delete(Integer id) {
        if (!performerRepository.existsById(id)) {
            throw new ResponseStatusException(NOT_FOUND, "Performer not found");
        }
        performerRepository.deleteById(id);
    }

    private PerformerResponse toResponse(Performer performer) {
        PerformerResponse resp = new PerformerResponse();
        resp.setId(performer.getId());
        resp.setName(performer.getName());
        return resp;
    }
}

