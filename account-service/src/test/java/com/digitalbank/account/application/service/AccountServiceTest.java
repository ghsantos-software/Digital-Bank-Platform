package com.digitalbank.account.application.service;

import com.digitalbank.account.api.exception.BusinessException;
import com.digitalbank.account.api.exception.ResourceNotFoundException;
import com.digitalbank.account.application.dto.BalanceResponse;
import com.digitalbank.account.application.mapper.AccountMapper;
import com.digitalbank.account.domain.model.Account;
import com.digitalbank.account.domain.model.AccountStatus;
import com.digitalbank.account.domain.model.AccountType;
import com.digitalbank.account.domain.repository.AccountRepository;
import com.digitalbank.account.infrastructure.generator.AccountNumberGenerator;
import com.digitalbank.account.infrastructure.messaging.TransactionCreatedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock AccountRepository accountRepository;
    @Mock AccountMapper accountMapper;
    @Mock AccountNumberGenerator accountNumberGenerator;
    @InjectMocks AccountService accountService;

    private Account checkingAccount;
    private Account savingsAccount;

    @BeforeEach
    void setUp() {
        checkingAccount = Account.builder()
                .id(UUID.randomUUID())
                .userId(UUID.randomUUID())
                .accountNumber("10000001")
                .branch("0001")
                .type(AccountType.CHECKING)
                .balance(new BigDecimal("1000.00"))
                .status(AccountStatus.ACTIVE)
                .build();

        savingsAccount = Account.builder()
                .id(UUID.randomUUID())
                .userId(UUID.randomUUID())
                .accountNumber("10000002")
                .branch("0001")
                .type(AccountType.SAVINGS)
                .balance(new BigDecimal("500.00"))
                .status(AccountStatus.ACTIVE)
                .build();
    }

    // ─── getBalance ──────────────────────────────────────────────────────────

    @Test
    void getBalance_returnsSnapshot() {
        when(accountRepository.findById(checkingAccount.getId())).thenReturn(Optional.of(checkingAccount));

        BalanceResponse balance = accountService.getBalance(checkingAccount.getId());

        assertThat(balance.accountId()).isEqualTo(checkingAccount.getId());
        assertThat(balance.balance()).isEqualByComparingTo("1000.00");
        assertThat(balance.status()).isEqualTo(AccountStatus.ACTIVE);
        assertThat(balance.checkedAt()).isNotNull();
    }

    // ─── processTransaction — DEPOSIT ────────────────────────────────────────

    @Test
    void processDeposit_creditsSourceAccount() {
        var event = depositEvent(checkingAccount.getId(), new BigDecimal("250.00"));
        when(accountRepository.findById(checkingAccount.getId())).thenReturn(Optional.of(checkingAccount));

        accountService.processTransaction(event);

        assertThat(checkingAccount.getBalance()).isEqualByComparingTo("1250.00");
        verify(accountRepository).save(checkingAccount);
    }

    // ─── processTransaction — WITHDRAWAL ─────────────────────────────────────

    @Test
    void processWithdrawal_debitsSourceAccount() {
        var event = withdrawalEvent(checkingAccount.getId(), new BigDecimal("300.00"));
        when(accountRepository.findById(checkingAccount.getId())).thenReturn(Optional.of(checkingAccount));

        accountService.processTransaction(event);

        assertThat(checkingAccount.getBalance()).isEqualByComparingTo("700.00");
        verify(accountRepository).save(checkingAccount);
    }

    @Test
    void processWithdrawal_insufficientBalance_throws() {
        var event = withdrawalEvent(checkingAccount.getId(), new BigDecimal("1500.00"));
        when(accountRepository.findById(checkingAccount.getId())).thenReturn(Optional.of(checkingAccount));

        assertThatThrownBy(() -> accountService.processTransaction(event))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Insufficient balance")
                .hasMessageContaining("10000001")
                .hasMessageContaining("1000.00")
                .hasMessageContaining("1500.00");

        // Balance must not be modified
        assertThat(checkingAccount.getBalance()).isEqualByComparingTo("1000.00");
    }

    @Test
    void processWithdrawal_exactBalance_succeeds() {
        var event = withdrawalEvent(checkingAccount.getId(), new BigDecimal("1000.00"));
        when(accountRepository.findById(checkingAccount.getId())).thenReturn(Optional.of(checkingAccount));

        accountService.processTransaction(event);

        assertThat(checkingAccount.getBalance()).isEqualByComparingTo("0.00");
    }

    @Test
    void processWithdrawal_blockedAccount_throws() {
        checkingAccount.setStatus(AccountStatus.BLOCKED);
        var event = withdrawalEvent(checkingAccount.getId(), new BigDecimal("100.00"));
        when(accountRepository.findById(checkingAccount.getId())).thenReturn(Optional.of(checkingAccount));

        assertThatThrownBy(() -> accountService.processTransaction(event))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("is not active")
                .hasMessageContaining("BLOCKED");
    }

    // ─── processTransaction — TRANSFER ───────────────────────────────────────

    @Test
    void processTransfer_debitsSourceAndCreditsDestination() {
        var event = transferEvent(checkingAccount.getId(), savingsAccount.getId(), new BigDecimal("400.00"));
        when(accountRepository.findById(checkingAccount.getId())).thenReturn(Optional.of(checkingAccount));
        when(accountRepository.findById(savingsAccount.getId())).thenReturn(Optional.of(savingsAccount));

        accountService.processTransaction(event);

        assertThat(checkingAccount.getBalance()).isEqualByComparingTo("600.00");
        assertThat(savingsAccount.getBalance()).isEqualByComparingTo("900.00");
    }

    @Test
    void processTransfer_sourceInsufficientBalance_rollsBack() {
        var event = transferEvent(checkingAccount.getId(), savingsAccount.getId(), new BigDecimal("2000.00"));
        when(accountRepository.findById(checkingAccount.getId())).thenReturn(Optional.of(checkingAccount));

        assertThatThrownBy(() -> accountService.processTransaction(event))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Insufficient balance");

        // Savings account must never be touched
        verify(accountRepository, never()).findById(savingsAccount.getId());
        assertThat(checkingAccount.getBalance()).isEqualByComparingTo("1000.00");
    }

    // ─── block ───────────────────────────────────────────────────────────────

    @Test
    void block_activeAccount_setsBlocked() {
        when(accountRepository.findById(checkingAccount.getId())).thenReturn(Optional.of(checkingAccount));

        accountService.block(checkingAccount.getId());

        assertThat(checkingAccount.getStatus()).isEqualTo(AccountStatus.BLOCKED);
        verify(accountRepository).save(checkingAccount);
    }

    @Test
    void block_alreadyBlocked_throws() {
        checkingAccount.setStatus(AccountStatus.BLOCKED);
        when(accountRepository.findById(checkingAccount.getId())).thenReturn(Optional.of(checkingAccount));

        assertThatThrownBy(() -> accountService.block(checkingAccount.getId()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("already blocked");
    }

    @Test
    void block_closedAccount_throws() {
        checkingAccount.setStatus(AccountStatus.CLOSED);
        when(accountRepository.findById(checkingAccount.getId())).thenReturn(Optional.of(checkingAccount));

        assertThatThrownBy(() -> accountService.block(checkingAccount.getId()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("closed");
    }

    @Test
    void findById_notFound_throws() {
        UUID id = UUID.randomUUID();
        when(accountRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> accountService.findById(id))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ─── helpers ─────────────────────────────────────────────────────────────

    private TransactionCreatedEvent depositEvent(UUID accountId, BigDecimal amount) {
        return new TransactionCreatedEvent(UUID.randomUUID(), accountId, null, amount, "DEPOSIT", LocalDateTime.now());
    }

    private TransactionCreatedEvent withdrawalEvent(UUID accountId, BigDecimal amount) {
        return new TransactionCreatedEvent(UUID.randomUUID(), accountId, null, amount, "WITHDRAWAL", LocalDateTime.now());
    }

    private TransactionCreatedEvent transferEvent(UUID source, UUID destination, BigDecimal amount) {
        return new TransactionCreatedEvent(UUID.randomUUID(), source, destination, amount, "TRANSFER", LocalDateTime.now());
    }
}
