package com.digitalbank.user.domain.repository;

import com.digitalbank.user.domain.model.User;
import com.digitalbank.user.domain.model.UserStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByCpf(String cpf);

    // Excludes the current user — used during email update validation
    boolean existsByEmailAndIdNot(String email, UUID id);

    Page<User> findAllByStatus(UserStatus status, Pageable pageable);
}
