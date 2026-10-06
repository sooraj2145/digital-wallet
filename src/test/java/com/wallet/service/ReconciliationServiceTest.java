package com.wallet.service;

import com.wallet.dto.DepositRequest;
import com.wallet.entity.Currency;
import com.wallet.entity.User;
import com.wallet.entity.Wallet;
import com.wallet.exception.WalletNotFoundException;
import com.wallet.repository.UserRepository;
import com.wallet.repository.WalletRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class ReconciliationServiceTest {

    @Autowired
    private ReconciliationService reconciliationService;

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WalletService walletService;

    @Test
    void shouldReturnMatchForConsistentWallet() {

        String testId = UUID.randomUUID().toString();

        User user = userRepository.save(
                new User(
                        "Reconciliation User",
                        "reconciliation-" + testId + "@example.com"
                )
        );

        Wallet wallet = walletRepository.save(
                new Wallet(user, Currency.INR)
        );

        walletService.deposit(
                wallet.getId(),
                new DepositRequest(
                        new BigDecimal("800.00"),
                        "reconciliation-deposit-" + testId
                )
        );

        ReconciliationResult result =
                reconciliationService.reconcileWallet(
                        wallet.getId()
                );

        assertEquals(
                ReconciliationStatus.MATCH,
                result.status()
        );

        assertEquals(
                0,
                new BigDecimal("800.00")
                        .compareTo(result.walletBalance())
        );

        assertEquals(
                0,
                new BigDecimal("800.00")
                        .compareTo(result.ledgerBalance())
        );

        assertEquals(
                0,
                BigDecimal.ZERO.compareTo(result.difference())
        );
    }

    @Test
    void shouldThrowExceptionForMissingWallet() {

        assertThrows(
                WalletNotFoundException.class,
                () -> reconciliationService.reconcileWallet(
                        Long.MAX_VALUE
                )
        );
    }


    @Test
    void shouldReturnMismatchWhenWalletAndLedgerBalancesDiffer() {

        String testId = UUID.randomUUID().toString();

        User user = userRepository.save(
                new User(
                        "Mismatch Test User",
                        "mismatch-" + testId + "@example.com"
                )
        );

        Wallet wallet = walletRepository.save(
                new Wallet(user, Currency.INR)
        );

        // Deliberately create inconsistent state.
        // No ledger entry is created.
        wallet.setBalance(new BigDecimal("1000.00"));
        walletRepository.save(wallet);

        ReconciliationResult result =
                reconciliationService.reconcileWallet(
                        wallet.getId()
                );

        assertEquals(
                ReconciliationStatus.MISMATCH,
                result.status()
        );

        assertEquals(
                0,
                new BigDecimal("1000.00")
                        .compareTo(result.walletBalance())
        );

        assertEquals(
                0,
                BigDecimal.ZERO.compareTo(result.ledgerBalance())
        );

        assertEquals(
                0,
                new BigDecimal("1000.00")
                        .compareTo(result.difference())
        );
    }
}