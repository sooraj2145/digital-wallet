package com.wallet.controller;


import com.wallet.dto.TransactionResponse;
import com.wallet.dto.WalletTransactionResponse;
import com.wallet.service.TransactionService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @GetMapping
    public Page<TransactionResponse> getTransactions(Pageable pageable) {

        return transactionService
                .getTransactions(pageable)
                .map(transaction -> new TransactionResponse(
                        transaction.getId(),
                        transaction.getType(),
                        transaction.getAmount(),
                        transaction.getCurrency(),
                        transaction.getIdempotencyKey(),
                        transaction.getCreatedAt()
                )
                );
    }

    @GetMapping("/wallet/{walletId}")
    public Page<TransactionResponse> getTransactionsByWallet(
            @PathVariable Long walletId,
            Pageable pageable
    ) {
        return transactionService
                .getTransactionsByWallet(walletId, pageable)
                .map(transaction -> new TransactionResponse(
                        transaction.getId(),
                        transaction.getType(),
                        transaction.getAmount(),
                        transaction.getCurrency(),
                        transaction.getIdempotencyKey(),
                        transaction.getCreatedAt()
                )
                );
    }

    @GetMapping("/wallet/{walletId}/statement")
    public Page<WalletTransactionResponse> getWalletStatement(
            @PathVariable Long walletId,
            Pageable pageable
    ) {
        return transactionService.getWalletStatement(
                walletId,
                pageable
        );
    }
}
