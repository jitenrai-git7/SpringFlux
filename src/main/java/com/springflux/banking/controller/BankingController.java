package com.springflux.banking.controller;

import com.springflux.banking.model.AccountBalance;
import com.springflux.banking.model.KycVerification;
import com.springflux.banking.model.Transaction;
import com.springflux.banking.service.BankingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1/banking")
public class BankingController {

    private static final Logger log = LoggerFactory.getLogger(BankingController.class);

    private final BankingService bankingService;

    public BankingController(BankingService bankingService) {
        this.bankingService = bankingService;
    }

    @GetMapping("/accounts/{accountId}/balance")
    public Mono<AccountBalance> getAccountBalance(@PathVariable String accountId) {
        log.info("REST request to get account balance for accountId={}", accountId);
        return bankingService.getAccountBalance(accountId);
    }

    @GetMapping("/accounts/{accountId}/transactions")
    public Flux<Transaction> getTransactionHistory(
            @PathVariable String accountId,
            @RequestParam(defaultValue = "50") int limit) {
        log.info("REST request to get transaction history for accountId={}, limit={}", accountId, limit);
        return bankingService.getTransactionHistory(accountId, limit);
    }

    @GetMapping("/customers/{customerId}/kyc")
    public Mono<KycVerification> verifyKyc(@PathVariable String customerId) {
        log.info("REST request to verify KYC for customerId={}", customerId);
        return bankingService.verifyKyc(customerId);
    }
}
