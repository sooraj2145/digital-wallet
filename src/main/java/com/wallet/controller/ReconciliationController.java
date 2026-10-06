package com.wallet.controller;


import com.wallet.service.ReconciliationResult;
import com.wallet.service.ReconciliationService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reconciliation")
public class ReconciliationController {

    private final ReconciliationService reconciliationService;

    public ReconciliationController(ReconciliationService reconciliationService) {
        this.reconciliationService = reconciliationService;
    }

    @GetMapping("/wallets/{walletId}")
    public ReconciliationResult reconcileWallet(@PathVariable Long walletId)  {
        return reconciliationService.reconcileWallet(walletId);
    }

}
