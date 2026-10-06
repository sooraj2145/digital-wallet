package com.wallet.service;

import com.wallet.entity.Currency;

import java.math.BigDecimal;

public record ReconciliationResult(
        Long walletId,
        Currency currency,
        BigDecimal walletBalance,
        BigDecimal ledgerBalance,
        BigDecimal difference,
        ReconciliationStatus status
) {
}
