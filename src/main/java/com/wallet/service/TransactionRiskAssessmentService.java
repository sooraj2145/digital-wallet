package com.wallet.service;

import com.wallet.dto.TransactionRiskAssessmentResponse;
import com.wallet.entity.TransactionRiskAssessment;
import com.wallet.exception.WalletNotFoundException;
import com.wallet.repository.LedgerEntryRepository;
import com.wallet.repository.TransactionRiskAssessmentRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class TransactionRiskAssessmentService {

    private final TransactionRiskAssessmentRepository assessmentRepository;
    private final LedgerEntryRepository ledgerEntryRepository;
    private final AuthenticationService authenticationService;

    public TransactionRiskAssessmentService(
            TransactionRiskAssessmentRepository assessmentRepository,
            LedgerEntryRepository ledgerEntryRepository,
            AuthenticationService authenticationService
    ) {
        this.assessmentRepository = assessmentRepository;
        this.ledgerEntryRepository = ledgerEntryRepository;
        this.authenticationService = authenticationService;
    }

    public TransactionRiskAssessmentResponse getAssessment(
            UUID transactionId,
            Authentication authentication
    ) {

        Long currentUserId =
                authenticationService.getCurrentUserId(authentication);

        TransactionRiskAssessment assessment =
                assessmentRepository
                        .findByTransactionIdWithTransaction(transactionId)
                        .orElseThrow(() ->
                                new WalletNotFoundException(
                                        "Transaction not found"
                                ));

        boolean ownsTransaction =
                ledgerEntryRepository.existsByTransactionIdAndUserId(
                        transactionId,
                        currentUserId
                );

        if (!ownsTransaction) {
            throw new WalletNotFoundException(
                    "Transaction not found"
            );
        }

        return new TransactionRiskAssessmentResponse(
                assessment.getTransaction().getId(),
                assessment.getRiskScore(),
                assessment.getRiskLevel(),
                assessment.getReason(),
                assessment.getAssessedAt()
        );
    }
}