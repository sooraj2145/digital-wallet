package com.wallet.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "transaction_risk_assessments",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_risk_assessment_transaction",
                        columnNames = "transaction_id"
                )
        }
)
public class TransactionRiskAssessment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "transaction_id",
            nullable = false,
            foreignKey = @ForeignKey(
                    name = "fk_risk_assessment_transaction"
            )
    )
    private Transaction transaction;

    @Column(nullable = false)
    private int riskScore;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private RiskLevel riskLevel;

    @Column(nullable = false, length = 500)
    private String reason;

    @Column(nullable = false, updatable = false)
    private LocalDateTime assessedAt;

    @PrePersist
    protected void onCreate() {
        assessedAt = LocalDateTime.now();
    }

    public TransactionRiskAssessment() {
    }

    public TransactionRiskAssessment(
            Transaction transaction,
            int riskScore,
            RiskLevel riskLevel,
            String reason
    ) {
        this.transaction = transaction;
        this.riskScore = riskScore;
        this.riskLevel = riskLevel;
        this.reason = reason;
    }

    public UUID getId() {
        return id;
    }

    public Transaction getTransaction() {
        return transaction;
    }

    public int getRiskScore() {
        return riskScore;
    }

    public RiskLevel getRiskLevel() {
        return riskLevel;
    }

    public String getReason() {
        return reason;
    }

    public LocalDateTime getAssessedAt() {
        return assessedAt;
    }
}