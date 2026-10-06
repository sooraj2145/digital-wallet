package com.wallet.service;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(
        name = "ai.provider",
        havingValue = "local"
)
public class LocalAiStatementService implements AiStatementService {

    @Override
    public String answerQuestion(
            String statementContext,
            String question
    ) {
        String normalizedQuestion =
                question.toLowerCase().trim();

        if (normalizedQuestion.contains("how many")
                || normalizedQuestion.contains("number of transactions")
                || normalizedQuestion.contains("transaction count")) {

            return extractLine(
                    statementContext,
                    "Transaction count:"
            );
        }

        if (normalizedQuestion.contains("total deposit")
                || normalizedQuestion.contains("how much deposited")
                || normalizedQuestion.contains("deposits")) {

            return extractLine(
                    statementContext,
                    "Total deposits:"
            );
        }

        if (normalizedQuestion.contains("outgoing transfer")
                || normalizedQuestion.contains("total transfer")
                || normalizedQuestion.contains("transferred")) {

            return extractLine(
                    statementContext,
                    "Total outgoing transfers:"
            );
        }

        return "I could not determine the answer from your statement.";
    }

    private String extractLine(
            String context,
            String prefix
    ) {
        return context.lines()
                .filter(line -> line.startsWith(prefix))
                .findFirst()
                .map(line -> line.substring(prefix.length()).trim())
                .map(value -> prefix + " " + value)
                .orElse(
                        "The requested information is not available."
                );
    }
}