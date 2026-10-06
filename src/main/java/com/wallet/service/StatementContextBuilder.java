package com.wallet.service;

import com.wallet.entity.LedgerEntry;
import com.wallet.entity.LedgerEntryType;
import com.wallet.entity.TransactionType;
import com.wallet.entity.Wallet;
import com.wallet.repository.LedgerEntryRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class StatementContextBuilder {

    private final LedgerEntryRepository ledgerEntryRepository;

    public StatementContextBuilder(
            LedgerEntryRepository ledgerEntryRepository
    ) {
        this.ledgerEntryRepository = ledgerEntryRepository;
    }

    public String build(Wallet wallet) {

        List<LedgerEntry> entries =
                ledgerEntryRepository
                        .findAllByWalletIdWithTransaction(wallet.getId());

        if (entries.isEmpty()) {
            return "The wallet has no transactions.";
        }

        BigDecimal totalDeposits = BigDecimal.ZERO;
        BigDecimal totalOutgoingTransfers = BigDecimal.ZERO;

        StringBuilder context = new StringBuilder();

        context.append("Wallet statement\n");
        context.append("Currency: ")
                .append(wallet.getCurrency())
                .append("\n\n");

        for (LedgerEntry entry : entries) {

            context.append("Transaction ID: ")
                    .append(entry.getTransaction().getId())
                    .append("\n");

            context.append("Type: ")
                    .append(entry.getTransaction().getType())
                    .append("\n");

            context.append("Entry type: ")
                    .append(entry.getEntryType())
                    .append("\n");

            context.append("Amount: ")
                    .append(entry.getAmount())
                    .append(" ")
                    .append(wallet.getCurrency())
                    .append("\n");

            context.append("Date: ")
                    .append(entry.getTransaction().getCreatedAt())
                    .append("\n\n");

            if (entry.getTransaction().getType()
                    == TransactionType.DEPOSIT) {

                totalDeposits =
                        totalDeposits.add(entry.getAmount());
            }

            if (entry.getTransaction().getType()
                    == TransactionType.TRANSFER
                    && entry.getEntryType()
                    == LedgerEntryType.DEBIT) {

                totalOutgoingTransfers =
                        totalOutgoingTransfers.add(entry.getAmount());
            }
        }

        context.append("Statement summary\n");
        context.append("Transaction count: ")
                .append(entries.size())
                .append("\n");

        context.append("Total deposits: ")
                .append(totalDeposits)
                .append(" ")
                .append(wallet.getCurrency())
                .append("\n");

        context.append("Total outgoing transfers: ")
                .append(totalOutgoingTransfers)
                .append(" ")
                .append(wallet.getCurrency())
                .append("\n");

        return context.toString();
    }
}