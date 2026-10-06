package com.wallet.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "user_credentials",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_user_credentials_user_id",
                        columnNames = "user_id"
                )
        }
)
public class UserCredential {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "user_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_user_credentials_user")
    )
    private User user;

    @Column(nullable = false)
    private String passwordHash;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public UserCredential() {
    }

    public UserCredential(User user, String passwordHash) {
        this.user = user;
        this.passwordHash = passwordHash;
    }

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}