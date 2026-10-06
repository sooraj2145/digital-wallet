package com.wallet.service;

import com.wallet.config.AnomalyDetectionProperties;
import com.wallet.entity.RiskLevel;
import com.wallet.entity.Transaction;
import com.wallet.entity.TransactionRiskAssessment;
import com.wallet.repository.TransactionRepository;
import com.wallet.repository.TransactionRiskAssessmentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
public class AnomalyDetectionService {

    private final AnomalyDetectionProperties properties;
    private final TransactionRiskAssessmentRepository assessmentRepository;
    private final TransactionRepository transactionRepository;

    public AnomalyDetectionService(
            AnomalyDetectionProperties properties,
            TransactionRiskAssessmentRepository assessmentRepository,
            TransactionRepository transactionRepository
    ) {
        this.properties = properties;
        this.assessmentRepository = assessmentRepository;
        this.transactionRepository = transactionRepository;
    }

    @Transactional
    public TransactionRiskAssessment assessTransaction(
            Transaction transaction,
            Long walletId
    ) {

        int riskScore = 0;

        StringBuilder reason = new StringBuilder();

        BigDecimal amount = transaction.getAmount();

        /*
         * Rule 1: Transaction amount
         */
        if (amount.compareTo(
                properties.getLargeAmountThreshold()
        ) >= 0) {

            riskScore += 40;

            reason.append(
                    "Transaction amount is unusually large"
            );

        } else if (amount.compareTo(
                properties.getHighAmountThreshold()
        ) >= 0) {

            riskScore += 20;

            reason.append(
                    "Transaction amount is relatively high"
            );
        }

        /*
         * Rule 2: Transaction frequency
         */
        LocalDateTime since =
                transaction.getCreatedAt()
                        .minusMinutes(
                                properties.getFrequencyWindowMinutes()
                        );

        long recentTransactionCount =
                transactionRepository.countRecentTransactions(
                        walletId,
                        since
                );

        if (recentTransactionCount >=
                properties.getFrequencyThreshold()) {

            riskScore += 30;

            if (!reason.isEmpty()) {
                reason.append("; ");
            }

            reason.append(
                    "High transaction frequency detected"
            );
        }

        /*
         * Keep score within 0–100.
         */
        riskScore = Math.min(riskScore, 100);

        RiskLevel riskLevel =
                determineRiskLevel(riskScore);

        /*
         * No anomaly detected.
         */
        if (reason.isEmpty()) {
            reason.append(
                    "No significant anomaly detected"
            );
        }

        TransactionRiskAssessment assessment =
                new TransactionRiskAssessment(
                        transaction,
                        riskScore,
                        riskLevel,
                        reason.toString()
                );

        return assessmentRepository.save(assessment);
    }

    private RiskLevel determineRiskLevel(int riskScore) {

        if (riskScore >= 60) {
            return RiskLevel.HIGH;
        }

        if (riskScore >= 30) {
            return RiskLevel.MEDIUM;
        }

        return RiskLevel.LOW;
    }
}