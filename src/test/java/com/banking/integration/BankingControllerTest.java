package com.banking.integration;

import com.banking.integration.model.AccountBalance;
import com.banking.integration.model.KycStatus;
import com.banking.integration.model.Transaction;
import com.banking.integration.service.BankingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@WebFluxTest
class BankingControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private BankingService bankingService;

    @Test
    void getAccountBalance_returnsOk() {
        AccountBalance balance = AccountBalance.builder()
                .accountId("ACC001")
                .accountNumber("1234567890")
                .currency("USD")
                .availableBalance(new BigDecimal("5000.00"))
                .currentBalance(new BigDecimal("5200.00"))
                .holdAmount(new BigDecimal("200.00"))
                .lastUpdated(Instant.now())
                .build();

        when(bankingService.getAccountBalance("ACC001")).thenReturn(Mono.just(balance));

        webTestClient.get()
                .uri("/api/v1/banking/accounts/ACC001/balance")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBody(AccountBalance.class)
                .value(b -> {
                    assert "ACC001".equals(b.getAccountId());
                    assert new BigDecimal("5000.00").compareTo(b.getAvailableBalance()) == 0;
                });
    }

    @Test
    void getAccountBalance_notFound_returns404() {
        when(bankingService.getAccountBalance("UNKNOWN")).thenReturn(Mono.empty());

        webTestClient.get()
                .uri("/api/v1/banking/accounts/UNKNOWN/balance")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void getTransactionHistory_returnsOk() {
        Transaction tx1 = Transaction.builder()
                .transactionId("TXN001")
                .accountId("ACC001")
                .type(Transaction.TransactionType.CREDIT)
                .amount(new BigDecimal("1000.00"))
                .currency("USD")
                .status(Transaction.TransactionStatus.COMPLETED)
                .build();

        Transaction tx2 = Transaction.builder()
                .transactionId("TXN002")
                .accountId("ACC001")
                .type(Transaction.TransactionType.DEBIT)
                .amount(new BigDecimal("250.00"))
                .currency("USD")
                .status(Transaction.TransactionStatus.COMPLETED)
                .build();

        when(bankingService.getTransactionHistory(anyString(), anyInt(), anyInt()))
                .thenReturn(Flux.just(tx1, tx2));

        webTestClient.get()
                .uri("/api/v1/banking/accounts/ACC001/transactions?page=0&size=20")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(Transaction.class)
                .hasSize(2);
    }

    @Test
    void getKycStatus_returnsOk() {
        KycStatus kyc = KycStatus.builder()
                .customerId("CUST001")
                .fullName("Jane Doe")
                .nationality("US")
                .verificationStatus(KycStatus.VerificationStatus.VERIFIED)
                .riskLevel(KycStatus.RiskLevel.LOW)
                .build();

        when(bankingService.getKycStatus("CUST001")).thenReturn(Mono.just(kyc));

        webTestClient.get()
                .uri("/api/v1/banking/customers/CUST001/kyc")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBody(KycStatus.class)
                .value(k -> {
                    assert "CUST001".equals(k.getCustomerId());
                    assert KycStatus.VerificationStatus.VERIFIED == k.getVerificationStatus();
                });
    }

    @Test
    void getKycStatus_notFound_returns404() {
        when(bankingService.getKycStatus("UNKNOWN")).thenReturn(Mono.empty());

        webTestClient.get()
                .uri("/api/v1/banking/customers/UNKNOWN/kyc")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void getAccountBalance_circuitBreakerOpen_returnsServiceUnavailable() {
        when(bankingService.getAccountBalance("ACC001"))
                .thenReturn(Mono.error(
                        io.github.resilience4j.circuitbreaker.CallNotPermittedException
                                .createCallNotPermittedException(
                                        io.github.resilience4j.circuitbreaker.CircuitBreaker.ofDefaults("test"))));

        webTestClient.get()
                .uri("/api/v1/banking/accounts/ACC001/balance")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isEqualTo(503);
    }
}
