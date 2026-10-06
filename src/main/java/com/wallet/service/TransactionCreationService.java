package com.wallet.service;

import com.wallet.entity.Currency;
import com.wallet.entity.Transaction;
import com.wallet.entity.TransactionType;
import com.wallet.exception.InvalidTransferException;
import com.wallet.repository.TransactionRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;

@Service
public class TransactionCreationService {

    private final TransactionRepository transactionRepository;

    public TransactionCreationService(
            TransactionRepository transactionRepository
    ) {
        this.transactionRepository = transactionRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public TransactionCreationResult create(
            BigDecimal amount,
            Currency currency,
            String idempotencyKey,
            String requestFingerprint
    ) {

        Optional<Transaction> existing =
                transactionRepository.findByIdempotencyKey(
                        idempotencyKey
                );


        if (existing.isPresent()) {

            if (!existing.get()
                    .getRequestFingerprint()
                    .equals(requestFingerprint)) {

                throw new InvalidTransferException(
                        "Idempotency key has already been used for a different request"
                );
            }

            return new TransactionCreationResult(
                    existing.get(),
                    false
            );
        }

        Transaction transaction = new Transaction(
                TransactionType.TRANSFER,
                amount,
                currency,
                idempotencyKey,
                requestFingerprint
        );

        try {
            Transaction saved =
                    transactionRepository.saveAndFlush(transaction);

            return new TransactionCreationResult(
                    saved,
                    true
            );

        } catch (DataIntegrityViolationException exception) {

            Transaction existingTransaction =
                    transactionRepository
                            .findByIdempotencyKey(idempotencyKey)
                            .orElseThrow(() ->
                                    new IllegalStateException(
                                            "Transaction was created concurrently " +
                                                    "but could not be retrieved"
                                    )
                            );

            if (!existingTransaction
                    .getRequestFingerprint()
                    .equals(requestFingerprint)) {

                throw new InvalidTransferException(
                        "Idempotency key has already been used for a different request"
                );
            }

            return new TransactionCreationResult(
                    existingTransaction,
                    false
            );
        }
    }
}