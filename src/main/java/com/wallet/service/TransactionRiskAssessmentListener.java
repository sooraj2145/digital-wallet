package com.wallet.service;

import com.wallet.entity.Transaction;
import com.wallet.event.TransactionCompletedEvent;
import com.wallet.repository.TransactionRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class TransactionRiskAssessmentListener {

    private final TransactionRepository transactionRepository;
    private final AnomalyDetectionService anomalyDetectionService;

    public TransactionRiskAssessmentListener(
            TransactionRepository transactionRepository,
            AnomalyDetectionService anomalyDetectionService
    ) {
        this.transactionRepository = transactionRepository;
        this.anomalyDetectionService = anomalyDetectionService;
    }

    @TransactionalEventListener(
            phase = TransactionPhase.AFTER_COMMIT
    )
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleTransactionCompleted(
            TransactionCompletedEvent event
    ) {

        Transaction transaction =
                transactionRepository
                        .findById(event.transactionId())
                        .orElseThrow(() ->
                                new IllegalStateException(
                                        "Transaction not found after commit"
                                )
                        );

        anomalyDetectionService.assessTransaction(
                transaction,
                event.walletId()
        );
    }
}