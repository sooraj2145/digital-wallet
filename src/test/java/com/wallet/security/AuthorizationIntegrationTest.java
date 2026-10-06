package com.wallet.security;

import com.wallet.dto.DepositRequest;
import com.wallet.dto.TransferRequest;
import com.wallet.entity.Currency;
import com.wallet.entity.User;
import com.wallet.entity.Wallet;
import com.wallet.exception.WalletNotFoundException;
import com.wallet.repository.UserRepository;
import com.wallet.repository.WalletRepository;
import com.wallet.service.TransferService;
import com.wallet.service.WalletService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.context.ActiveProfiles;
import com.wallet.entity.LedgerEntry;
import com.wallet.repository.LedgerEntryRepository;
import com.wallet.service.TransactionService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import com.wallet.dto.WalletTransactionResponse;


import java.math.BigDecimal;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@ActiveProfiles("test")
class AuthorizationIntegrationTest {

    @Autowired
    private WalletService walletService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private TransferService transferService;

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private LedgerEntryRepository ledgerEntryRepository;

    @Test
    void userCannotDepositIntoAnotherUsersWallet() {

        String suffix = String.valueOf(System.nanoTime());

        User userA = userRepository.save(
                new User(
                        "Authorization User A",
                        "authorization-a-" + suffix + "@example.com"
                )
        );

        User userB = userRepository.save(
                new User(
                        "Authorization User B",
                        "authorization-b-" + suffix + "@example.com"
                )
        );

        Wallet walletB = walletRepository.save(
                new Wallet(userB, Currency.INR)
        );

        Authentication authentication =
                createAuthentication(userA.getId());

        DepositRequest request =
                new DepositRequest(
                        new BigDecimal("500.00"),
                        "authorization-test-" + suffix
                );

        assertThrows(
                WalletNotFoundException.class,
                () -> walletService.deposit(
                        walletB.getId(),
                        request,
                        authentication
                )
        );

        Wallet unchangedWallet =
                walletRepository.findById(walletB.getId())
                        .orElseThrow();

        assertEquals(
                0,
                BigDecimal.ZERO.compareTo(
                        unchangedWallet.getBalance()
                )
        );
    }

    @Test
    void transferShouldEnforceSourceWalletOwnership() {

        String suffix = String.valueOf(System.nanoTime());

        User userA = userRepository.save(
                new User(
                        "Transfer User A",
                        "transfer-a-" + suffix + "@example.com"
                )
        );

        User userB = userRepository.save(
                new User(
                        "Transfer User B",
                        "transfer-b-" + suffix + "@example.com"
                )
        );

        Wallet walletA = walletRepository.save(
                new Wallet(userA, Currency.INR)
        );

        Wallet walletB = walletRepository.save(
                new Wallet(userB, Currency.INR)
        );

        Authentication userAAuthentication =
                createAuthentication(userA.getId());

        /*
         * Give User A an opening balance.
         */
        walletService.deposit(
                walletA.getId(),
                new DepositRequest(
                        new BigDecimal("1000.00"),
                        "transfer-opening-" + suffix
                )
        );

        /*
         * User A -> User B should succeed.
         */
        transferService.transfer(
                new TransferRequest(
                        walletA.getId(),
                        walletB.getId(),
                        new BigDecimal("300.00"),
                        "authorized-transfer-" + suffix
                ),
                userAAuthentication
        );

        Wallet updatedWalletA =
                walletRepository.findById(walletA.getId())
                        .orElseThrow();

        Wallet updatedWalletB =
                walletRepository.findById(walletB.getId())
                        .orElseThrow();

        assertEquals(
                0,
                new BigDecimal("700.00")
                        .compareTo(updatedWalletA.getBalance())
        );

        assertEquals(
                0,
                new BigDecimal("300.00")
                        .compareTo(updatedWalletB.getBalance())
        );

        /*
         * User A attempts to transfer money
         * from User B's wallet.
         *
         * This must be rejected.
         */
        assertThrows(
                WalletNotFoundException.class,
                () -> transferService.transfer(
                        new TransferRequest(
                                walletB.getId(),
                                walletA.getId(),
                                new BigDecimal("100.00"),
                                "unauthorized-transfer-" + suffix
                        ),
                        userAAuthentication
                )
        );

        /*
         * User B's balance must remain unchanged.
         */
        Wallet finalWalletB =
                walletRepository.findById(walletB.getId())
                        .orElseThrow();

        assertEquals(
                0,
                new BigDecimal("300.00")
                        .compareTo(finalWalletB.getBalance())
        );
    }

    @Test
    void userCannotViewAnotherUsersTransactionHistory() {

        String suffix = String.valueOf(System.nanoTime());

        User userA = userRepository.save(
                new User(
                        "History User A",
                        "history-a-" + suffix + "@example.com"
                )
        );

        User userB = userRepository.save(
                new User(
                        "History User B",
                        "history-b-" + suffix + "@example.com"
                )
        );

        Wallet walletB = walletRepository.save(
                new Wallet(userB, Currency.INR)
        );

        Authentication userAAuthentication =
                createAuthentication(userA.getId());

        assertThrows(
                WalletNotFoundException.class,
                () -> transactionService.getTransactionsByWallet(
                        walletB.getId(),
                        PageRequest.of(0, 10),
                        userAAuthentication
                )
        );
    }

    @Test
    void userCannotViewAnotherUsersWalletStatement() {

        String suffix = String.valueOf(System.nanoTime());

        User userA = userRepository.save(
                new User(
                        "Statement User A",
                        "statement-a-" + suffix + "@example.com"
                )
        );

        User userB = userRepository.save(
                new User(
                        "Statement User B",
                        "statement-b-" + suffix + "@example.com"
                )
        );

        Wallet walletB = walletRepository.save(
                new Wallet(userB, Currency.INR)
        );

        Authentication userAAuthentication =
                createAuthentication(userA.getId());

        assertThrows(
                WalletNotFoundException.class,
                () -> transactionService.getWalletStatement(
                        walletB.getId(),
                        PageRequest.of(0, 10),
                        userAAuthentication
                )
        );
    }

    private Authentication createAuthentication(Long userId) {

        Jwt jwt = Jwt.withTokenValue("test-token")
                .header("alg", "HS256")
                .subject(userId.toString())
                .issuedAt(Instant.now())
                .expiresAt(
                        Instant.now().plusSeconds(3600)
                )
                .claim("email", "test@example.com")
                .build();

        return new UsernamePasswordAuthenticationToken(
                jwt,
                null
        );
    }
}