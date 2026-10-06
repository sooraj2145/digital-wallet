package com.wallet.repository;

import com.wallet.entity.TransactionRiskAssessment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface TransactionRiskAssessmentRepository
    extends JpaRepository<TransactionRiskAssessment, UUID> {

    Optional<TransactionRiskAssessment> findByTransactionId(
            UUID transactionId
    );

    @Query("""
    SELECT a
    FROM TransactionRiskAssessment a
    JOIN FETCH a.transaction t
    WHERE a.transaction.id = :transactionId
    """)
    Optional<TransactionRiskAssessment> findByTransactionIdWithTransaction(
            @Param("transactionId") UUID transactionId
    );
}
