package com.digitalbank.user.application.service;

import com.digitalbank.user.api.exception.BusinessException;
import com.digitalbank.user.api.exception.ResourceNotFoundException;
import com.digitalbank.user.application.dto.CreateUserRequest;
import com.digitalbank.user.application.dto.UpdateUserRequest;
import com.digitalbank.user.application.dto.UserResponse;
import com.digitalbank.user.application.mapper.UserMapper;
import com.digitalbank.user.domain.model.User;
import com.digitalbank.user.domain.model.UserStatus;
import com.digitalbank.user.domain.repository.UserRepository;
import com.digitalbank.user.infrastructure.messaging.UserCreatedEvent;
import com.digitalbank.user.infrastructure.messaging.UserEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final UserEventPublisher eventPublisher;

    @Transactional
    public UserResponse create(CreateUserRequest request) {
        log.info("Creating user with email: {}", request.email());

        if (userRepository.existsByEmail(request.email())) {
            throw new BusinessException("Email already in use: " + request.email());
        }
        if (userRepository.existsByCpf(request.cpf())) {
            throw new BusinessException("CPF already registered");
        }

        User user = userMapper.toEntity(request);
        User saved = userRepository.save(user);

        eventPublisher.publishUserCreated(new UserCreatedEvent(
                saved.getId(),
                saved.getEmail(),
                saved.getFullName(),
                LocalDateTime.now()
        ));

        log.info("User created successfully — id: {}", saved.getId());
        return userMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public UserResponse findById(UUID id) {
        log.debug("Fetching user — id: {}", id);
        return userMapper.toResponse(findUserOrThrow(id));
    }

    @Transactional(readOnly = true)
    public Page<UserResponse> findAll(Pageable pageable) {
        log.debug("Listing users — page: {}, size: {}", pageable.getPageNumber(), pageable.getPageSize());
        return userRepository.findAll(pageable).map(userMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<UserResponse> findAllByStatus(UserStatus status, Pageable pageable) {
        log.debug("Listing users by status: {}", status);
        return userRepository.findAllByStatus(status, pageable).map(userMapper::toResponse);
    }

    @Transactional
    public UserResponse update(UUID id, UpdateUserRequest request) {
        log.info("Updating user — id: {}", id);

        User user = findUserOrThrow(id);

        if (user.getStatus() == UserStatus.INACTIVE) {
            throw new BusinessException("Cannot update an inactive user");
        }

        if (request.email() != null && !request.email().equals(user.getEmail())) {
            if (userRepository.existsByEmailAndIdNot(request.email(), id)) {
                throw new BusinessException("Email already in use: " + request.email());
            }
        }

        userMapper.updateEntity(request, user);
        User updated = userRepository.save(user);

        log.info("User updated successfully — id: {}", updated.getId());
        return userMapper.toResponse(updated);
    }

    @Transactional
    public void deactivate(UUID id) {
        log.info("Deactivating user — id: {}", id);

        User user = findUserOrThrow(id);

        if (user.getStatus() == UserStatus.INACTIVE) {
            throw new BusinessException("User is already inactive");
        }

        user.setStatus(UserStatus.INACTIVE);
        userRepository.save(user);

        log.info("User deactivated — id: {}", id);
    }

    private User findUserOrThrow(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", id));
    }
}
