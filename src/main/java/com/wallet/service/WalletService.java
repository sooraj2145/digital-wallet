package com.wallet.service;

import com.wallet.dto.DepositRequest;
import com.wallet.entity.LedgerEntry;
import com.wallet.entity.LedgerEntryType;
import com.wallet.entity.Transaction;
import com.wallet.entity.TransactionType;
import com.wallet.entity.Wallet;
import com.wallet.exception.WalletNotFoundException;
import com.wallet.repository.LedgerEntryRepository;
import com.wallet.repository.TransactionRepository;
import com.wallet.repository.WalletRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WalletService {

    private final WalletRepository walletRepository;
    private final TransactionRepository transactionRepository;
    private final LedgerEntryRepository ledgerEntryRepository;

    public WalletService(
            WalletRepository walletRepository,
            TransactionRepository transactionRepository,
            LedgerEntryRepository ledgerEntryRepository
    ) {
        this.walletRepository = walletRepository;
        this.transactionRepository = transactionRepository;
        this.ledgerEntryRepository = ledgerEntryRepository;
    }

    @Transactional
    public Transaction deposit(
            Long walletId,
            DepositRequest request
    ) {

        var existingTransaction =
                transactionRepository.findByIdempotencyKey(
                        request.idempotencyKey()
                );

        if (existingTransaction.isPresent()) {
            return existingTransaction.get();
        }

        Wallet wallet = walletRepository
                .findByIdForUpdate(walletId)
                .orElseThrow(() ->
                        new WalletNotFoundException(
                                "Wallet not found"
                        )
                );

        Transaction transaction = new Transaction(
                TransactionType.DEPOSIT,
                request.amount(),
                wallet.getCurrency(),
                request.idempotencyKey()
        );

        transactionRepository.save(transaction);

        LedgerEntry creditEntry = new LedgerEntry(
                transaction,
                wallet,
                LedgerEntryType.CREDIT,
                request.amount()
        );

        ledgerEntryRepository.save(creditEntry);

        wallet.setBalance(
                wallet.getBalance().add(request.amount())
        );

        return transaction;
    }
}