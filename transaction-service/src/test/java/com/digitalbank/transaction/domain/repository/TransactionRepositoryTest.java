package com.digitalbank.transaction.domain.repository;

import com.digitalbank.transaction.domain.model.Transaction;
import com.digitalbank.transaction.domain.model.TransactionStatus;
import com.digitalbank.transaction.domain.model.TransactionType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class TransactionRepositoryTest {

    @Autowired
    TransactionRepository repository;

    private UUID accountA;
    private UUID accountB;

    @BeforeEach
    void insertFixtures() {
        accountA = UUID.randomUUID();
        accountB = UUID.randomUUID();

        repository.save(completedTransaction(accountA, null, new BigDecimal("1000.00"), TransactionType.DEPOSIT));
        repository.save(completedTransaction(accountA, null, new BigDecimal("200.00"), TransactionType.WITHDRAWAL));
        repository.save(completedTransaction(accountA, accountB, new BigDecimal("300.00"), TransactionType.TRANSFER));

        // PENDING — must not appear in statement
        repository.save(Transaction.builder()
                .idempotencyKey(UUID.randomUUID())
                .sourceAccountId(accountA)
                .amount(new BigDecimal("50.00"))
                .type(TransactionType.WITHDRAWAL)
                .status(TransactionStatus.PENDING)
                .build());
    }

    @Test
    void findStatement_returnsOnlyCompletedTransactions() {
        Page<Transaction> page = repository.findStatement(accountA, null, null, null, PageRequest.of(0, 10));

        assertThat(page.getTotalElements()).isEqualTo(3);
        assertThat(page.getContent()).allMatch(tx -> tx.getStatus() == TransactionStatus.COMPLETED);
    }

    @Test
    void findStatement_filterByType_returnsOnlyDeposits() {
        Page<Transaction> page = repository.findStatement(accountA, null, null, "DEPOSIT", PageRequest.of(0, 10));

        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent().get(0).getType()).isEqualTo(TransactionType.DEPOSIT);
    }

    @Test
    void findStatement_filterByDateRange_excludesOutsideRange() {
        LocalDateTime future = LocalDateTime.now().plusMinutes(1);

        Page<Transaction> page = repository.findStatement(accountA, future, null, null, PageRequest.of(0, 10));

        assertThat(page.getTotalElements()).isZero();
    }

    @Test
    void findStatement_destinationAccount_includesIncomingTransfers() {
        Page<Transaction> page = repository.findStatement(accountB, null, null, null, PageRequest.of(0, 10));

        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent().get(0).getType()).isEqualTo(TransactionType.TRANSFER);
    }

    @Test
    void sumCredits_calculatesDepositsAndIncomingTransfers() {
        BigDecimal credits = repository.sumCredits(accountA, null, null, null);
        assertThat(credits).isEqualByComparingTo("1000.00");
    }

    @Test
    void sumDebits_calculatesWithdrawalsAndOutgoingTransfers() {
        BigDecimal debits = repository.sumDebits(accountA, null, null, null);
        assertThat(debits).isEqualByComparingTo("500.00");
    }

    @Test
    void findByIdempotencyKey_returnsExisting() {
        UUID key = UUID.randomUUID();
        repository.save(Transaction.builder()
                .idempotencyKey(key)
                .sourceAccountId(accountA)
                .amount(new BigDecimal("100.00"))
                .type(TransactionType.DEPOSIT)
                .status(TransactionStatus.PENDING)
                .build());

        assertThat(repository.findByIdempotencyKey(key)).isPresent();
        assertThat(repository.findByIdempotencyKey(UUID.randomUUID())).isEmpty();
    }

    private Transaction completedTransaction(UUID source, UUID destination, BigDecimal amount, TransactionType type) {
        return Transaction.builder()
                .idempotencyKey(UUID.randomUUID())
                .sourceAccountId(source)
                .destinationAccountId(destination)
                .amount(amount)
                .type(type)
                .status(TransactionStatus.COMPLETED)
                .build();
    }
}
