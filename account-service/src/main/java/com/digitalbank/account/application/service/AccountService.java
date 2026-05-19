package com.digitalbank.account.application.service;

import com.digitalbank.account.api.exception.BusinessException;
import com.digitalbank.account.api.exception.ResourceNotFoundException;
import com.digitalbank.account.application.dto.AccountResponse;
import com.digitalbank.account.application.dto.BalanceResponse;
import com.digitalbank.account.application.dto.CreateAccountRequest;
import com.digitalbank.account.application.mapper.AccountMapper;
import com.digitalbank.account.domain.model.Account;
import com.digitalbank.account.domain.model.AccountStatus;
import com.digitalbank.account.domain.model.AccountType;
import com.digitalbank.account.domain.repository.AccountRepository;
import com.digitalbank.account.infrastructure.generator.AccountNumberGenerator;
import com.digitalbank.account.infrastructure.messaging.TransactionCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;
    private final AccountMapper accountMapper;
    private final AccountNumberGenerator accountNumberGenerator;

    @Transactional
    public AccountResponse create(CreateAccountRequest request) {
        log.info("Opening {} account for userId: {}", request.type(), request.userId());

        String branch = (request.branch() != null && !request.branch().isBlank())
                ? request.branch()
                : "0001";

        Account account = Account.builder()
                .userId(request.userId())
                .accountNumber(accountNumberGenerator.generate())
                .branch(branch)
                .type(request.type())
                .balance(BigDecimal.ZERO)
                .status(AccountStatus.ACTIVE)
                .build();

        Account saved = accountRepository.save(account);
        log.info("Account created — id: {}, number: {}", saved.getId(), saved.getAccountNumber());
        return accountMapper.toResponse(saved);
    }

    @Transactional
    public AccountResponse createDefault(UUID userId) {
        log.info("Creating default CHECKING account for userId: {}", userId);

        Account account = Account.builder()
                .userId(userId)
                .accountNumber(accountNumberGenerator.generate())
                .branch("0001")
                .type(AccountType.CHECKING)
                .balance(BigDecimal.ZERO)
                .status(AccountStatus.ACTIVE)
                .build();

        Account saved = accountRepository.save(account);
        log.info("Default account created — id: {}, number: {}", saved.getId(), saved.getAccountNumber());
        return accountMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public BalanceResponse getBalance(UUID id) {
        Account account = findAccountOrThrow(id);
        return new BalanceResponse(
                account.getId(),
                account.getAccountNumber(),
                account.getBranch(),
                account.getType(),
                account.getBalance(),
                account.getStatus(),
                LocalDateTime.now()
        );
    }

    @Transactional(readOnly = true)
    public AccountResponse findById(UUID id) {
        log.debug("Fetching account — id: {}", id);
        return accountMapper.toResponse(findAccountOrThrow(id));
    }

    @Transactional(readOnly = true)
    public List<AccountResponse> findByUserId(UUID userId) {
        log.debug("Listing accounts for userId: {}", userId);
        return accountRepository.findAllByUserId(userId)
                .stream()
                .map(accountMapper::toResponse)
                .toList();
    }

    @Transactional
    public void block(UUID id) {
        log.info("Blocking account — id: {}", id);
        Account account = findAccountOrThrow(id);

        if (account.getStatus() == AccountStatus.BLOCKED) {
            throw new BusinessException("Account is already blocked");
        }
        if (account.getStatus() == AccountStatus.CLOSED) {
            throw new BusinessException("Cannot block a closed account");
        }

        account.setStatus(AccountStatus.BLOCKED);
        accountRepository.save(account);
        log.info("Account blocked — id: {}", id);
    }

    @Transactional
    public void processTransaction(TransactionCreatedEvent event) {
        log.info("Applying balance for transactionId: {}, type: {}, amount: R$ {}",
                event.transactionId(), event.type(), event.amount());

        switch (event.type()) {
            case "DEPOSIT"    -> creditAccount(event.sourceAccountId(), event.amount());
            case "WITHDRAWAL" -> debitAccount(event.sourceAccountId(), event.amount());
            case "TRANSFER"   -> {
                debitAccount(event.sourceAccountId(), event.amount());
                creditAccount(event.destinationAccountId(), event.amount());
            }
            default -> throw new BusinessException("Unknown transaction type: " + event.type());
        }
    }

    private void debitAccount(UUID accountId, BigDecimal amount) {
        Account account = findAccountOrThrow(accountId);
        requireActive(account);

        if (account.getBalance().compareTo(amount) < 0) {
            throw new BusinessException(
                    "Saldo insuficiente na conta %s — disponível: R$ %s, solicitado: R$ %s"
                            .formatted(account.getAccountNumber(), account.getBalance(), amount));
        }

        account.setBalance(account.getBalance().subtract(amount));
        accountRepository.save(account);
        log.debug("Debited R$ {} from account {}", amount, accountId);
    }

    private void creditAccount(UUID accountId, BigDecimal amount) {
        Account account = findAccountOrThrow(accountId);
        requireActive(account);

        account.setBalance(account.getBalance().add(amount));
        accountRepository.save(account);
        log.debug("Credited R$ {} to account {}", amount, accountId);
    }

    private void requireActive(Account account) {
        if (account.getStatus() != AccountStatus.ACTIVE) {
            throw new BusinessException(
                    "A conta %s não está ativa — status atual: %s"
                            .formatted(account.getAccountNumber(), account.getStatus()));
        }
    }

    private Account findAccountOrThrow(UUID id) {
        return accountRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Account", id));
    }
}
