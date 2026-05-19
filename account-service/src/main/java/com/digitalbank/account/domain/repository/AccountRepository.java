package com.digitalbank.account.domain.repository;

import com.digitalbank.account.domain.model.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AccountRepository extends JpaRepository<Account, UUID> {

    List<Account> findAllByUserId(UUID userId);

    boolean existsByAccountNumber(String accountNumber);

    // Fetches the next value from the DB sequence — guarantees uniqueness without retries
    @Query(value = "SELECT nextval('account_number_seq')", nativeQuery = true)
    Long nextAccountNumberSeq();
}
