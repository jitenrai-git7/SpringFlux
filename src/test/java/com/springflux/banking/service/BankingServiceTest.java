package com.springflux.banking.service;

import com.springflux.banking.model.AccountBalance;
import com.springflux.banking.model.KycVerification;
import com.springflux.banking.model.Transaction;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

class BankingServiceTest {

    private MockWebServer mockWebServer;
    private BankingService bankingService;

    @BeforeEach
    void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();

        WebClient webClient = WebClient.builder()
                .baseUrl(mockWebServer.url("/").toString())
                .build();

        bankingService = new BankingService(webClient, new SimpleMeterRegistry());
    }

    @AfterEach
    void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    @Test
    void getAccountBalance_shouldReturnBalance() {
        String responseBody = """
                {
                    "accountId": "ACC-001",
                    "availableBalance": 1500.00,
                    "currentBalance": 1600.00,
                    "currency": "USD",
                    "lastUpdated": "2024-01-15T10:30:00Z"
                }
                """;

        mockWebServer.enqueue(new MockResponse()
                .setBody(responseBody)
                .addHeader("Content-Type", "application/json"));

        Mono<AccountBalance> result = bankingService.getAccountBalance("ACC-001");

        StepVerifier.create(result)
                .assertNext(balance -> {
                    assertThat(balance.getAccountId()).isEqualTo("ACC-001");
                    assertThat(balance.getCurrency()).isEqualTo("USD");
                })
                .verifyComplete();
    }

    @Test
    void getTransactionHistory_shouldReturnTransactions() {
        String responseBody = """
                [
                    {
                        "transactionId": "TXN-001",
                        "accountId": "ACC-001",
                        "type": "DEBIT",
                        "amount": 50.00,
                        "currency": "USD",
                        "description": "Coffee shop",
                        "status": "COMPLETED",
                        "timestamp": "2024-01-15T10:30:00Z"
                    }
                ]
                """;

        mockWebServer.enqueue(new MockResponse()
                .setBody(responseBody)
                .addHeader("Content-Type", "application/json"));

        Flux<Transaction> result = bankingService.getTransactionHistory("ACC-001", 10);

        StepVerifier.create(result)
                .assertNext(tx -> {
                    assertThat(tx.getTransactionId()).isEqualTo("TXN-001");
                    assertThat(tx.getType()).isEqualTo("DEBIT");
                })
                .verifyComplete();
    }

    @Test
    void verifyKyc_shouldReturnKycVerification() {
        String responseBody = """
                {
                    "customerId": "CUST-001",
                    "status": "VERIFIED",
                    "riskLevel": "LOW",
                    "verified": true,
                    "verifiedAt": "2024-01-15T10:30:00Z",
                    "remarks": "All documents verified"
                }
                """;

        mockWebServer.enqueue(new MockResponse()
                .setBody(responseBody)
                .addHeader("Content-Type", "application/json"));

        Mono<KycVerification> result = bankingService.verifyKyc("CUST-001");

        StepVerifier.create(result)
                .assertNext(kyc -> {
                    assertThat(kyc.getCustomerId()).isEqualTo("CUST-001");
                    assertThat(kyc.isVerified()).isTrue();
                    assertThat(kyc.getRiskLevel()).isEqualTo("LOW");
                })
                .verifyComplete();
    }

    @Test
    void getAccountBalance_serverError_shouldPropagate() {
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(500)
                .setBody("{\"error\": \"Internal Server Error\"}")
                .addHeader("Content-Type", "application/json"));

        Mono<AccountBalance> result = bankingService.getAccountBalance("ACC-001");

        StepVerifier.create(result)
                .expectError()
                .verify();
    }

    @Test
    void getAccountBalanceFallback_shouldReturnError() {
        Mono<AccountBalance> result = bankingService.getAccountBalanceFallback(
                "ACC-001", new RuntimeException("connection refused"));

        StepVerifier.create(result)
                .expectError(BankingService.BankingServiceException.class)
                .verify();
    }

    @Test
    void getTransactionHistoryFallback_shouldReturnError() {
        Flux<Transaction> result = bankingService.getTransactionHistoryFallback(
                "ACC-001", 10, new RuntimeException("timeout"));

        StepVerifier.create(result)
                .expectError(BankingService.BankingServiceException.class)
                .verify();
    }

    @Test
    void verifyKycFallback_shouldReturnError() {
        Mono<KycVerification> result = bankingService.verifyKycFallback(
                "CUST-001", new RuntimeException("service down"));

        StepVerifier.create(result)
                .expectError(BankingService.BankingServiceException.class)
                .verify();
    }
}
