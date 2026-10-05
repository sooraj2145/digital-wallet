package com.wallet.dto;

import com.wallet.entity.Currency;
import com.wallet.entity.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record TransactionResponse(
        UUID id,
        TransactionType type,
        BigDecimal amount,
        Currency currency,
        String idempotencyKey,
        LocalDateTime createdAt
) {
}
