package com.wallet.repository;


import com.wallet.entity.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface TransactionRepository  extends
        JpaRepository<Transaction, UUID> {

    Optional<Transaction> findByIdempotencyKey(String idempotencyKey);

    @Query("""
        SELECT DISTINCT t
        FROM Transaction t
        JOIN LedgerEntry le ON le.transaction = t
        WHERE le.wallet.id = :walletId
        """)
    Page<Transaction> findByWalletId(
            @Param("walletId") Long walletId,
            Pageable pageable
    );

    @Query("""
    SELECT DISTINCT t
    FROM Transaction t
    JOIN LedgerEntry le ON le.transaction = t
    JOIN Wallet w ON le.wallet = w
    WHERE w.user.id = :userId
    """)
    Page<Transaction> findByUserId(
            @Param("userId") Long userId,
            Pageable pageable
    );
}
