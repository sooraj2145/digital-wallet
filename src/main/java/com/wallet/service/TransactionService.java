package com.wallet.service;


import com.wallet.entity.Transaction;
import com.wallet.exception.WalletNotFoundException;
import com.wallet.repository.TransactionRepository;
import com.wallet.repository.WalletRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final WalletRepository walletRepository;

    public TransactionService(
            TransactionRepository transactionRepository,
            WalletRepository walletRepository
    ) {
        this.transactionRepository = transactionRepository;
        this.walletRepository = walletRepository;
    }

    public Page<Transaction> getTransactions(Pageable pageable) {
        return transactionRepository.findAll(pageable);
    }

    public Page<Transaction> getTransactionsByWallet(
            Long walletId,
            Pageable pageable
    ) {
        if (!walletRepository.existsById(walletId)) {
            throw new WalletNotFoundException(
                    "Wallet not found"
            );
        }
        return transactionRepository.findByWalletId(walletId, pageable);
    }
}
