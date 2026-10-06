package com.wallet.service;


import com.wallet.dto.WalletTransactionResponse;
import com.wallet.entity.LedgerEntry;
import com.wallet.entity.LedgerEntryType;
import com.wallet.entity.Transaction;
import com.wallet.exception.WalletNotFoundException;
import com.wallet.repository.LedgerEntryRepository;
import com.wallet.repository.TransactionRepository;
import com.wallet.repository.WalletRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final WalletRepository walletRepository;
    private final LedgerEntryRepository ledgerEntryRepository;
    private final AuthenticationService authenticationService;

    public TransactionService(
            TransactionRepository transactionRepository,
            WalletRepository walletRepository,
            LedgerEntryRepository ledgerEntryRepository,
            AuthenticationService authenticationService
    ) {
        this.transactionRepository = transactionRepository;
        this.walletRepository = walletRepository;
        this.ledgerEntryRepository = ledgerEntryRepository;
        this.authenticationService = authenticationService;
    }

    public Page<Transaction> getTransactions(
            Pageable pageable,
            Authentication authentication
    ) {
        Long currentUserId =
                authenticationService.getCurrentUserId(authentication);

        return transactionRepository.findByUserId(
                currentUserId,
                pageable
        );
    }

    public Page<Transaction> getTransactionsByWallet(
            Long walletId,
            Pageable pageable,
            Authentication  authentication
    ) {

        Long currentUserId = authenticationService.getCurrentUserId(authentication);


        if (!walletRepository.findByIdAndUserId(
                walletId,
                currentUserId
        ).isPresent()) {
            throw new WalletNotFoundException(
                    "Wallet not found"
            );
        }
        return transactionRepository.findByWalletId(walletId, pageable);
    }

    public Page<WalletTransactionResponse> getWalletStatement(
            Long walletId,
            Pageable pageable,
            Authentication  authentication
    ) {

        Long currentUserId = authenticationService.getCurrentUserId(authentication);



        if (!walletRepository.findByIdAndUserId(
                walletId,
                currentUserId
        ).isPresent()) {
            throw new WalletNotFoundException(
                    "Wallet not found"
            );
        }

        List<LedgerEntry> allEntries =
                ledgerEntryRepository
                        .findAllByWalletIdWithTransaction(walletId);

        Map<UUID, BigDecimal> balanceAfterByTransaction =
                new HashMap<>();

        BigDecimal runningBalance = BigDecimal.ZERO;

        for (LedgerEntry entry : allEntries) {

            if (entry.getEntryType() == LedgerEntryType.CREDIT) {
                runningBalance =
                        runningBalance.add(entry.getAmount());
            } else {
                runningBalance =
                        runningBalance.subtract(entry.getAmount());
            }

            balanceAfterByTransaction.put(
                    entry.getTransaction().getId(),
                    runningBalance
            );
        }

        Page<LedgerEntry> page =
                ledgerEntryRepository
                        .findByWalletIdWithTransaction(
                                walletId,
                                pageable
                        );

        return page.map(ledgerEntry ->
                new WalletTransactionResponse(
                        ledgerEntry.getTransaction().getId(),
                        ledgerEntry.getTransaction().getType(),
                        ledgerEntry.getEntryType(),
                        ledgerEntry.getAmount(),
                        balanceAfterByTransaction.get(
                                ledgerEntry.getTransaction().getId()
                        ),
                        ledgerEntry.getTransaction().getCurrency(),
                        ledgerEntry.getTransaction().getIdempotencyKey(),
                        ledgerEntry.getTransaction().getCreatedAt()
                )
        );
    }
}
