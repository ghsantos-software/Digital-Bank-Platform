package com.digitalbank.transaction.application.service;

import com.digitalbank.transaction.api.exception.BusinessException;
import com.digitalbank.transaction.api.exception.ResourceNotFoundException;
import com.digitalbank.transaction.application.dto.CreateTransactionRequest;
import com.digitalbank.transaction.application.dto.StatementResponse;
import com.digitalbank.transaction.application.dto.StatementTransactionRow;
import com.digitalbank.transaction.application.dto.TransactionResponse;
import com.digitalbank.transaction.application.mapper.TransactionMapper;
import com.digitalbank.transaction.domain.model.Transaction;
import com.digitalbank.transaction.domain.model.TransactionStatus;
import com.digitalbank.transaction.domain.model.TransactionType;
import com.digitalbank.transaction.domain.repository.TransactionRepository;
import com.digitalbank.transaction.infrastructure.client.AccountClient;
import com.digitalbank.transaction.infrastructure.client.dto.AccountSummary;
import com.digitalbank.transaction.infrastructure.messaging.TransactionCreatedEvent;
import com.digitalbank.transaction.infrastructure.messaging.TransactionEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final TransactionMapper transactionMapper;
    private final AccountClient accountClient;
    private final TransactionEventPublisher eventPublisher;

    @Transactional
    public TransactionResponse create(CreateTransactionRequest request) {
        UUID idempotencyKey = request.idempotencyKey() != null
                ? request.idempotencyKey()
                : UUID.randomUUID();

        var existing = transactionRepository.findByIdempotencyKey(idempotencyKey);
        if (existing.isPresent()) {
            log.info("Duplicate request — idempotencyKey: {} — returning existing transaction {}",
                    idempotencyKey, existing.get().getId());
            return transactionMapper.toResponse(existing.get());
        }

        if (request.type() == TransactionType.TRANSFER && request.destinationAccountId() == null) {
            throw new BusinessException("Destination account is required for TRANSFER");
        }
        if (request.type() == TransactionType.TRANSFER
                && request.sourceAccountId().equals(request.destinationAccountId())) {
            throw new BusinessException("Source and destination accounts must be different");
        }

        // Pre-check via Feign — authoritative check happens in account-service after Kafka processing
        AccountSummary source = fetchActiveAccount(request.sourceAccountId());

        if (request.type() == TransactionType.WITHDRAWAL || request.type() == TransactionType.TRANSFER) {
            if (source.balance().compareTo(request.amount()) < 0) {
                throw new BusinessException(
                        "Insufficient balance — available: R$ %s, requested: R$ %s"
                                .formatted(source.balance(), request.amount()));
            }
        }

        if (request.type() == TransactionType.TRANSFER) {
            fetchActiveAccount(request.destinationAccountId());
        }

        Transaction transaction = Transaction.builder()
                .idempotencyKey(idempotencyKey)
                .sourceAccountId(request.sourceAccountId())
                .destinationAccountId(request.destinationAccountId())
                .amount(request.amount())
                .type(request.type())
                .status(TransactionStatus.PENDING)
                .description(request.description())
                .performedBy(extractPerformedBy())
                .build();

        Transaction saved = transactionRepository.save(transaction);

        eventPublisher.publishTransactionCreated(new TransactionCreatedEvent(
                saved.getId(),
                saved.getSourceAccountId(),
                saved.getDestinationAccountId(),
                saved.getAmount(),
                saved.getType(),
                LocalDateTime.now()
        ));

        log.info("Transaction {} created as PENDING — type: {}, amount: R$ {}, performedBy: {}",
                saved.getId(), saved.getType(), saved.getAmount(), saved.getPerformedBy());

        return transactionMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public TransactionResponse findById(UUID id) {
        log.debug("Fetching transaction — id: {}", id);
        return transactionMapper.toResponse(findTransactionOrThrow(id));
    }

    @Transactional(readOnly = true)
    public Page<TransactionResponse> findByAccountId(UUID accountId, Pageable pageable) {
        log.debug("Listing transactions for accountId: {}", accountId);
        return transactionRepository.findAllByAccountId(accountId, pageable)
                .map(transactionMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public StatementResponse getStatement(
            UUID accountId,
            LocalDate from,
            LocalDate to,
            TransactionType type,
            Pageable pageable) {

        LocalDateTime fromDt = from != null ? from.atStartOfDay() : null;
        LocalDateTime toDt   = to   != null ? to.atTime(LocalTime.MAX) : null;

        String typeStr = type != null ? type.name() : null;
        // Strip sort from pageable — the native query has its own ORDER BY and Spring Data
        // would otherwise append the Java field name (updatedAt) instead of the column name.
        PageRequest unsorted = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize());
        Page<Transaction> page = transactionRepository.findStatement(accountId, fromDt, toDt, typeStr, unsorted);

        BigDecimal totalCredit = transactionRepository.sumCredits(accountId, fromDt, toDt, typeStr);
        BigDecimal totalDebit  = transactionRepository.sumDebits(accountId, fromDt, toDt, typeStr);

        List<StatementTransactionRow> rows = page.getContent().stream()
                .map(tx -> toStatementRow(tx, accountId))
                .toList();

        log.debug("Statement for accountId={}, from={}, to={}: {} transactions", accountId, from, to, page.getTotalElements());

        return new StatementResponse(
                accountId,
                from,
                to,
                totalCredit,
                totalDebit,
                rows,
                page.getNumber(),
                page.getTotalPages(),
                page.getTotalElements()
        );
    }

    @Transactional
    public void markCompleted(UUID transactionId) {
        Transaction tx = findTransactionOrThrow(transactionId);
        tx.setStatus(TransactionStatus.COMPLETED);
        transactionRepository.save(tx);
        log.info("Transaction {} marked as COMPLETED", transactionId);
    }

    @Transactional
    public void markFailed(UUID transactionId, String reason) {
        Transaction tx = findTransactionOrThrow(transactionId);
        tx.setStatus(TransactionStatus.FAILED);
        tx.setFailureReason(reason);
        transactionRepository.save(tx);
        log.warn("Transaction {} marked as FAILED — reason: {}", transactionId, reason);
    }

    // ─── private helpers ──────────────────────────────────────────────────────

    private StatementTransactionRow toStatementRow(Transaction tx, UUID accountId) {
        boolean isCredit = isCredit(tx, accountId);
        UUID counterpart = isCredit ? tx.getSourceAccountId() : tx.getDestinationAccountId();

        return new StatementTransactionRow(
                tx.getId(),
                tx.getType().getLabel(),
                isCredit ? "CREDIT" : "DEBIT",
                tx.getAmount(),
                counterpart,
                tx.getDescription(),
                tx.getStatus().getLabel(),
                tx.getUpdatedAt()
        );
    }

    private boolean isCredit(Transaction tx, UUID accountId) {
        return (tx.getType() == TransactionType.DEPOSIT && accountId.equals(tx.getSourceAccountId()))
                || (tx.getType() == TransactionType.TRANSFER && accountId.equals(tx.getDestinationAccountId()));
    }

    private AccountSummary fetchActiveAccount(UUID accountId) {
        AccountSummary account = accountClient.findById(accountId);
        if (!"ACTIVE".equals(account.status())) {
            throw new BusinessException("Account %s is not active — current status: %s"
                    .formatted(accountId, account.status()));
        }
        return account;
    }

    private String extractPerformedBy() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth instanceof JwtAuthenticationToken jwtAuth) {
            return jwtAuth.getToken().getSubject();
        }
        return null;
    }

    private Transaction findTransactionOrThrow(UUID id) {
        return transactionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction", id));
    }
}
