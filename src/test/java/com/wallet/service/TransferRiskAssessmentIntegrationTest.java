package com.wallet.service;

import com.wallet.dto.DepositRequest;
import com.wallet.dto.TransferRequest;
import com.wallet.entity.Currency;
import com.wallet.entity.RiskLevel;
import com.wallet.entity.TransactionRiskAssessment;
import com.wallet.entity.User;
import com.wallet.entity.Wallet;
import com.wallet.repository.TransactionRiskAssessmentRepository;
import com.wallet.repository.UserRepository;
import com.wallet.repository.WalletRepository;
import com.wallet.repository.TransactionRepository;
import com.wallet.entity.Transaction;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class TransferRiskAssessmentIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private WalletService walletService;

    @Autowired
    private TransferService transferService;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private TransactionRiskAssessmentRepository assessmentRepository;

    @Test
    void shouldCreateRiskAssessmentWhenTransferIsCompleted() {

        // Arrange
        User sourceUser =
                userRepository.save(
                        new User(
                                "Risk Test Source",
                                "risk-source@example.com"
                        )
                );

        User destinationUser =
                userRepository.save(
                        new User(
                                "Risk Test Destination",
                                "risk-destination@example.com"
                        )
                );

        Wallet sourceWallet =
                walletRepository.save(
                        new Wallet(
                                sourceUser,
                                Currency.INR
                        )
                );

        Wallet destinationWallet =
                walletRepository.save(
                        new Wallet(
                                destinationUser,
                                Currency.INR
                        )
                );

        walletService.deposit(
                sourceWallet.getId(),
                new DepositRequest(
                        new BigDecimal("1000.00"),
                        "risk-test-deposit"
                )
        );

        TransferRequest transferRequest =
                new TransferRequest(
                        sourceWallet.getId(),
                        destinationWallet.getId(),
                        new BigDecimal("100.00"),
                        "risk-test-transfer"
                );

        // Act
        Transaction transaction =
                transferService.transfer(transferRequest);

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
    void shouldCreateHighRiskAssessmentForLargeTransfer() {

        // Arrange
        User sourceUser =
                userRepository.save(
                        new User(
                                "Large Transfer Source",
                                "large-source@example.com"
                        )
                );

        User destinationUser =
                userRepository.save(
                        new User(
                                "Large Transfer Destination",
                                "large-destination@example.com"
                        )
                );

        Wallet sourceWallet =
                walletRepository.save(
                        new Wallet(
                                sourceUser,
                                Currency.INR
                        )
                );

        Wallet destinationWallet =
                walletRepository.save(
                        new Wallet(
                                destinationUser,
                                Currency.INR
                        )
                );

        walletService.deposit(
                sourceWallet.getId(),
                new DepositRequest(
                        new BigDecimal("60000.00"),
                        "large-risk-test-deposit"
                )
        );

        TransferRequest transferRequest =
                new TransferRequest(
                        sourceWallet.getId(),
                        destinationWallet.getId(),
                        new BigDecimal("50000.00"),
                        "large-risk-test-transfer"
                );

        // Act
        Transaction transaction =
                transferService.transfer(transferRequest);

        // Assert
        TransactionRiskAssessment assessment =
                assessmentRepository
                        .findByTransactionId(transaction.getId())
                        .orElseThrow(() ->
                                new AssertionError(
                                        "Risk assessment was not created"
                                ));

        assertEquals(
                transaction.getId(),
                assessment.getTransaction().getId()
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
                        .contains(
                                "Transaction amount is unusually large"
                        )
        );

        assertNotNull(
                assessment.getAssessedAt()
        );
    }
}