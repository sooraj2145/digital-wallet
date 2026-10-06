package com.wallet.service;

import com.wallet.dto.StatementAnswerResponse;
import com.wallet.dto.StatementQuestionRequest;
import com.wallet.entity.Wallet;
import com.wallet.repository.WalletRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
public class StatementQaService {

    private final AuthenticationService authenticationService;
    private final WalletRepository walletRepository;
    private final StatementAnalysisService statementAnalysisService;
    private final StatementContextBuilder statementContextBuilder;
    private final AiStatementService aiStatementService;


    public StatementQaService(
            AuthenticationService authenticationService,
            WalletRepository walletRepository,
            StatementAnalysisService statementAnalysisService,
            StatementContextBuilder statementContextBuilder,
            AiStatementService aiStatementService
    ) {
        this.authenticationService = authenticationService;
        this.walletRepository = walletRepository;
        this.statementAnalysisService = statementAnalysisService;
        this.statementContextBuilder = statementContextBuilder;
        this.aiStatementService = aiStatementService;
    }

    public StatementAnswerResponse answer(
            Long walletId,
            StatementQuestionRequest request,
            Authentication authentication
    ) {
        Long currentUserId =
                authenticationService.getCurrentUserId(authentication);

        Wallet wallet =
                walletRepository
                        .findByIdAndUserId(walletId, currentUserId)
                        .orElseThrow(() ->
                                new RuntimeException("Wallet not found"));

        String statementContext = statementContextBuilder.build(wallet);

        String answer =
                aiStatementService.answerQuestion(
                        statementContext,
                        request.question()
                );

        return new StatementAnswerResponse(answer);
    }
}