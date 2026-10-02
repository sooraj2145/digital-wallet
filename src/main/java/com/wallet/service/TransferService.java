package com.wallet.service;


import com.wallet.entity.LedgerEntry;
import com.wallet.entity.LedgerEntryType;
import com.wallet.exception.InsufficientBalanceException;
import com.wallet.exception.InvalidTransferException;
import com.wallet.exception.WalletNotFoundException;
import com.wallet.repository.LedgerEntryRepository;
import com.wallet.entity.Currency;
import com.wallet.entity.Transaction;
import com.wallet.entity.TransactionType;
import com.wallet.repository.TransactionRepository;
import com.wallet.dto.TransferRequest;
import com.wallet.entity.Wallet;
import com.wallet.repository.WalletRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class TransferService {

    private final WalletRepository walletRepository;
    private final TransactionRepository transactionRepository;
    private final LedgerEntryRepository ledgerEntryRepository;

    public TransferService(
            WalletRepository walletRepository,
            TransactionRepository transactionRepository,
            LedgerEntryRepository ledgerEntryRepository
    ) {
        this.walletRepository = walletRepository;
        this.transactionRepository = transactionRepository;
        this.ledgerEntryRepository = ledgerEntryRepository;
    }

    @Transactional
    public void transfer(TransferRequest request) {

        Long sourceWalletId = request.sourceWalletId();
        Long destinationWalletId = request.destinationWalletId();


        if (sourceWalletId.equals(destinationWalletId)) {
            throw new InvalidTransferException(
                    "Source and destination wallets must be different"
            );
        }

        Long firstWalletId = Math.min(sourceWalletId, destinationWalletId);
        Long secondWalletId = Math.max(sourceWalletId, destinationWalletId);

        Wallet firstWallet = walletRepository
                .findByIdForUpdate(firstWalletId)
                .orElseThrow(() ->
                        new WalletNotFoundException("Wallet not found"));


        Wallet secondWallet = walletRepository
                .findByIdForUpdate(secondWalletId)
                .orElseThrow(() ->
                        new WalletNotFoundException("Wallet not found"));

        Wallet sourceWallet;
        Wallet destinationWallet;

        if(sourceWalletId.equals(firstWalletId)) {
            sourceWallet = firstWallet;
            destinationWallet = secondWallet;
        } else {
            sourceWallet = secondWallet;
            destinationWallet = firstWallet;
        }

        if (sourceWallet.getCurrency() != destinationWallet.getCurrency()) {
            throw new InvalidTransferException(
                    "Source and destination wallets must use the same currency"
            );
        }

        BigDecimal amount = request.amount();

        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidTransferException(
                    "Transfer amount must be greater than zero"
            );
        }

        if (sourceWallet.getBalance().compareTo(amount) < 0) {
            throw new InsufficientBalanceException(
                    "Insufficient wallet balance"
            );
        }

        Transaction transaction = new Transaction(
                TransactionType.TRANSFER,
                amount,
                sourceWallet.getCurrency()
        );

        transactionRepository.save(transaction);

        LedgerEntry ledgerEntry = new LedgerEntry(
                transaction,
                sourceWallet,
                LedgerEntryType.DEBIT,
                amount
        );

        ledgerEntryRepository.save(ledgerEntry);

        LedgerEntry creditEntry = new LedgerEntry(
                transaction,
                destinationWallet,
                LedgerEntryType.CREDIT,
                amount
        );

        ledgerEntryRepository.save(creditEntry);

        sourceWallet.setBalance(
                sourceWallet.getBalance().subtract(amount)
        );

        destinationWallet.setBalance(
                destinationWallet.getBalance().add(amount)
        );



    }


    private Wallet getWalletForUpdate(Long walletId, String walletRole) {
        return walletRepository
                .findByIdForUpdate(walletId)
                .orElseThrow(() -> new WalletNotFoundException(walletRole + " wallet not found."));
    }




}
