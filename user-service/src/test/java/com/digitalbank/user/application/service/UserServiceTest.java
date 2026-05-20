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
import com.digitalbank.user.infrastructure.messaging.UserEventPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock UserRepository userRepository;
    @Mock UserMapper userMapper;
    @Mock UserEventPublisher eventPublisher;
    @InjectMocks UserService userService;

    private User sampleUser;
    private UserResponse sampleResponse;

    @BeforeEach
    void setUp() {
        sampleUser = User.builder()
                .id(UUID.randomUUID())
                .fullName("João da Silva")
                .email("joao@email.com")
                .cpf("529.982.247-25")
                .birthDate(LocalDate.of(1990, 5, 20))
                .status(UserStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        sampleResponse = new UserResponse(
                sampleUser.getId(), sampleUser.getFullName(), sampleUser.getEmail(),
                sampleUser.getCpf(), sampleUser.getBirthDate(), sampleUser.getStatus(),
                sampleUser.getCreatedAt()
        );
    }


    @Test
    void create_success_publishesEventAndReturnsResponse() {
        var request = new CreateUserRequest("João da Silva", "joao@email.com", "529.982.247-25", LocalDate.of(1990, 5, 20));

        when(userRepository.existsByEmail(request.email())).thenReturn(false);
        when(userRepository.existsByCpf(request.cpf())).thenReturn(false);
        when(userMapper.toEntity(request)).thenReturn(sampleUser);
        when(userRepository.save(sampleUser)).thenReturn(sampleUser);
        when(userMapper.toResponse(sampleUser)).thenReturn(sampleResponse);

        UserResponse result = userService.create(request);

        assertThat(result).isEqualTo(sampleResponse);
        verify(eventPublisher).publishUserCreated(any());
    }

    @Test
    void create_duplicateEmail_throws() {
        var request = new CreateUserRequest("João da Silva", "joao@email.com", "529.982.247-25", LocalDate.of(1990, 5, 20));
        when(userRepository.existsByEmail("joao@email.com")).thenReturn(true);

        assertThatThrownBy(() -> userService.create(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Email already in use");

        verify(userRepository, never()).save(any());
        verify(eventPublisher, never()).publishUserCreated(any());
    }

    @Test
    void create_duplicateCpf_throws() {
        var request = new CreateUserRequest("João da Silva", "joao@email.com", "529.982.247-25", LocalDate.of(1990, 5, 20));
        when(userRepository.existsByEmail("joao@email.com")).thenReturn(false);
        when(userRepository.existsByCpf("529.982.247-25")).thenReturn(true);

        assertThatThrownBy(() -> userService.create(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("CPF already registered");

        verify(userRepository, never()).save(any());
    }


    @Test
    void findById_exists_returnsResponse() {
        when(userRepository.findById(sampleUser.getId())).thenReturn(Optional.of(sampleUser));
        when(userMapper.toResponse(sampleUser)).thenReturn(sampleResponse);

        assertThat(userService.findById(sampleUser.getId())).isEqualTo(sampleResponse);
    }

    @Test
    void findById_notFound_throws() {
        UUID id = UUID.randomUUID();
        when(userRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.findById(id))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User");
    }


    @Test
    void update_inactiveUser_throws() {
        sampleUser.setStatus(UserStatus.INACTIVE);
        when(userRepository.findById(sampleUser.getId())).thenReturn(Optional.of(sampleUser));

        var request = new UpdateUserRequest("Novo Nome", null, null);

        assertThatThrownBy(() -> userService.update(sampleUser.getId(), request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("inactive");
    }

    @Test
    void update_emailAlreadyTakenByOtherUser_throws() {
        when(userRepository.findById(sampleUser.getId())).thenReturn(Optional.of(sampleUser));
        when(userRepository.existsByEmailAndIdNot("outro@email.com", sampleUser.getId())).thenReturn(true);

        var request = new UpdateUserRequest(null, "outro@email.com", null);

        assertThatThrownBy(() -> userService.update(sampleUser.getId(), request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Email already in use");
    }

    @Test
    void update_sameEmail_skipsUniquenessCheck() {
        when(userRepository.findById(sampleUser.getId())).thenReturn(Optional.of(sampleUser));
        when(userRepository.save(sampleUser)).thenReturn(sampleUser);
        when(userMapper.toResponse(sampleUser)).thenReturn(sampleResponse);

        // Updating with the same email — should not trigger the uniqueness check
        var request = new UpdateUserRequest(null, sampleUser.getEmail(), null);

        userService.update(sampleUser.getId(), request);

        verify(userRepository, never()).existsByEmailAndIdNot(any(), any());
    }


    @Test
    void deactivate_activeUser_setsInactive() {
        when(userRepository.findById(sampleUser.getId())).thenReturn(Optional.of(sampleUser));
        when(userRepository.save(sampleUser)).thenReturn(sampleUser);

        userService.deactivate(sampleUser.getId());

        assertThat(sampleUser.getStatus()).isEqualTo(UserStatus.INACTIVE);
        verify(userRepository).save(sampleUser);
    }

    @Test
    void deactivate_alreadyInactive_throws() {
        sampleUser.setStatus(UserStatus.INACTIVE);
        when(userRepository.findById(sampleUser.getId())).thenReturn(Optional.of(sampleUser));

        assertThatThrownBy(() -> userService.deactivate(sampleUser.getId()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("already inactive");
    }
}
