package com.wallet.controller;


import com.wallet.dto.TransactionResponse;
import com.wallet.dto.TransferRequest;
import com.wallet.entity.Transaction;
import com.wallet.service.TransferService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/transfers")
public class TransferController {

    private final TransferService transferService;

    public TransferController(TransferService transferService) {
        this.transferService = transferService;
    }

    @PostMapping
    public TransactionResponse createTransfer(
            @Valid @RequestBody TransferRequest request
            ) {
        Transaction transaction = transferService.transfer(request);

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
