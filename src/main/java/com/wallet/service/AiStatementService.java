package com.wallet.service;

public interface AiStatementService {

    String answerQuestion(
            String statementContext,
            String question
    );
}
