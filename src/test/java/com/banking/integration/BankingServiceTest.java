package com.banking.integration.service;

import com.banking.integration.model.AccountBalance;
import com.banking.integration.model.KycStatus;
import com.banking.integration.model.Transaction;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.Duration;

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

        MeterRegistry meterRegistry = new SimpleMeterRegistry();

        // Use retry config that only retries on transient errors (not business exceptions)
        RetryConfig retryConfig = RetryConfig.custom()
                .maxAttempts(3)
                .waitDuration(Duration.ofMillis(100))
                .retryExceptions(java.io.IOException.class)
                .build();

        // Use a generous time limiter for unit tests
        TimeLimiterConfig timeLimiterConfig = TimeLimiterConfig.custom()
                .timeoutDuration(Duration.ofSeconds(10))
                .build();

        bankingService = new BankingService(
                webClient,
                CircuitBreakerRegistry.ofDefaults(),
                RetryRegistry.of(retryConfig),
                TimeLimiterRegistry.of(timeLimiterConfig),
                meterRegistry
        );
    }

    @AfterEach
    void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    @Test
    void getAccountBalance_successfulResponse_returnsBalance() {
        String responseBody = """
                {
                    "accountId": "ACC001",
                    "accountNumber": "1234567890",
                    "accountType": "SAVINGS",
                    "currency": "USD",
                    "availableBalance": 5000.00,
                    "currentBalance": 5200.00,
                    "holdAmount": 200.00
                }
                """;

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setBody(responseBody));

        Mono<AccountBalance> result = bankingService.getAccountBalance("ACC001");

        StepVerifier.create(result)
                .expectNextMatches(balance ->
                        "ACC001".equals(balance.getAccountId()) &&
                        new BigDecimal("5000.00").compareTo(balance.getAvailableBalance()) == 0)
                .verifyComplete();
    }

    @Test
    void getAccountBalance_notFound_returnsBankingServiceException() {
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(404)
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setBody("{\"error\": \"Not Found\"}"));

        Mono<AccountBalance> result = bankingService.getAccountBalance("UNKNOWN");

        StepVerifier.create(result)
                .expectErrorMatches(error ->
                        error instanceof BankingService.BankingServiceException &&
                        "ACCOUNT_NOT_FOUND".equals(((BankingService.BankingServiceException) error).getErrorCode()))
                .verify();
    }

    @Test
    void getTransactionHistory_successfulResponse_returnsTransactions() {
        String responseBody = """
                [
                    {
                        "transactionId": "TXN001",
                        "accountId": "ACC001",
                        "type": "CREDIT",
                        "amount": 1000.00,
                        "currency": "USD",
                        "status": "COMPLETED"
                    },
                    {
                        "transactionId": "TXN002",
                        "accountId": "ACC001",
                        "type": "DEBIT",
                        "amount": 250.00,
                        "currency": "USD",
                        "status": "COMPLETED"
                    }
                ]
                """;

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setBody(responseBody));

        Flux<Transaction> result = bankingService.getTransactionHistory("ACC001", 0, 20);

        StepVerifier.create(result)
                .expectNextMatches(t -> "TXN001".equals(t.getTransactionId()))
                .expectNextMatches(t -> "TXN002".equals(t.getTransactionId()))
                .verifyComplete();
    }

    @Test
    void getKycStatus_successfulResponse_returnsKycStatus() {
        String responseBody = """
                {
                    "customerId": "CUST001",
                    "fullName": "Jane Doe",
                    "nationality": "US",
                    "verificationStatus": "VERIFIED",
                    "riskLevel": "LOW"
                }
                """;

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setBody(responseBody));

        Mono<KycStatus> result = bankingService.getKycStatus("CUST001");

        StepVerifier.create(result)
                .expectNextMatches(kyc ->
                        "CUST001".equals(kyc.getCustomerId()) &&
                        KycStatus.VerificationStatus.VERIFIED == kyc.getVerificationStatus())
                .verifyComplete();
    }

    @Test
    void getKycStatus_customerNotFound_returnsBankingServiceException() {
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(404)
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setBody("{\"error\": \"Not Found\"}"));

        Mono<KycStatus> result = bankingService.getKycStatus("UNKNOWN");

        StepVerifier.create(result)
                .expectErrorMatches(error ->
                        error instanceof BankingService.BankingServiceException &&
                        "CUSTOMER_NOT_FOUND".equals(((BankingService.BankingServiceException) error).getErrorCode()))
                .verify();
    }

    @Test
    void getAccountBalance_serverError_returnsBankingServiceException() {
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(500)
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setBody("{\"error\": \"Internal Server Error\"}"));

        Mono<AccountBalance> result = bankingService.getAccountBalance("ACC001");

        StepVerifier.create(result)
                .expectErrorMatches(error ->
                        error instanceof BankingService.BankingServiceException &&
                        "UPSTREAM_ERROR".equals(((BankingService.BankingServiceException) error).getErrorCode()))
                .verify();
    }
}
