package com.wallet.controller;

import com.wallet.dto.DepositRequest;
import com.wallet.dto.TransactionResponse;
import com.wallet.entity.Transaction;
import com.wallet.service.WalletService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/wallets")
public class WalletController {

    private final WalletService walletService;

    public WalletController(WalletService walletService) {
        this.walletService = walletService;
    }

    @PostMapping("/{walletId}/deposit")
    @ResponseStatus(HttpStatus.CREATED)
    public TransactionResponse deposit(
            @PathVariable Long walletId,
            @Valid @RequestBody DepositRequest request,
            Authentication authentication
    ) {
        Transaction transaction =
                walletService.deposit(
                        walletId,
                        request,
                        authentication
                );

        return new TransactionResponse(
                transaction.getId(),
                transaction.getType(),
                transaction.getAmount(),
                transaction.getCurrency(),
                transaction.getIdempotencyKey(),
                transaction.getCreatedAt()
        );
    }
}