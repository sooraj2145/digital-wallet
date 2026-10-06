package com.wallet.service;

import com.wallet.config.AnomalyDetectionProperties;
import com.wallet.entity.Currency;
import com.wallet.entity.RiskLevel;
import com.wallet.entity.Transaction;
import com.wallet.entity.TransactionRiskAssessment;
import com.wallet.entity.TransactionType;
import com.wallet.repository.TransactionRepository;
import com.wallet.repository.TransactionRiskAssessmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import static org.mockito.ArgumentMatchers.eq;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

class AnomalyDetectionServiceTest {

    private TransactionRiskAssessmentRepository assessmentRepository;
    private TransactionRepository transactionRepository;
    private AnomalyDetectionProperties properties;
    private AnomalyDetectionService anomalyDetectionService;

    @BeforeEach
    void setUp() {

        assessmentRepository =
                mock(TransactionRiskAssessmentRepository.class);

        transactionRepository =
                mock(TransactionRepository.class);

        properties =
                new AnomalyDetectionProperties();

        anomalyDetectionService =
                new AnomalyDetectionService(
                        properties,
                        assessmentRepository,
                        transactionRepository
                );

        when(assessmentRepository.save(
                any(TransactionRiskAssessment.class)
        )).thenAnswer(invocation ->
                invocation.getArgument(0)
        );
    }

    @Test
    void normalTransactionShouldHaveLowRisk() {

        Transaction transaction = createTransaction(
                "5000.00"
        );

        TransactionRiskAssessment assessment =
                anomalyDetectionService.assessTransaction(
                        transaction,
                        1L
                );

        assertEquals(
                0,
                assessment.getRiskScore()
        );

        assertEquals(
                RiskLevel.LOW,
                assessment.getRiskLevel()
        );

        assertEquals(
                "No significant anomaly detected",
                assessment.getReason()
        );
    }

    @Test
    void highAmountTransactionShouldAddTwentyRiskPoints() {

        Transaction transaction = createTransaction(
                "10000.00"
        );

        TransactionRiskAssessment assessment =
                anomalyDetectionService.assessTransaction(
                        transaction,
                        1L
                );

        assertEquals(
                20,
                assessment.getRiskScore()
        );

        assertEquals(
                RiskLevel.LOW,
                assessment.getRiskLevel()
        );

        assertTrue(
                assessment.getReason()
                        .contains("relatively high")
        );
    }

    @Test
    void largeTransactionShouldAddFortyRiskPoints() {

        Transaction transaction = createTransaction(
                "50000.00"
        );

        TransactionRiskAssessment assessment =
                anomalyDetectionService.assessTransaction(
                        transaction,
                        1L
                );

        assertEquals(
                40,
                assessment.getRiskScore()
        );

        assertEquals(
                RiskLevel.MEDIUM,
                assessment.getRiskLevel()
        );

        assertTrue(
                assessment.getReason()
                        .contains("unusually large")
        );
    }

    @Test
    void assessmentShouldBeSaved() {

        Transaction transaction = createTransaction(
                "50000.00"
        );

        anomalyDetectionService.assessTransaction(
                transaction,
                1L
        );

        verify(assessmentRepository)
                .save(any(TransactionRiskAssessment.class));
    }

    private Transaction createTransaction(String amount) {

        Transaction transaction = new Transaction(
                TransactionType.TRANSFER,
                new BigDecimal(amount),
                Currency.INR,
                "test-key-" + System.nanoTime(),
                "test-fingerprint"
        );

        ReflectionTestUtils.setField(
                transaction,
                "createdAt",
                LocalDateTime.now()
        );

        return transaction;
    }


    @Test
    void highTransactionFrequencyShouldAddThirtyRiskPoints() {

        Transaction transaction = createTransaction(
                "5000.00"
        );

        when(transactionRepository.countRecentTransactions(
                eq(1L),
                any(LocalDateTime.class)
        )).thenReturn(5L);

        TransactionRiskAssessment assessment =
                anomalyDetectionService.assessTransaction(
                        transaction,
                        1L
                );

        assertEquals(
                30,
                assessment.getRiskScore()
        );

        assertEquals(
                RiskLevel.MEDIUM,
                assessment.getRiskLevel()
        );

        assertTrue(
                assessment.getReason()
                        .contains("High transaction frequency detected")
        );
    }

    @Test
    void largeAmountAndHighFrequencyShouldProduceHighRisk() {

        Transaction transaction = createTransaction(
                "50000.00"
        );

        when(transactionRepository.countRecentTransactions(
                eq(1L),
                any(LocalDateTime.class)
        )).thenReturn(5L);

        TransactionRiskAssessment assessment =
                anomalyDetectionService.assessTransaction(
                        transaction,
                        1L
                );

        assertEquals(
                70,
                assessment.getRiskScore()
        );

        assertEquals(
                RiskLevel.HIGH,
                assessment.getRiskLevel()
        );

        assertTrue(
                assessment.getReason()
                        .contains("unusually large")
        );

        assertTrue(
                assessment.getReason()
                        .contains("High transaction frequency detected")
        );
    }


    @Test
    void transactionFrequencyBelowThresholdShouldNotAddRiskPoints() {

        Transaction transaction = createTransaction(
                "5000.00"
        );

        when(transactionRepository.countRecentTransactions(
                eq(1L),
                any(LocalDateTime.class)
        )).thenReturn(4L);

        TransactionRiskAssessment assessment =
                anomalyDetectionService.assessTransaction(
                        transaction,
                        1L
                );

        assertEquals(
                0,
                assessment.getRiskScore()
        );

        assertEquals(
                RiskLevel.LOW,
                assessment.getRiskLevel()
        );

        assertEquals(
                "No significant anomaly detected",
                assessment.getReason()
        );
    }
}