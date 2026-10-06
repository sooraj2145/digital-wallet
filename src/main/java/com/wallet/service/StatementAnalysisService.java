package com.wallet.service;

import com.wallet.entity.LedgerEntry;
import com.wallet.entity.LedgerEntryType;
import com.wallet.entity.Transaction;
import com.wallet.repository.LedgerEntryRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;

@Service
public class StatementAnalysisService {

    private final LedgerEntryRepository ledgerEntryRepository;

    public StatementAnalysisService(
            LedgerEntryRepository ledgerEntryRepository
    ) {
        this.ledgerEntryRepository = ledgerEntryRepository;
    }

    public String analyze(
            Long walletId,
            String question
    ) {
        List<LedgerEntry> entries =
                ledgerEntryRepository
                        .findAllByWalletIdWithTransaction(walletId);

        if (entries.isEmpty()) {
            return "You do not have any transactions yet.";
        }

        String normalizedQuestion =
                question.toLowerCase().trim();

        if (normalizedQuestion.contains("how many")
                || normalizedQuestion.contains("number of transactions")
                || normalizedQuestion.contains("transaction count")) {

            return "You have made "
                    + entries.size()
                    + " transactions.";
        }

        if (normalizedQuestion.contains("largest")
                || normalizedQuestion.contains("biggest")
                || normalizedQuestion.contains("highest")) {

            LedgerEntry largest =
                    entries.stream()
                            .max(Comparator.comparing(
                                    LedgerEntry::getAmount
                            ))
                            .orElseThrow();

            return "Your largest transaction was "
                    + largest.getAmount()
                    + " "
                    + largest.getTransaction()
                    .getCurrency()
                    + ".";
        }

        if (normalizedQuestion.contains("deposit")) {

            BigDecimal totalDeposits =
                    entries.stream()
                            .filter(entry ->
                                    entry.getTransaction()
                                            .getType()
                                            .name()
                                            .equals("DEPOSIT")
                            )
                            .map(LedgerEntry::getAmount)
                            .reduce(
                                    BigDecimal.ZERO,
                                    BigDecimal::add
                            );

            return "Your total deposits are "
                    + totalDeposits
                    + ".";
        }

        if (normalizedQuestion.contains("transfer")) {

            BigDecimal totalTransfers =
                    entries.stream()
                            .filter(entry ->
                                    entry.getTransaction()
                                            .getType()
                                            .name()
                                            .equals("TRANSFER")
                            )
                            .filter(entry ->
                                    entry.getEntryType()
                                            == LedgerEntryType.DEBIT
                            )
                            .map(LedgerEntry::getAmount)
                            .reduce(
                                    BigDecimal.ZERO,
                                    BigDecimal::add
                            );

            return "Your total outgoing transfers are "
                    + totalTransfers
                    + ".";
        }

        if (normalizedQuestion.contains("latest")
                || normalizedQuestion.contains("last")
                || normalizedQuestion.contains("recent")) {

            LedgerEntry latest =
                    entries.stream()
                            .max(Comparator.comparing(
                                    entry ->
                                            entry.getTransaction()
                                                    .getCreatedAt()
                            ))
                            .orElseThrow();

            Transaction transaction =
                    latest.getTransaction();

            return "Your latest transaction was a "
                    + transaction.getType()
                    + " of "
                    + transaction.getAmount()
                    + " "
                    + transaction.getCurrency()
                    + ".";
        }

        return "I could not determine the answer from your statement.";
    }
}