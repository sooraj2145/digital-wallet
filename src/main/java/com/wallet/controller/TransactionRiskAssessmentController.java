package com.wallet.controller;

import com.wallet.dto.TransactionRiskAssessmentResponse;
import com.wallet.service.TransactionRiskAssessmentService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/transactions")
public class TransactionRiskAssessmentController {

    private final TransactionRiskAssessmentService assessmentService;

    public TransactionRiskAssessmentController(
            TransactionRiskAssessmentService assessmentService
    ) {
        this.assessmentService = assessmentService;
    }

    @GetMapping("/{transactionId}/risk")
    public TransactionRiskAssessmentResponse getRiskAssessment(
            @PathVariable UUID transactionId,
            Authentication authentication
    ) {

        return assessmentService.getAssessment(
                transactionId,
                authentication
        );
    }
}