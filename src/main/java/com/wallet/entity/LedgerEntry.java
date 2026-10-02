package com.wallet.entity;


import jakarta.persistence.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name="ledger_entries")
public class LedgerEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "transaction_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_ledger_transaction")
    )
    private Transaction transaction;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "wallet_id",
            nullable = false,
            foreignKey = @ForeignKey(name="fk_ledger_wallet")
    )
    private Wallet wallet;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private LedgerEntryType entryType;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    public LedgerEntry() {}

    public LedgerEntry(Transaction transaction, Wallet wallet, LedgerEntryType entryType, BigDecimal amount) {
        this.transaction = transaction;
        this.wallet = wallet;
        this.entryType = entryType;
        this.amount = amount;
    }

    public UUID getId() {
        return id;
    }

    public Transaction getTransaction() {
        return transaction;
    }

    public Wallet getWallet() {
        return wallet;
    }

    public LedgerEntryType getEntryType() {
        return entryType;
    }

    public BigDecimal getAmount() {
        return amount;
    }
}
