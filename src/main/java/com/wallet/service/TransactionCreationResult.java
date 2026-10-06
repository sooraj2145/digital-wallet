package com.wallet.service;

import com.wallet.entity.Transaction;

public record TransactionCreationResult(
        Transaction transaction,
        boolean created
) {
}
