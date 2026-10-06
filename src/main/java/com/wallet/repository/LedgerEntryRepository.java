package com.wallet.repository;

import com.wallet.entity.LedgerEntry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface LedgerEntryRepository
        extends JpaRepository<LedgerEntry, UUID> {

    List<LedgerEntry> findByTransactionId(UUID transactionId);

    List<LedgerEntry> findByWalletId(Long walletId);


    @Query("""
        SELECT le
        FROM LedgerEntry le
        JOIN FETCH le.transaction t
        WHERE le.wallet.id = :walletId
        ORDER BY t.createdAt DESC
        """)
    Page<LedgerEntry> findByWalletIdWithTransaction(
            @Param("walletId") Long walletId,
            Pageable pageable
    );

    @Query("""
    SELECT le
    FROM LedgerEntry le
    JOIN FETCH le.transaction t
    WHERE le.wallet.id = :walletId
    ORDER BY t.createdAt ASC
    """)
    List<LedgerEntry> findAllByWalletIdWithTransaction(
            @Param("walletId") Long walletId
    );

    @Query("""
    SELECT CASE WHEN COUNT(le) > 0 THEN true ELSE false END
    FROM LedgerEntry le
    WHERE le.transaction.id = :transactionId
      AND le.wallet.user.id = :userId
    """)
    boolean existsByTransactionIdAndUserId(
            @Param("transactionId") UUID transactionId,
            @Param("userId") Long userId
    );
}
