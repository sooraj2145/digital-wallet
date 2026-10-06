package com.wallet.service;

import com.wallet.entity.LedgerEntry;
import com.wallet.entity.LedgerEntryType;
import com.wallet.entity.Wallet;
import com.wallet.exception.WalletNotFoundException;
import com.wallet.repository.LedgerEntryRepository;
import com.wallet.repository.WalletRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class ReconciliationService {

    private final WalletRepository walletRepository;
    private final LedgerEntryRepository ledgerEntryRepository;
    private final AuthenticationService authenticationService;

    public ReconciliationService(
            WalletRepository walletRepository,
            LedgerEntryRepository ledgerEntryRepository,
            AuthenticationService authenticationService
    ) {
        this.walletRepository = walletRepository;
        this.ledgerEntryRepository = ledgerEntryRepository;
        this.authenticationService = authenticationService;
    }

    /**
     * Authenticated reconciliation used by the REST API.
     * Verifies that the wallet belongs to the authenticated user.
     */
    @Transactional(readOnly = true)
    public ReconciliationResult reconcileWallet(
            Long walletId,
            Authentication authentication
    ) {

        Long currentUserId =
                authenticationService.getCurrentUserId(authentication);

        Wallet wallet = walletRepository
                .findByIdAndUserId(walletId, currentUserId)
                .orElseThrow(() ->
                        new WalletNotFoundException("Wallet not found"));

        return reconcile(wallet);
    }

    /**
     * Internal reconciliation method used by existing service tests.
     */
    @Transactional(readOnly = true)
    public ReconciliationResult reconcileWallet(Long walletId) {

        Wallet wallet = walletRepository
                .findById(walletId)
                .orElseThrow(() ->
                        new WalletNotFoundException("Wallet not found"));

        return reconcile(wallet);
    }

    /**
     * Performs the actual reconciliation calculation.
     */
    private ReconciliationResult reconcile(Wallet wallet) {

        List<LedgerEntry> entries =
                ledgerEntryRepository.findAllByWalletIdWithTransaction(
                        wallet.getId()
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