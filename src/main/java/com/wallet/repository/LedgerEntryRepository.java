package com.wallet.repository;

import com.wallet.entity.LedgerEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface LedgerEntryRepository
        extends JpaRepository<LedgerEntry, UUID> {

    List<LedgerEntry> findByTransactionId(UUID transactionId);

    List<LedgerEntry> findByWalletId(Long walletId);


}
