package com.wallet.repository;

import com.wallet.entity.Currency;
import com.wallet.entity.Wallet;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface WalletRepository extends JpaRepository<Wallet, Long> {

    Optional<Wallet> findByUserIdAndCurrency(Long userId, Currency currency);

    boolean existsByUserIdAndCurrency(Long userId, Currency currency);
}
