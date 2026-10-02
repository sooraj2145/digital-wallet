package com.wallet.config;


import com.wallet.entity.Currency;
import com.wallet.entity.User;
import com.wallet.entity.Wallet;
import com.wallet.repository.UserRepository;
import com.wallet.repository.WalletRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;

@Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner initializeData(
            UserRepository userRepository,
            WalletRepository walletRepository
    ) {

        return args -> {

            if(userRepository.count() > 0) {
                return;
            }

            User user1 = userRepository.save(
                    new User("Alice", "alice@example.com")
            );

            User user2 = userRepository.save(
                    new User("Bob", "bob@example.com")
            );

            Wallet wallet1 = walletRepository.save(
                    new Wallet(user1, Currency.INR)
            );

            Wallet wallet2 = walletRepository.save(
                    new Wallet(user2, Currency.INR)
            );

            Wallet wallet3 = walletRepository.save(
                    new Wallet(user1, Currency.USD)
            );

            wallet1.setBalance(new BigDecimal("1000.00"));
            wallet2.setBalance(new BigDecimal("0.00"));
            wallet3.setBalance(new BigDecimal("1000.00"));

            walletRepository.save(wallet1);
            walletRepository.save(wallet2);
            walletRepository.save(wallet3);
        };
    }
}
