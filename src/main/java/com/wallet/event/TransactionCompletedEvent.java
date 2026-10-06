package com.wallet.event;

import java.util.UUID;

public record TransactionCompletedEvent(
        UUID transactionId,
        Long walletId
) {
}