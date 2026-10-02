package com.wallet.service;

import com.wallet.dto.TransferRequest;
import com.wallet.entity.Currency;
import com.wallet.entity.User;
import com.wallet.entity.Wallet;
import com.wallet.repository.UserRepository;
import com.wallet.repository.WalletRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
public class TransferServiceConcurrencyTest {

    @Autowired
    private TransferService transferService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WalletRepository walletRepository;

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

        Wallet wallet1 = walletRepository.save(new Wallet(
                user1, Currency.INR
        ));
        Wallet wallet2 = walletRepository.save(new Wallet(
                user2, Currency.INR
        ));

        wallet1.setBalance(new BigDecimal("1000.00"));
        wallet2.setBalance(new BigDecimal("1000.00"));

        walletRepository.save(wallet1);
        walletRepository.save(wallet2);

        ExecutorService executorService = Executors.newFixedThreadPool(2);

        CountDownLatch startSignal = new CountDownLatch(1);

        Callable<Void> transfer1 = () -> {
            startSignal.await();

            transferService.transfer(
                    new TransferRequest(
                            wallet1.getId(),
                            wallet2.getId(),
                            new BigDecimal("100.00")
                    )
            );
            return null;
        };

        Callable<Void> transfer2 = () ->  {
            startSignal.await();

            transferService.transfer(
                    new TransferRequest(
                            wallet2.getId(),
                            wallet1.getId(),
                            new BigDecimal("100.00")
                    )
            );
            return null;
        };

        Future<Void> future1 = executorService.submit(transfer1);
        Future<Void> future2 = executorService.submit(transfer2);

        startSignal.countDown();

        future1.get(10, TimeUnit.SECONDS);
        future2.get(10, TimeUnit.SECONDS);

        executorService.shutdown();

        Wallet finalWallet1 = walletRepository.findById(wallet1.getId()).orElseThrow(null);
        Wallet finalWallet2 = walletRepository.findById(wallet2.getId()).orElseThrow(null);

        assertEquals(
                0,
                new BigDecimal("1000.00").compareTo(finalWallet1.getBalance())
        );

        assertEquals(
                0,
                new BigDecimal("1000.00").compareTo(finalWallet2.getBalance())
        );
    }
}
