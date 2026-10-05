package com.wallet.dto;

import com.wallet.entity.Currency;
import com.wallet.entity.LedgerEntryType;
import com.wallet.entity.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record WalletTransactionResponse(
        UUID transactionId,
        TransactionType type,
        LedgerEntryType entryType,
        BigDecimal amount,
        BigDecimal balanceAfter,
        Currency currency,
        String idempotencyKey,
        LocalDateTime createdAt
) {
}
