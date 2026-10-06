package com.wallet.dto;

import com.wallet.entity.RiskLevel;

import java.time.LocalDateTime;
import java.util.UUID;

public record TransactionRiskAssessmentResponse(
        UUID transactionId,
        int riskScore,
        RiskLevel riskLevel,
        String reason,
        LocalDateTime assessedAt
) {
}
