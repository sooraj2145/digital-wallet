package com.wallet.service;

import com.wallet.entity.LedgerEntry;
import com.wallet.entity.LedgerEntryType;
import com.wallet.entity.Wallet;
import com.wallet.exception.WalletNotFoundException;
import com.wallet.repository.LedgerEntryRepository;
import com.wallet.repository.WalletRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class ReconciliationService {

    private final WalletRepository walletRepository;
    private final LedgerEntryRepository ledgerEntryRepository;

    public ReconciliationService(
            WalletRepository walletRepository,
            LedgerEntryRepository ledgerEntryRepository
    ) {
        this.walletRepository = walletRepository;
        this.ledgerEntryRepository = ledgerEntryRepository;
    }

    @Transactional(readOnly = true)
    public ReconciliationResult reconcileWallet(Long walletId) {

        Wallet wallet = walletRepository.findById(walletId)
                .orElseThrow(() ->
                        new WalletNotFoundException("Wallet not found")
                );

        List<LedgerEntry> entries =
                ledgerEntryRepository.findAllByWalletIdWithTransaction(
                        walletId
                );

        BigDecimal ledgerBalance = entries.stream()
                .map(entry -> {
                    if (entry.getEntryType() == LedgerEntryType.CREDIT) {
                        return entry.getAmount();
                    }

                    return entry.getAmount().negate();
                })
                .reduce(
                        BigDecimal.ZERO,
                        BigDecimal::add
                );

        BigDecimal walletBalance = wallet.getBalance();

        BigDecimal difference =
                walletBalance.subtract(ledgerBalance);

        ReconciliationStatus status =
                difference.compareTo(BigDecimal.ZERO) == 0
                        ? ReconciliationStatus.MATCH
                        : ReconciliationStatus.MISMATCH;

        return new ReconciliationResult(
                wallet.getId(),
                wallet.getCurrency(),
                walletBalance,
                ledgerBalance,
                difference,
                status
        );
    }
}