package com.wallet.service;

import com.wallet.dto.DepositRequest;
import com.wallet.dto.TransferRequest;
import com.wallet.entity.*;
import com.wallet.repository.LedgerEntryRepository;
import com.wallet.repository.TransactionRepository;
import com.wallet.repository.UserRepository;
import com.wallet.repository.WalletRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@ActiveProfiles("test")
public class TransferServiceConcurrencyTest {

    @Autowired
    private TransferService transferService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private WalletService walletService;

    @Autowired
    private LedgerEntryRepository ledgerEntryRepository;

    @Test
    void concurrentTransfersShouldNotDeadlock() throws Exception {

        String testId = UUID.randomUUID().toString();

        User user1 = userRepository.save(new User(
                "Concurrency User 1",
                "concurrency1-" + testId + "@example.com"
        ));

        User user2 = userRepository.save(new User(
                "Concurrency User 2",
                "concurrency2-" + testId + "@example.com"
        ));

        Wallet wallet1 = walletRepository.save(
                new Wallet(user1, Currency.INR)
        );

        Wallet wallet2 = walletRepository.save(
                new Wallet(user2, Currency.INR)
        );

        walletService.deposit(
                wallet1.getId(),
                new DepositRequest(
                        new BigDecimal("1000.00"),
                        "concurrency-opening-1-" + testId
                )
        );

        walletService.deposit(
                wallet2.getId(),
                new DepositRequest(
                        new BigDecimal("1000.00"),
                        "concurrency-opening-2-" + testId
                )
        );

        String idempotencyKey1 =
                "concurrent-transfer-1-" + testId;

        String idempotencyKey2 =
                "concurrent-transfer-2-" + testId;

        ExecutorService executorService =
                Executors.newFixedThreadPool(2);

        CountDownLatch startSignal =
                new CountDownLatch(1);

        Callable<Void> transfer1 = () -> {

            startSignal.await();

            transferService.transfer(
                    new TransferRequest(
                            wallet1.getId(),
                            wallet2.getId(),
                            new BigDecimal("100.00"),
                            idempotencyKey1
                    )
            );

            return null;
        };

        Callable<Void> transfer2 = () -> {

            startSignal.await();

            transferService.transfer(
                    new TransferRequest(
                            wallet2.getId(),
                            wallet1.getId(),
                            new BigDecimal("100.00"),
                            idempotencyKey2
                    )
            );

            return null;
        };

        Future<Void> future1 =
                executorService.submit(transfer1);

        Future<Void> future2 =
                executorService.submit(transfer2);

        startSignal.countDown();

        future1.get(10, TimeUnit.SECONDS);
        future2.get(10, TimeUnit.SECONDS);

        executorService.shutdown();

        Wallet finalWallet1 =
                walletRepository.findById(wallet1.getId())
                        .orElseThrow();

        Wallet finalWallet2 =
                walletRepository.findById(wallet2.getId())
                        .orElseThrow();

        assertEquals(
                0,
                new BigDecimal("1000.00")
                        .compareTo(finalWallet1.getBalance())
        );

        assertEquals(
                0,
                new BigDecimal("1000.00")
                        .compareTo(finalWallet2.getBalance())
        );
    }

    @Test
    void sameIdempotencyKeyShouldNotProcessTransferTwice() {

        String testId = UUID.randomUUID().toString();

        User user1 = userRepository.save(
                new User(
                        "Idempotency User 1",
                        "idempotency1-" + testId + "@example.com"
                )
        );

        User user2 = userRepository.save(
                new User(
                        "Idempotency User 2",
                        "idempotency2-" + testId + "@example.com"
                )
        );

        Wallet wallet1 = walletRepository.save(
                new Wallet(user1, Currency.INR)
        );

        Wallet wallet2 = walletRepository.save(
                new Wallet(user2, Currency.INR)
        );

        walletService.deposit(
                wallet1.getId(),
                new DepositRequest(
                        new BigDecimal("1000.00"),
                        "idempotency-opening-" + testId
                )
        );

        String idempotencyKey =
                "test-key-" + testId;

        TransferRequest request =
                new TransferRequest(
                        wallet1.getId(),
                        wallet2.getId(),
                        new BigDecimal("500.00"),
                        idempotencyKey
                );

        transferService.transfer(request);
        transferService.transfer(request);

        Wallet updatedWallet1 =
                walletRepository.findById(wallet1.getId())
                        .orElseThrow();

        Wallet updatedWallet2 =
                walletRepository.findById(wallet2.getId())
                        .orElseThrow();

        assertEquals(
                0,
                new BigDecimal("500.00")
                        .compareTo(updatedWallet1.getBalance())
        );

        assertEquals(
                0,
                new BigDecimal("500.00")
                        .compareTo(updatedWallet2.getBalance())
        );

        assertEquals(
                1,
                transactionRepository.findAll()
                        .stream()
                        .filter(transaction ->
                                idempotencyKey.equals(
                                        transaction.getIdempotencyKey()
                                )
                        )
                        .count()
        );
    }

