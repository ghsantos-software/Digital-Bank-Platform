package com.digitalbank.account.infrastructure.generator;

import com.digitalbank.account.domain.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AccountNumberGenerator {

    private final AccountRepository accountRepository;

    // Delegates to a PostgreSQL sequence — always unique, no collision risk
    public String generate() {
        Long nextVal = accountRepository.nextAccountNumberSeq();
        return String.valueOf(nextVal);
    }
}
