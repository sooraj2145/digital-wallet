package com.wallet.controller;

import com.wallet.dto.StatementAnswerResponse;
import com.wallet.dto.StatementQuestionRequest;
import com.wallet.service.StatementQaService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/statement")
public class StatementQaController {

    private final StatementQaService statementQaService;

    public StatementQaController(
            StatementQaService statementQaService
    ) {
        this.statementQaService = statementQaService;
    }

    @PostMapping("/wallet/{walletId}/ask")
    public StatementAnswerResponse ask(
            @PathVariable Long walletId,
            @Valid @RequestBody StatementQuestionRequest request,
            Authentication authentication
    ) {
        return statementQaService.answer(
                walletId,
                request,
                authentication
        );
    }
}