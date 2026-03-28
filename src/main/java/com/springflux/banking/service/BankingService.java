package com.springflux.banking.service;

import com.springflux.banking.model.AccountBalance;
import com.springflux.banking.model.KycVerification;
import com.springflux.banking.model.Transaction;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Service
public class BankingService {

    private static final Logger log = LoggerFactory.getLogger(BankingService.class);

    private final WebClient bankingWebClient;
    private final Counter accountBalanceRequests;
    private final Counter transactionHistoryRequests;
    private final Counter kycVerificationRequests;
    private final Counter fallbackCounter;

    public BankingService(WebClient bankingWebClient, MeterRegistry meterRegistry) {
        this.bankingWebClient = bankingWebClient;

        this.accountBalanceRequests = Counter.builder("banking.account.balance.requests")
                .description("Number of account balance requests")
                .register(meterRegistry);
        this.transactionHistoryRequests = Counter.builder("banking.transaction.history.requests")
                .description("Number of transaction history requests")
                .register(meterRegistry);
        this.kycVerificationRequests = Counter.builder("banking.kyc.verification.requests")
                .description("Number of KYC verification requests")
                .register(meterRegistry);
        this.fallbackCounter = Counter.builder("banking.fallback.invocations")
                .description("Number of fallback invocations")
                .register(meterRegistry);
    }

    @CircuitBreaker(name = "accountService", fallbackMethod = "getAccountBalanceFallback")
    @Retry(name = "accountService")
    @TimeLimiter(name = "accountService")
    public Mono<AccountBalance> getAccountBalance(String accountId) {
        log.info("Fetching account balance for accountId={}", accountId);
        accountBalanceRequests.increment();

        return bankingWebClient.get()
                .uri("/accounts/{accountId}/balance", accountId)
                .retrieve()
                .bodyToMono(AccountBalance.class)
                .doOnSuccess(balance -> log.info("Successfully fetched balance for accountId={}", accountId))
                .doOnError(error -> log.error("Error fetching balance for accountId={}: {}",
                        accountId, error.getMessage()));
    }

    @CircuitBreaker(name = "transactionService", fallbackMethod = "getTransactionHistoryFallback")
    @Retry(name = "transactionService")
    public Flux<Transaction> getTransactionHistory(String accountId, int limit) {
        log.info("Fetching transaction history for accountId={}, limit={}", accountId, limit);
        transactionHistoryRequests.increment();

        return bankingWebClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/accounts/{accountId}/transactions")
                        .queryParam("limit", limit)
                        .build(accountId))
                .retrieve()
                .bodyToFlux(Transaction.class)
                .timeout(Duration.ofSeconds(5))
                .doOnComplete(() -> log.info("Successfully fetched transactions for accountId={}", accountId))
                .doOnError(error -> log.error("Error fetching transactions for accountId={}: {}",
                        accountId, error.getMessage()));
    }

    @CircuitBreaker(name = "kycService", fallbackMethod = "verifyKycFallback")
    @Retry(name = "kycService")
    @TimeLimiter(name = "kycService")
    public Mono<KycVerification> verifyKyc(String customerId) {
        log.info("Verifying KYC for customerId={}", customerId);
        kycVerificationRequests.increment();

        return bankingWebClient.get()
                .uri("/customers/{customerId}/kyc", customerId)
                .retrieve()
                .bodyToMono(KycVerification.class)
                .doOnSuccess(kyc -> log.info("Successfully verified KYC for customerId={}", customerId))
                .doOnError(error -> log.error("Error verifying KYC for customerId={}: {}",
                        customerId, error.getMessage()));
    }

    // --- Fallback methods ---

    public Mono<AccountBalance> getAccountBalanceFallback(String accountId, Throwable throwable) {
        log.warn("Fallback triggered for getAccountBalance, accountId={}, reason={}",
                accountId, throwable.getMessage());
        fallbackCounter.increment();
        return handleFallbackError(throwable);
    }

    public Flux<Transaction> getTransactionHistoryFallback(String accountId, int limit, Throwable throwable) {
        log.warn("Fallback triggered for getTransactionHistory, accountId={}, reason={}",
                accountId, throwable.getMessage());
        fallbackCounter.increment();
        return Flux.error(new BankingServiceException(
                "Transaction history service is temporarily unavailable. Please try again later.",
                throwable));
    }

    public Mono<KycVerification> verifyKycFallback(String customerId, Throwable throwable) {
        log.warn("Fallback triggered for verifyKyc, customerId={}, reason={}",
                customerId, throwable.getMessage());
        fallbackCounter.increment();
        return handleFallbackError(throwable);
    }

    private <T> Mono<T> handleFallbackError(Throwable throwable) {
        if (throwable instanceof WebClientResponseException webClientEx) {
            return Mono.error(new BankingServiceException(
                    "Banking service returned error: " + webClientEx.getStatusCode(),
                    throwable));
        }
        return Mono.error(new BankingServiceException(
                "Banking service is temporarily unavailable. Please try again later.",
                throwable));
    }

    public static class BankingServiceException extends RuntimeException {
        public BankingServiceException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
