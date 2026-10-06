package com.wallet.service;

import com.wallet.dto.DepositRequest;
import com.wallet.entity.*;
import com.wallet.event.TransactionCompletedEvent;
import com.wallet.exception.InvalidTransferException;
import com.wallet.exception.WalletNotFoundException;
import com.wallet.repository.LedgerEntryRepository;
import com.wallet.repository.TransactionRepository;
import com.wallet.repository.WalletRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WalletService {

    private final WalletRepository walletRepository;
    private final TransactionRepository transactionRepository;
    private final LedgerEntryRepository ledgerEntryRepository;
    private final IdempotencyFingerprintService fingerprintService;
    private final AuthenticationService authenticationService;
    private final ApplicationEventPublisher eventPublisher;

    public WalletService(
            WalletRepository walletRepository,
            TransactionRepository transactionRepository,
            LedgerEntryRepository ledgerEntryRepository,
            IdempotencyFingerprintService fingerprintService,
            AuthenticationService authenticationService,
            ApplicationEventPublisher eventPublisher
    ) {
        this.walletRepository = walletRepository;
        this.transactionRepository = transactionRepository;
        this.ledgerEntryRepository = ledgerEntryRepository;
        this.fingerprintService = fingerprintService;
        this.authenticationService = authenticationService;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Authenticated deposit used by the REST API.
     */
    @Transactional
    public Transaction deposit(
            Long walletId,
            DepositRequest request,
            Authentication authentication
    ) {

        Long currentUserId =
                authenticationService.getCurrentUserId(authentication);

        Wallet wallet = walletRepository
                .findByIdAndUserId(walletId, currentUserId)
                .orElseThrow(() ->
                        new WalletNotFoundException(
                                "Wallet not found"
                        ));

        return createDeposit(wallet, request);
    }

    /**
     * Internal deposit used by application initialization
     * and service-level tests.
     */
    @Transactional
    public Transaction deposit(
            Long walletId,
            DepositRequest request
    ) {

        Wallet wallet = walletRepository
                .findByIdForUpdate(walletId)
                .orElseThrow(() ->
                        new WalletNotFoundException(
                                "Wallet not found"
                        ));

        return createDeposit(wallet, request);
    }

    /**
     * Shared deposit logic.
     */
    private Transaction createDeposit(
            Wallet wallet,
            DepositRequest request
    ) {

        String requestFingerprint =
                fingerprintService.fingerprintDeposit(
                        wallet.getId(),
                        request.amount(),
                        wallet.getCurrency().name()
                );

        var existingTransaction =
                transactionRepository.findByIdempotencyKey(
                        request.idempotencyKey()
                );

        /*
         * Idempotent retry.
         *
         * Do not create another transaction,
         * ledger entry, balance update, or
         * risk-assessment event.
         */
        if (existingTransaction.isPresent()) {

            Transaction existing =
                    existingTransaction.get();

            if (!existing.getRequestFingerprint()
                    .equals(requestFingerprint)) {

                throw new InvalidTransferException(
                        "Idempotency key has already been used for a different request"
                );
            }

            return existing;
        }

        Transaction transaction =
                new Transaction(
                        TransactionType.DEPOSIT,
                        request.amount(),
                        wallet.getCurrency(),
                        request.idempotencyKey(),
                        requestFingerprint
                );

        transactionRepository.save(transaction);

        LedgerEntry creditEntry =
                new LedgerEntry(
                        transaction,
                        wallet,
                        LedgerEntryType.CREDIT,
                        request.amount()
                );

        ledgerEntryRepository.save(creditEntry);

        wallet.setBalance(
                wallet.getBalance().add(request.amount())
        );

        /*
         * Publish the event only for a newly-created deposit.
         *
         * TransactionRiskAssessmentListener handles this
         * event AFTER the deposit transaction commits.
         */
        eventPublisher.publishEvent(
                new TransactionCompletedEvent(
                        transaction.getId(),
                        wallet.getId()
                )
        );

        return transaction;
    }
}