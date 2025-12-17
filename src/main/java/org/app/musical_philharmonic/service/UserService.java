package org.app.musical_philharmonic.service;

import org.app.musical_philharmonic.dto.UserCreateRequest;
import org.app.musical_philharmonic.dto.UserResponse;
import org.app.musical_philharmonic.dto.UserUpdateRequest;
import org.app.musical_philharmonic.entity.Role;
import org.app.musical_philharmonic.entity.User;
import org.app.musical_philharmonic.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public Page<UserResponse> list(String name, String email, Role role, Pageable pageable) {
        Page<User> page = userRepository.findAll(pageable);
        if (name != null && !name.isEmpty()) {
            page = userRepository.findByNameContainingIgnoreCase(name, pageable);
        } else if (email != null && !email.isEmpty()) {
            page = userRepository.findByEmailContainingIgnoreCase(email, pageable);
        } else if (role != null) {
            page = userRepository.findByRole(role, pageable);
        }
        return page.map(this::toResponse);
    }

    public UserResponse get(UUID id) {
        return userRepository.findById(id).map(this::toResponse)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "User not found"));
    }

    public UserResponse create(UserCreateRequest request) {
        User user = new User();
        user.setEmail(request.getEmail());
        user.setName(request.getName());
        user.setPhone(request.getPhone());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(request.getRole() != null ? request.getRole() : Role.CUSTOMER);
        return toResponse(userRepository.save(user));
    }

    public UserResponse update(UUID id, UserUpdateRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "User not found"));
        if (request.getName() != null) user.setName(request.getName());
        if (request.getPhone() != null) user.setPhone(request.getPhone());
        if (request.getRole() != null) user.setRole(request.getRole());
        if (request.getPassword() != null) user.setPassword(passwordEncoder.encode(request.getPassword()));
        return toResponse(userRepository.save(user));
    }

    public void delete(UUID id) {
        if (!userRepository.existsById(id)) {
            throw new ResponseStatusException(NOT_FOUND, "User not found");
        }
        userRepository.deleteById(id);
    }

    private UserResponse toResponse(User user) {
        UserResponse resp = new UserResponse();
        resp.setId(user.getId());
        resp.setEmail(user.getEmail());
        resp.setName(user.getName());
        resp.setPhone(user.getPhone());
        resp.setCreatedAt(user.getCreatedAt());
        resp.setRole(user.getRole());
        return resp;
    }
}

