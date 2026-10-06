package com.wallet.service;

import com.wallet.dto.TransferRequest;
import com.wallet.entity.LedgerEntry;
import com.wallet.entity.LedgerEntryType;
import com.wallet.entity.Transaction;
import com.wallet.entity.Wallet;
import com.wallet.exception.InsufficientBalanceException;
import com.wallet.exception.InvalidTransferException;
import com.wallet.exception.WalletNotFoundException;
import com.wallet.repository.LedgerEntryRepository;
import com.wallet.repository.WalletRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class TransferService {

    private final WalletRepository walletRepository;
    private final LedgerEntryRepository ledgerEntryRepository;
    private final TransactionCreationService transactionCreationService;
    private final IdempotencyFingerprintService fingerprintService;

    public TransferService(
            WalletRepository walletRepository,
            LedgerEntryRepository ledgerEntryRepository,
            TransactionCreationService transactionCreationService,
            IdempotencyFingerprintService fingerprintService
    ) {
        this.walletRepository = walletRepository;
        this.ledgerEntryRepository = ledgerEntryRepository;
        this.transactionCreationService = transactionCreationService;
        this.fingerprintService = fingerprintService;
    }

    @Transactional
    public Transaction transfer(TransferRequest request) {

        Long sourceWalletId = request.sourceWalletId();
        Long destinationWalletId = request.destinationWalletId();

        if (sourceWalletId.equals(destinationWalletId)) {
            throw new InvalidTransferException(
                    "Source and destination wallets must be different"
            );
        }

        Long firstWalletId =
                Math.min(sourceWalletId, destinationWalletId);

        Long secondWalletId =
                Math.max(sourceWalletId, destinationWalletId);

        Wallet firstWallet = walletRepository
                .findByIdForUpdate(firstWalletId)
                .orElseThrow(() ->
                        new WalletNotFoundException(
                                "Wallet not found"
                        )
                );

        Wallet secondWallet = walletRepository
                .findByIdForUpdate(secondWalletId)
                .orElseThrow(() ->
                        new WalletNotFoundException(
                                "Wallet not found"
                        )
                );

        Wallet sourceWallet;
        Wallet destinationWallet;

        if (sourceWalletId.equals(firstWalletId)) {
            sourceWallet = firstWallet;
            destinationWallet = secondWallet;
        } else {
            sourceWallet = secondWallet;
            destinationWallet = firstWallet;
        }

        if (sourceWallet.getCurrency()
                != destinationWallet.getCurrency()) {

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

        String requestFingerprint =
                fingerprintService.fingerprintTransfer(
                        sourceWallet.getId(),
                        destinationWallet.getId(),
                        amount,
                        sourceWallet.getCurrency().name()
                );

        TransactionCreationResult result =
                transactionCreationService.create(
                        amount,
                        sourceWallet.getCurrency(),
                        request.idempotencyKey(),
                        requestFingerprint
                );


        if (!result.created()) {
            return result.transaction();
        }

        Transaction transaction = result.transaction();

        LedgerEntry debitEntry = new LedgerEntry(
                transaction,
                sourceWallet,
                LedgerEntryType.DEBIT,
                amount
        );

        ledgerEntryRepository.save(debitEntry);

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

        return transaction;
    }
}