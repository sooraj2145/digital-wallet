package com.wallet.service;

import com.wallet.dto.TransactionRiskAssessmentResponse;

import com.wallet.entity.Transaction;
import com.wallet.entity.TransactionRiskAssessment;

import com.wallet.entity.Currency;
import com.wallet.entity.RiskLevel;
import com.wallet.entity.TransactionType;
import com.wallet.exception.WalletNotFoundException;
import com.wallet.repository.LedgerEntryRepository;
import com.wallet.repository.TransactionRiskAssessmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.core.Authentication;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TransactionRiskAssessmentServiceTest {

    @Mock
    private TransactionRiskAssessmentRepository assessmentRepository;

    @Mock
    private LedgerEntryRepository ledgerEntryRepository;

    @Mock
    private AuthenticationService authenticationService;

    @Mock
    private Authentication authentication;

    private TransactionRiskAssessmentService assessmentService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        assessmentService = new TransactionRiskAssessmentService(
                assessmentRepository,
                ledgerEntryRepository,
                authenticationService
        );
    }

    @Test
    void shouldReturnRiskAssessmentWhenUserOwnsTransaction() {

        Long userId = 1L;
        UUID transactionId = UUID.randomUUID();

        LocalDateTime createdAt =
                LocalDateTime.of(2026, 10, 6, 12, 0);

        Transaction transaction = new Transaction(
                TransactionType.TRANSFER,
                new BigDecimal("1000.00"),
                Currency.INR,
                "test-key",
                "test-fingerprint"
        );

        setTransactionId(transaction, transactionId);
        setTransactionCreatedAt(transaction, createdAt);

        TransactionRiskAssessment assessment =
                new TransactionRiskAssessment(
                        transaction,
                        0,
                        RiskLevel.LOW,
                        "No significant anomaly detected"
                );

        setAssessmentTimestamp(
                assessment,
                createdAt.plusSeconds(1)
        );

        when(authenticationService.getCurrentUserId(authentication))
                .thenReturn(userId);

        when(
                assessmentRepository
                        .findByTransactionIdWithTransaction(transactionId)
        ).thenReturn(Optional.of(assessment));

        when(
                ledgerEntryRepository.existsByTransactionIdAndUserId(
                        transactionId,
                        userId
                )
        ).thenReturn(true);

        TransactionRiskAssessmentResponse response =
                assessmentService.getAssessment(
                        transactionId,
                        authentication
                );

        assertNotNull(response);

        assertEquals(
                transactionId,
                response.transactionId()
        );

        assertEquals(
                0,
                response.riskScore()
        );

        assertEquals(
                RiskLevel.LOW,
                response.riskLevel()
        );

        assertEquals(
                "No significant anomaly detected",
                response.reason()
        );

        assertEquals(
                createdAt.plusSeconds(1),
                response.assessedAt()
        );

        verify(
                ledgerEntryRepository
        ).existsByTransactionIdAndUserId(
                transactionId,
                userId
        );
    }

    @Test
    void shouldRejectWhenUserDoesNotOwnTransaction() {

        Long currentUserId = 1L;
        UUID transactionId = UUID.randomUUID();

        Transaction transaction = new Transaction(
                TransactionType.TRANSFER,
                new BigDecimal("1000.00"),
                Currency.INR,
                "test-key",
                "test-fingerprint"
        );

        setTransactionId(transaction, transactionId);
        setTransactionCreatedAt(
                transaction,
                LocalDateTime.now()
        );

        TransactionRiskAssessment assessment =
                new TransactionRiskAssessment(
                        transaction,
                        0,
                        RiskLevel.LOW,
                        "No significant anomaly detected"
                );

        when(
                authenticationService.getCurrentUserId(authentication)
        ).thenReturn(currentUserId);

        when(
                assessmentRepository
                        .findByTransactionIdWithTransaction(transactionId)
        ).thenReturn(Optional.of(assessment));

        when(
                ledgerEntryRepository.existsByTransactionIdAndUserId(
                        transactionId,
                        currentUserId
                )
        ).thenReturn(false);

        assertThrows(
                WalletNotFoundException.class,
                () ->
                        assessmentService.getAssessment(
                                transactionId,
                                authentication
                        )
        );

        verify(
                ledgerEntryRepository
        ).existsByTransactionIdAndUserId(
                transactionId,
                currentUserId
        );
    }

    @Test
    void shouldRejectWhenAssessmentDoesNotExist() {

        Long userId = 1L;
        UUID transactionId = UUID.randomUUID();

        when(
                authenticationService.getCurrentUserId(authentication)
        ).thenReturn(userId);

        when(
                assessmentRepository
                        .findByTransactionIdWithTransaction(transactionId)
        ).thenReturn(Optional.empty());

        assertThrows(
                WalletNotFoundException.class,
                () ->
                        assessmentService.getAssessment(
                                transactionId,
                                authentication
                        )
        );

        verify(
                ledgerEntryRepository,
                never()
        ).existsByTransactionIdAndUserId(
                any(UUID.class),
                anyLong()
        );
    }

    @Test
    void shouldReturnHighRiskAssessmentCorrectly() {

        Long userId = 1L;
        UUID transactionId = UUID.randomUUID();

        LocalDateTime assessedAt =
                LocalDateTime.of(2026, 10, 6, 13, 0);

        Transaction transaction = new Transaction(
                TransactionType.TRANSFER,
                new BigDecimal("60000.00"),
                Currency.INR,
                "high-risk-key",
                "high-risk-fingerprint"
        );

        setTransactionId(transaction, transactionId);

        TransactionRiskAssessment assessment =
                new TransactionRiskAssessment(
                        transaction,
                        70,
                        RiskLevel.HIGH,
                        "Transaction amount is unusually large; " +
                                "High transaction frequency detected"
                );

        setAssessmentTimestamp(
                assessment,
                assessedAt
        );

        when(
                authenticationService.getCurrentUserId(authentication)
        ).thenReturn(userId);

        when(
                assessmentRepository
                        .findByTransactionIdWithTransaction(transactionId)
        ).thenReturn(Optional.of(assessment));

        when(
                ledgerEntryRepository.existsByTransactionIdAndUserId(
                        transactionId,
                        userId
                )
        ).thenReturn(true);

        TransactionRiskAssessmentResponse response =
                assessmentService.getAssessment(
                        transactionId,
                        authentication
                );

        assertEquals(
                transactionId,
                response.transactionId()
        );

        assertEquals(
                70,
                response.riskScore()
        );

        assertEquals(
                RiskLevel.HIGH,
                response.riskLevel()
        );

        assertTrue(
                response.reason()
                        .contains("Transaction amount is unusually large")
        );

        assertTrue(
                response.reason()
                        .contains("High transaction frequency detected")
        );

        assertEquals(
                assessedAt,
                response.assessedAt()
        );
    }

    /*
     * Transaction IDs and timestamps are normally generated
     * by JPA/Hibernate. These helpers allow the unit test
     * to create deterministic entity instances without
     * starting the Spring context.
     */
    private void setTransactionId(
            Transaction transaction,
            UUID transactionId
    ) {
        try {
            var field = Transaction.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(transaction, transactionId);
        } catch (ReflectiveOperationException exception) {
            throw new RuntimeException(exception);
        }
    }

    private void setTransactionCreatedAt(
            Transaction transaction,
            LocalDateTime createdAt
    ) {
        try {
            var field =
                    Transaction.class.getDeclaredField("createdAt");
            field.setAccessible(true);
            field.set(transaction, createdAt);
        } catch (ReflectiveOperationException exception) {
            throw new RuntimeException(exception);
        }
    }

    private void setAssessmentTimestamp(
            TransactionRiskAssessment assessment,
            LocalDateTime assessedAt
    ) {
        try {
            var field =
                    TransactionRiskAssessment.class
                            .getDeclaredField("assessedAt");
            field.setAccessible(true);
            field.set(assessment, assessedAt);
        } catch (ReflectiveOperationException exception) {
            throw new RuntimeException(exception);
        }
    }
}