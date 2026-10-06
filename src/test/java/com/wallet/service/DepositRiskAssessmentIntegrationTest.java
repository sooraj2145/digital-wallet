package com.wallet.service;

import com.wallet.dto.DepositRequest;
import com.wallet.entity.Currency;
import com.wallet.entity.RiskLevel;
import com.wallet.entity.Transaction;
import com.wallet.entity.TransactionRiskAssessment;
import com.wallet.entity.User;
import com.wallet.entity.Wallet;
import com.wallet.repository.TransactionRiskAssessmentRepository;
import com.wallet.repository.UserRepository;
import com.wallet.repository.WalletRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class DepositRiskAssessmentIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private WalletService walletService;

    @Autowired
    private TransactionRiskAssessmentRepository assessmentRepository;

    @Test
    void shouldCreateRiskAssessmentWhenDepositIsCompleted() {

        // Arrange
        User user =
                userRepository.save(
                        new User(
                                "Deposit Risk User",
                                "deposit-risk@example.com"
                        )
                );

        Wallet wallet =
                walletRepository.save(
                        new Wallet(
                                user,
                                Currency.INR
                        )
                );

        DepositRequest request =
                new DepositRequest(
                        new BigDecimal("1000.00"),
                        "deposit-risk-test"
                );

        // Act
        Transaction transaction =
                walletService.deposit(
                        wallet.getId(),
                        request
                );

        // Assert
        assertNotNull(transaction);
        assertNotNull(transaction.getId());

        TransactionRiskAssessment assessment =
                assessmentRepository
                        .findByTransactionId(transaction.getId())
                        .orElseThrow(() ->
                                new AssertionError(
                                        "Risk assessment was not created"
                                ));

        assertNotNull(assessment);

        assertEquals(
                transaction.getId(),
                assessment.getTransaction().getId()
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

        assertNotNull(
                assessment.getAssessedAt()
        );
    }

    @Test
    void shouldNotCreateDuplicateRiskAssessmentForIdempotentDeposit() {

        // Arrange
        User user =
                userRepository.save(
                        new User(
                                "Idempotent Deposit User",
                                "idempotent-deposit@example.com"
                        )
                );

        Wallet wallet =
                walletRepository.save(
                        new Wallet(
                                user,
                                Currency.INR
                        )
                );

        DepositRequest request =
                new DepositRequest(
                        new BigDecimal("1000.00"),
                        "same-deposit-key"
                );

        // Act
        Transaction firstTransaction =
                walletService.deposit(
                        wallet.getId(),
                        request
                );

        Transaction secondTransaction =
                walletService.deposit(
                        wallet.getId(),
                        request
                );

        // Assert
        assertEquals(
                firstTransaction.getId(),
                secondTransaction.getId()
        );

        assertEquals(
                1,
                assessmentRepository
                        .findByTransactionId(firstTransaction.getId())
                        .stream()
                        .count()
        );

        Wallet updatedWallet =
                walletRepository
                        .findById(wallet.getId())
                        .orElseThrow();

        assertEquals(
                0,
                updatedWallet.getBalance()
                        .compareTo(new BigDecimal("1000.00"))
        );
    }

    @Test
    void shouldCreateMediumRiskAssessmentForLargeDeposit() {

        // Arrange
        User user =
                userRepository.save(
                        new User(
                                "Large Deposit User",
                                "large-deposit@example.com"
                        )
                );

        Wallet wallet =
                walletRepository.save(
                        new Wallet(
                                user,
                                Currency.INR
                        )
                );

        DepositRequest request =
                new DepositRequest(
                        new BigDecimal("50000.00"),
                        "large-deposit-risk-test"
                );

        // Act
        Transaction transaction =
                walletService.deposit(
                        wallet.getId(),
                        request
                );

        // Assert
        TransactionRiskAssessment assessment =
                assessmentRepository
                        .findByTransactionId(transaction.getId())
                        .orElseThrow(() ->
                                new AssertionError(
                                        "Risk assessment was not created"
                                ));

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
                        .contains(
                                "Transaction amount is unusually large"
                        )
        );
    }
}