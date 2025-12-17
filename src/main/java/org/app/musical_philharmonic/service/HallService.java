package org.app.musical_philharmonic.service;

import org.app.musical_philharmonic.dto.HallRequest;
import org.app.musical_philharmonic.dto.HallResponse;
import org.app.musical_philharmonic.entity.Hall;
import org.app.musical_philharmonic.repository.HallRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class HallService {

    private final HallRepository hallRepository;

    public HallService(HallRepository hallRepository) {
        this.hallRepository = hallRepository;
    }

    public Page<HallResponse> list(Pageable pageable) {
        return hallRepository.findAll(pageable).map(this::toResponse);
    }

    public HallResponse get(Integer id) {
        return hallRepository.findById(id).map(this::toResponse)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Hall not found"));
    }

    public HallResponse create(HallRequest request) {
        Hall hall = new Hall();
        hall.setName(request.getName());
        hall.setCapacity(request.getCapacity());
        hall.setLocation(request.getLocation());
        return toResponse(hallRepository.save(hall));
    }

    public HallResponse update(Integer id, HallRequest request) {
        Hall hall = hallRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Hall not found"));
        hall.setName(request.getName());
        hall.setCapacity(request.getCapacity());
        hall.setLocation(request.getLocation());
        return toResponse(hallRepository.save(hall));
    }

    public void delete(Integer id) {
        if (!hallRepository.existsById(id)) {
            throw new ResponseStatusException(NOT_FOUND, "Hall not found");
        }
        hallRepository.deleteById(id);
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