    @Test
    void concurrentRequestsWithSameIdempotencyKeyShouldProcessOnlyOnce()
            throws InterruptedException, ExecutionException {

        String testId = UUID.randomUUID().toString();

        User user1 = userRepository.save(
                new User(
                        "Concurrent Idempotency User 1",
                        "concurrent1-" + testId + "@example.com"
                )
        );

        User user2 = userRepository.save(
                new User(
                        "Concurrent Idempotency User 2",
                        "concurrent2-" + testId + "@example.com"
                )
        );

        Wallet wallet1 = walletRepository.save(
                new Wallet(user1, Currency.INR)
        );

        Wallet wallet2 = walletRepository.save(
                new Wallet(user2, Currency.INR)
        );

        walletService.deposit(
                wallet1.getId(),
                new DepositRequest(
                        new BigDecimal("1000.00"),
                        "concurrent-idempotency-opening-" + testId
                )
        );

        String idempotencyKey =
                "concurrent-key-" + testId;

        TransferRequest request =
                new TransferRequest(
                        wallet1.getId(),
                        wallet2.getId(),
                        new BigDecimal("500.00"),
                        idempotencyKey
                );

        ExecutorService executor =
                Executors.newFixedThreadPool(2);

        List<Future<?>> futures =
                new ArrayList<>();

        futures.add(
                executor.submit(() -> {
                    try {
                        transferService.transfer(request);
                    } catch (Exception ignored) {
                    }
                })
        );

        futures.add(
                executor.submit(() -> {
                    try {
                        transferService.transfer(request);
                    } catch (Exception ignored) {
                    }
                })
        );

        for (Future<?> future : futures) {
            future.get();
        }

        executor.shutdown();

        Wallet updatedWallet1 =
                walletRepository.findById(wallet1.getId())
                        .orElseThrow();

        Wallet updatedWallet2 =
                walletRepository.findById(wallet2.getId())
                        .orElseThrow();

        assertEquals(
                0,
                new BigDecimal("500.00")
                        .compareTo(updatedWallet1.getBalance())
        );

        assertEquals(
                0,
                new BigDecimal("500.00")
                        .compareTo(updatedWallet2.getBalance())
        );

        long transactionCount =
                transactionRepository.findAll()
                        .stream()
                        .filter(transaction ->
                                idempotencyKey.equals(
                                        transaction.getIdempotencyKey()
                                )
                        )
                        .count();

        assertEquals(1, transactionCount);
    }

    @Test
    void depositShouldIncreaseWalletBalanceAndCreateCreditLedgerEntry() {

        String testId = UUID.randomUUID().toString();

        User user = userRepository.save(
                new User(
                        "Deposit Test User",
                        "deposit-" + testId + "@example.com"
                )
        );

        Wallet wallet = walletRepository.save(
                new Wallet(user, Currency.INR)
        );

        DepositRequest request =
                new DepositRequest(
                        new BigDecimal("500.00"),
                        "deposit-test-" + testId
                );

        Transaction transaction =
                walletService.deposit(
                        wallet.getId(),
                        request
                );

        Wallet updatedWallet =
                walletRepository.findById(wallet.getId())
                        .orElseThrow();

        assertEquals(
                0,
                new BigDecimal("500.00")
                        .compareTo(updatedWallet.getBalance())
        );

        List<LedgerEntry> entries =
                ledgerEntryRepository.findByTransactionId(
                        transaction.getId()
                );

        assertEquals(1, entries.size());

        LedgerEntry entry = entries.get(0);

        assertEquals(
                LedgerEntryType.CREDIT,
                entry.getEntryType()
        );

        assertEquals(
                0,
                new BigDecimal("500.00")
                        .compareTo(entry.getAmount())
        );

        assertEquals(
                transaction.getId(),
                entry.getTransaction().getId()
        );
    }

    @Test
    void walletBalanceShouldMatchLedgerBalance() {

        List<Wallet> wallets =
                walletRepository.findAll();

        for (Wallet wallet : wallets) {

            BigDecimal ledgerBalance =
                    ledgerEntryRepository
                            .findAllByWalletIdWithTransaction(
                                    wallet.getId()
                            )
                            .stream()
                            .map(entry -> {

                                if (entry.getEntryType()
                                        == LedgerEntryType.CREDIT) {

                                    return entry.getAmount();
                                }

                                return entry.getAmount().negate();
                            })
                            .reduce(
                                    BigDecimal.ZERO,
                                    BigDecimal::add
                            );

            assertEquals(
                    0,
                    wallet.getBalance()
                            .compareTo(ledgerBalance),
                    "Wallet balance does not match ledger balance for wallet "
                            + wallet.getId()
            );
        }
    }
}