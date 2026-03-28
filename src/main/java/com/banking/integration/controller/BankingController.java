package com.banking.integration.controller;

import com.banking.integration.model.AccountBalance;
import com.banking.integration.model.KycStatus;
import com.banking.integration.model.Transaction;
import com.banking.integration.service.BankingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/banking")
public class BankingController {

    private final BankingService bankingService;

    /**
     * GET /api/v1/banking/accounts/{accountId}/balance
     * Returns the current account balance for the specified account.
     */
    @GetMapping("/accounts/{accountId}/balance")
    public Mono<ResponseEntity<AccountBalance>> getAccountBalance(
            @PathVariable String accountId) {
        log.info("REST GET /accounts/{}/balance", accountId);
        return bankingService.getAccountBalance(accountId)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    /**
     * GET /api/v1/banking/accounts/{accountId}/transactions
     * Returns paginated transaction history for the specified account.
     * Streams results as an application/json Flux.
     */
    @GetMapping(value = "/accounts/{accountId}/transactions",
                produces = MediaType.APPLICATION_JSON_VALUE)
    public Flux<Transaction> getTransactionHistory(
            @PathVariable String accountId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.info("REST GET /accounts/{}/transactions?page={}&size={}", accountId, page, size);
        return bankingService.getTransactionHistory(accountId, page, size);
    }

    /**
     * GET /api/v1/banking/customers/{customerId}/kyc
     * Returns the KYC verification status for the specified customer.
     */
    @GetMapping("/customers/{customerId}/kyc")
    public Mono<ResponseEntity<KycStatus>> getKycStatus(
            @PathVariable String customerId) {
        log.info("REST GET /customers/{}/kyc", customerId);
        return bankingService.getKycStatus(customerId)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }
}
