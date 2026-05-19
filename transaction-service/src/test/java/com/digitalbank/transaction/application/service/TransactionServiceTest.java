package com.digitalbank.transaction.application.service;

import com.digitalbank.transaction.api.exception.BusinessException;
import com.digitalbank.transaction.api.exception.ResourceNotFoundException;
import com.digitalbank.transaction.application.dto.CreateTransactionRequest;
import com.digitalbank.transaction.application.dto.TransactionResponse;
import com.digitalbank.transaction.application.mapper.TransactionMapper;
import com.digitalbank.transaction.domain.model.Transaction;
import com.digitalbank.transaction.domain.model.TransactionStatus;
import com.digitalbank.transaction.domain.model.TransactionType;
import com.digitalbank.transaction.domain.repository.TransactionRepository;
import com.digitalbank.transaction.infrastructure.client.AccountClient;
import com.digitalbank.transaction.infrastructure.client.dto.AccountSummary;
import com.digitalbank.transaction.infrastructure.messaging.TransactionEventPublisher;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock TransactionRepository transactionRepository;
    @Mock TransactionMapper transactionMapper;
    @Mock AccountClient accountClient;
    @Mock TransactionEventPublisher eventPublisher;
    @InjectMocks TransactionService transactionService;

    private UUID sourceAccountId;
    private UUID destinationAccountId;
    private AccountSummary activeAccount;

    @BeforeEach
    void setUp() {
        sourceAccountId = UUID.randomUUID();
        destinationAccountId = UUID.randomUUID();
        activeAccount = new AccountSummary(sourceAccountId, "10000001", "0001", "CHECKING", new BigDecimal("2000.00"), "ACTIVE");

        // Simulate authenticated user (JWT subject = user UUID)
        var jwt = Jwt.withTokenValue("test-token")
                .header("alg", "RS256")
                .subject("keycloak-user-123")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(300))
                .build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt, List.of()));
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    // ─── idempotency ─────────────────────────────────────────────────────────

    @Test
    void create_duplicateIdempotencyKey_returnsExistingTransaction() {
        UUID idempotencyKey = UUID.randomUUID();
        var existingTx = pendingTransaction(idempotencyKey);
        var existingResponse = pendingResponse(existingTx);

        when(transactionRepository.findByIdempotencyKey(idempotencyKey)).thenReturn(Optional.of(existingTx));
        when(transactionMapper.toResponse(existingTx)).thenReturn(existingResponse);

        var request = depositRequest(sourceAccountId, new BigDecimal("100.00"), idempotencyKey);
        TransactionResponse result = transactionService.create(request);

        assertThat(result.id()).isEqualTo(existingTx.getId());
        verify(transactionRepository, never()).save(any());
        verify(eventPublisher, never()).publishTransactionCreated(any());
    }

    @Test
    void create_noIdempotencyKey_generatesOne() {
        var savedTx = pendingTransaction(UUID.randomUUID());
        var savedResponse = pendingResponse(savedTx);

        when(transactionRepository.findByIdempotencyKey(any())).thenReturn(Optional.empty());
        when(accountClient.findById(sourceAccountId)).thenReturn(activeAccount);
        when(transactionRepository.save(any())).thenReturn(savedTx);
        when(transactionMapper.toResponse(savedTx)).thenReturn(savedResponse);

        // Request without an idempotency key
        var request = new CreateTransactionRequest(null, sourceAccountId, null, new BigDecimal("100.00"), TransactionType.DEPOSIT, null);

        transactionService.create(request);

        verify(transactionRepository).save(any());
        verify(eventPublisher).publishTransactionCreated(any());
    }

    // ─── validation ──────────────────────────────────────────────────────────

    @Test
    void create_transfer_missingDestination_throws() {
        when(transactionRepository.findByIdempotencyKey(any())).thenReturn(Optional.empty());

        var request = new CreateTransactionRequest(null, sourceAccountId, null, new BigDecimal("100.00"), TransactionType.TRANSFER, null);

        assertThatThrownBy(() -> transactionService.create(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Destination account is required");

        verify(transactionRepository, never()).save(any());
    }

    @Test
    void create_transfer_sameSourceAndDestination_throws() {
        when(transactionRepository.findByIdempotencyKey(any())).thenReturn(Optional.empty());

        var request = new CreateTransactionRequest(null, sourceAccountId, sourceAccountId, new BigDecimal("100.00"), TransactionType.TRANSFER, null);

        assertThatThrownBy(() -> transactionService.create(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("must be different");
    }

    @Test
    void create_withdrawal_insufficientBalance_throws() {
        var poorAccount = new AccountSummary(sourceAccountId, "10000001", "0001", "CHECKING", new BigDecimal("50.00"), "ACTIVE");
        when(transactionRepository.findByIdempotencyKey(any())).thenReturn(Optional.empty());
        when(accountClient.findById(sourceAccountId)).thenReturn(poorAccount);

        var request = new CreateTransactionRequest(null, sourceAccountId, null, new BigDecimal("200.00"), TransactionType.WITHDRAWAL, null);

        assertThatThrownBy(() -> transactionService.create(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Saldo insuficiente")
                .hasMessageContaining("50.00")
                .hasMessageContaining("200.00");
    }

    @Test
    void create_inactiveAccount_throws() {
        var blockedAccount = new AccountSummary(sourceAccountId, "10000001", "0001", "CHECKING", new BigDecimal("1000.00"), "BLOCKED");
        when(transactionRepository.findByIdempotencyKey(any())).thenReturn(Optional.empty());
        when(accountClient.findById(sourceAccountId)).thenReturn(blockedAccount);

        var request = depositRequest(sourceAccountId, new BigDecimal("100.00"), null);

        assertThatThrownBy(() -> transactionService.create(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("não está ativa")
                .hasMessageContaining("BLOCKED");
    }

    // ─── performedBy (audit) ─────────────────────────────────────────────────

    @Test
    void create_capturesJwtSubjectAsPerformedBy() {
        var savedTx = pendingTransaction(UUID.randomUUID());
        var savedResponse = pendingResponse(savedTx);

        when(transactionRepository.findByIdempotencyKey(any())).thenReturn(Optional.empty());
        when(accountClient.findById(sourceAccountId)).thenReturn(activeAccount);
        when(transactionRepository.save(any())).thenAnswer(inv -> {
            Transaction tx = inv.getArgument(0);
            assertThat(tx.getPerformedBy()).isEqualTo("keycloak-user-123");
            return savedTx;
        });
        when(transactionMapper.toResponse(savedTx)).thenReturn(savedResponse);

        transactionService.create(depositRequest(sourceAccountId, new BigDecimal("100.00"), null));
    }

    // ─── status transitions ──────────────────────────────────────────────────

    @Test
    void markCompleted_setsStatusCompleted() {
        var tx = pendingTransaction(UUID.randomUUID());
        when(transactionRepository.findById(tx.getId())).thenReturn(Optional.of(tx));
        when(transactionRepository.save(tx)).thenReturn(tx);

        transactionService.markCompleted(tx.getId());

        assertThat(tx.getStatus()).isEqualTo(TransactionStatus.COMPLETED);
    }

    @Test
    void markFailed_setsStatusAndReason() {
        var tx = pendingTransaction(UUID.randomUUID());
        when(transactionRepository.findById(tx.getId())).thenReturn(Optional.of(tx));
        when(transactionRepository.save(tx)).thenReturn(tx);

        transactionService.markFailed(tx.getId(), "Saldo insuficiente na conta 10000001");

        assertThat(tx.getStatus()).isEqualTo(TransactionStatus.FAILED);
        assertThat(tx.getFailureReason()).isEqualTo("Saldo insuficiente na conta 10000001");
    }

    @Test
    void findById_notFound_throws() {
        UUID id = UUID.randomUUID();
        when(transactionRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> transactionService.findById(id))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ─── helpers ─────────────────────────────────────────────────────────────

    private Transaction pendingTransaction(UUID idempotencyKey) {
        return Transaction.builder()
                .id(UUID.randomUUID())
                .idempotencyKey(idempotencyKey)
                .sourceAccountId(sourceAccountId)
                .amount(new BigDecimal("100.00"))
                .type(TransactionType.DEPOSIT)
                .status(TransactionStatus.PENDING)
                .build();
    }

    private TransactionResponse pendingResponse(Transaction tx) {
        return new TransactionResponse(
                tx.getId(), tx.getIdempotencyKey(), tx.getSourceAccountId(), null,
                tx.getAmount(), tx.getType(), "Depósito",
                TransactionStatus.PENDING, "Pendente",
                null, null, null, null, null
        );
    }

    private CreateTransactionRequest depositRequest(UUID accountId, BigDecimal amount, UUID idempotencyKey) {
        return new CreateTransactionRequest(idempotencyKey, accountId, null, amount, TransactionType.DEPOSIT, null);
    }
}
