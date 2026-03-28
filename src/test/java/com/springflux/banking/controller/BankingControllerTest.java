package com.springflux.banking.controller;

import com.springflux.banking.model.AccountBalance;
import com.springflux.banking.model.KycVerification;
import com.springflux.banking.model.Transaction;
import com.springflux.banking.service.BankingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.Instant;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@WebFluxTest(BankingController.class)
class BankingControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private BankingService bankingService;

    @Test
    void getAccountBalance_shouldReturnBalance() {
        AccountBalance balance = new AccountBalance(
                "ACC-001", new BigDecimal("1500.00"), new BigDecimal("1600.00"),
                "USD", Instant.now());

        when(bankingService.getAccountBalance("ACC-001")).thenReturn(Mono.just(balance));

        webTestClient.get()
                .uri("/api/v1/banking/accounts/ACC-001/balance")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.accountId").isEqualTo("ACC-001")
                .jsonPath("$.availableBalance").isEqualTo(1500.00)
                .jsonPath("$.currency").isEqualTo("USD");
    }

    @Test
    void getTransactionHistory_shouldReturnTransactions() {
        Transaction tx = new Transaction(
                "TXN-001", "ACC-001", "DEBIT", new BigDecimal("50.00"),
                "USD", "Coffee shop", "COMPLETED", Instant.now());

        when(bankingService.getTransactionHistory(anyString(), anyInt()))
                .thenReturn(Flux.just(tx));

        webTestClient.get()
                .uri("/api/v1/banking/accounts/ACC-001/transactions?limit=10")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$[0].transactionId").isEqualTo("TXN-001")
                .jsonPath("$[0].type").isEqualTo("DEBIT")
                .jsonPath("$[0].amount").isEqualTo(50.00);
    }

    @Test
    void verifyKyc_shouldReturnKycStatus() {
        KycVerification kyc = new KycVerification(
                "CUST-001", "VERIFIED", "LOW", true, Instant.now(), "All documents verified");

        when(bankingService.verifyKyc("CUST-001")).thenReturn(Mono.just(kyc));

        webTestClient.get()
                .uri("/api/v1/banking/customers/CUST-001/kyc")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.customerId").isEqualTo("CUST-001")
                .jsonPath("$.status").isEqualTo("VERIFIED")
                .jsonPath("$.verified").isEqualTo(true);
    }

    @Test
    void getAccountBalance_whenServiceError_shouldReturnServiceUnavailable() {
        when(bankingService.getAccountBalance("ACC-999"))
                .thenReturn(Mono.error(new BankingService.BankingServiceException(
                        "Service unavailable", new RuntimeException("connection refused"))));

        webTestClient.get()
                .uri("/api/v1/banking/accounts/ACC-999/balance")
                .exchange()
                .expectStatus().is5xxServerError();
    }

    @Test
    void getTransactionHistory_defaultLimit_shouldUse50() {
        when(bankingService.getTransactionHistory("ACC-001", 50))
                .thenReturn(Flux.empty());

        webTestClient.get()
                .uri("/api/v1/banking/accounts/ACC-001/transactions")
                .exchange()
                .expectStatus().isOk();
    }
}
