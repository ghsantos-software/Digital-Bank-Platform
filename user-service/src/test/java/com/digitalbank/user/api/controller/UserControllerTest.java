package com.digitalbank.user.api.controller;

import com.digitalbank.user.api.exception.BusinessException;
import com.digitalbank.user.api.exception.ResourceNotFoundException;
import com.digitalbank.user.application.dto.CreateUserRequest;
import com.digitalbank.user.application.dto.UserResponse;
import com.digitalbank.user.application.service.UserService;
import com.digitalbank.user.domain.model.UserStatus;
import com.digitalbank.user.infrastructure.config.SecurityConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
@Import(SecurityConfig.class)
class UserControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockBean UserService userService;
    @MockBean JwtDecoder jwtDecoder; // prevents startup from contacting Keycloak

    private static final String BASE_URL = "/api/v1/users";

    private UserResponse sampleResponse() {
        return new UserResponse(
                UUID.randomUUID(), "João da Silva", "joao@email.com",
                "529.982.247-25", LocalDate.of(1990, 5, 20),
                UserStatus.ACTIVE, LocalDateTime.now()
        );
    }

    // ─── POST /api/v1/users (public) ─────────────────────────────────────────

    @Test
    void createUser_validPayload_returns201() throws Exception {
        var request = new CreateUserRequest("João da Silva", "joao@email.com", "529.982.247-25", LocalDate.of(1990, 5, 20));
        when(userService.create(any())).thenReturn(sampleResponse());

        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("joao@email.com"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void createUser_missingEmail_returns400() throws Exception {
        var request = new CreateUserRequest("João da Silva", null, "529.982.247-25", LocalDate.of(1990, 5, 20));

        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void createUser_duplicateEmail_returns422() throws Exception {
        var request = new CreateUserRequest("João da Silva", "joao@email.com", "529.982.247-25", LocalDate.of(1990, 5, 20));
        when(userService.create(any())).thenThrow(new BusinessException("Email already in use: joao@email.com"));

        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message").value("Email already in use: joao@email.com"));
    }

    // ─── GET /api/v1/users (ADMIN only) ──────────────────────────────────────

    @Test
    void listUsers_withoutAuth_returns401() throws Exception {
        mockMvc.perform(get(BASE_URL))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void listUsers_asCustomer_returns403() throws Exception {
        mockMvc.perform(get(BASE_URL)
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CUSTOMER"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void listUsers_asAdmin_returns200() throws Exception {
        when(userService.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(sampleResponse())));

        mockMvc.perform(get(BASE_URL)
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].email").value("joao@email.com"));
    }

    // ─── GET /api/v1/users/{id} (authenticated) ──────────────────────────────

    @Test
    void getUser_withoutAuth_returns401() throws Exception {
        mockMvc.perform(get(BASE_URL + "/" + UUID.randomUUID()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getUser_asCustomer_returns200() throws Exception {
        UUID id = UUID.randomUUID();
        when(userService.findById(id)).thenReturn(sampleResponse());

        mockMvc.perform(get(BASE_URL + "/" + id)
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CUSTOMER"))))
                .andExpect(status().isOk());
    }

    @Test
    void getUser_notFound_returns404() throws Exception {
        UUID id = UUID.randomUUID();
        when(userService.findById(id)).thenThrow(new ResourceNotFoundException("User", id));

        mockMvc.perform(get(BASE_URL + "/" + id)
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CUSTOMER"))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    // ─── DELETE /api/v1/users/{id} (ADMIN only) ──────────────────────────────

    @Test
    void deleteUser_asCustomer_returns403() throws Exception {
        mockMvc.perform(delete(BASE_URL + "/" + UUID.randomUUID())
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CUSTOMER"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void deleteUser_asAdmin_returns204() throws Exception {
        UUID id = UUID.randomUUID();
        doNothing().when(userService).deactivate(id);

        mockMvc.perform(delete(BASE_URL + "/" + id)
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isNoContent());
    }
}
