package com.wallet.config;


import com.wallet.dto.DepositRequest;
import com.wallet.entity.Currency;
import com.wallet.entity.User;
import com.wallet.entity.Wallet;
import com.wallet.repository.UserRepository;
import com.wallet.repository.WalletRepository;
import com.wallet.service.WalletService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.math.BigDecimal;

@Configuration
@Profile("!test")
public class DataInitializer {

    @Bean
    CommandLineRunner initializeData(
            UserRepository userRepository,
            WalletRepository walletRepository,
            WalletService walletService
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


            walletService.deposit(
                    wallet1.getId(),
                    new DepositRequest(
                            new BigDecimal("1000.00"),
                            "initializer-alice-opening-balance"
                    )
            );
        };
    }
}
