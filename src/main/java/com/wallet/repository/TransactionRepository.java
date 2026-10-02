package com.wallet.repository;


import com.wallet.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface TransactionRepository  extends
        JpaRepository<Transaction, UUID> {
}
