package com.digitalbank.transaction.domain.repository;

import com.digitalbank.transaction.domain.model.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, UUID> {

    Optional<Transaction> findByIdempotencyKey(UUID idempotencyKey);

    @Query("""
        SELECT t FROM Transaction t
        WHERE t.sourceAccountId = :accountId
           OR t.destinationAccountId = :accountId
        ORDER BY t.createdAt DESC
        """)
    Page<Transaction> findAllByAccountId(UUID accountId, Pageable pageable);

    // Native SQL — NULL enum params need CAST(:x AS text) to avoid "could not determine data type" in PostgreSQL.
    @Query(value = """
        SELECT * FROM transactions t
        WHERE (t.source_account_id = :accountId OR t.destination_account_id = :accountId)
          AND t.status = 'COMPLETED'
          AND (CAST(:from AS timestamp) IS NULL OR t.updated_at >= :from)
          AND (CAST(:to   AS timestamp) IS NULL OR t.updated_at <= :to)
          AND (CAST(:type AS text)      IS NULL OR t.type = :type)
        ORDER BY t.updated_at DESC
        """,
        countQuery = """
        SELECT COUNT(*) FROM transactions t
        WHERE (t.source_account_id = :accountId OR t.destination_account_id = :accountId)
          AND t.status = 'COMPLETED'
          AND (CAST(:from AS timestamp) IS NULL OR t.updated_at >= :from)
          AND (CAST(:to   AS timestamp) IS NULL OR t.updated_at <= :to)
          AND (CAST(:type AS text)      IS NULL OR t.type = :type)
        """,
        nativeQuery = true)
    Page<Transaction> findStatement(
            UUID accountId,
            LocalDateTime from,
            LocalDateTime to,
            String type,
            Pageable pageable);

    // Sum of amounts credited to the account (deposits + incoming transfers)
    @Query(value = """
        SELECT COALESCE(SUM(t.amount), 0) FROM transactions t
        WHERE t.status = 'COMPLETED'
          AND (CAST(:from AS timestamp) IS NULL OR t.updated_at >= :from)
          AND (CAST(:to   AS timestamp) IS NULL OR t.updated_at <= :to)
          AND (CAST(:type AS text)      IS NULL OR t.type = :type)
          AND (
              (t.type = 'DEPOSIT'  AND t.source_account_id      = :accountId)
           OR (t.type = 'TRANSFER' AND t.destination_account_id = :accountId)
          )
        """, nativeQuery = true)
    BigDecimal sumCredits(UUID accountId, LocalDateTime from, LocalDateTime to, String type);

    // Sum of amounts debited from the account (withdrawals + outgoing transfers)
    @Query(value = """
        SELECT COALESCE(SUM(t.amount), 0) FROM transactions t
        WHERE t.status = 'COMPLETED'
          AND (CAST(:from AS timestamp) IS NULL OR t.updated_at >= :from)
          AND (CAST(:to   AS timestamp) IS NULL OR t.updated_at <= :to)
          AND (CAST(:type AS text)      IS NULL OR t.type = :type)
          AND (
              (t.type = 'WITHDRAWAL' AND t.source_account_id = :accountId)
           OR (t.type = 'TRANSFER'  AND t.source_account_id = :accountId)
          )
        """, nativeQuery = true)
    BigDecimal sumDebits(UUID accountId, LocalDateTime from, LocalDateTime to, String type);
}
