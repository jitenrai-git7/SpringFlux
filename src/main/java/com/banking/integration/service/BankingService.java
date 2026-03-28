package com.banking.integration.service;

import com.banking.integration.config.Resilience4jConfig;
import com.banking.integration.model.AccountBalance;
import com.banking.integration.model.KycStatus;
import com.banking.integration.model.Transaction;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator;
import io.github.resilience4j.reactor.retry.RetryOperator;
import io.github.resilience4j.reactor.timelimiter.TimeLimiterOperator;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import io.github.resilience4j.timelimiter.TimeLimiter;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;

@Slf4j
@Service
public class BankingService {

    private final WebClient webClient;
    private final CircuitBreaker circuitBreaker;
    private final Retry retry;
    private final TimeLimiter timeLimiter;
    private final MeterRegistry meterRegistry;

    public BankingService(WebClient bankingWebClient,
                          CircuitBreakerRegistry circuitBreakerRegistry,
                          RetryRegistry retryRegistry,
                          TimeLimiterRegistry timeLimiterRegistry,
                          MeterRegistry meterRegistry) {
        this.webClient = bankingWebClient;
        this.circuitBreaker = circuitBreakerRegistry.circuitBreaker(Resilience4jConfig.BANKING_SERVICE);
        this.retry = retryRegistry.retry(Resilience4jConfig.BANKING_SERVICE);
        this.timeLimiter = timeLimiterRegistry.timeLimiter(Resilience4jConfig.BANKING_SERVICE);
        this.meterRegistry = meterRegistry;
    }

    /**
     * Retrieves the account balance for a given account ID.
     * Applies circuit breaker, retry, and time limiter resilience patterns.
     */
    public Mono<AccountBalance> getAccountBalance(String accountId) {
        log.debug("Fetching account balance for accountId={}", accountId);
        Timer.Sample sample = Timer.start(meterRegistry);

        return webClient.get()
                .uri("/accounts/{accountId}/balance", accountId)
                .retrieve()
                .onStatus(HttpStatus.NOT_FOUND::equals,
                        response -> Mono.error(new BankingServiceException(
                                "ACCOUNT_NOT_FOUND", "Account not found: " + accountId, 404)))
                .onStatus(HttpStatus.UNAUTHORIZED::equals,
                        response -> Mono.error(new BankingServiceException(
                                "UNAUTHORIZED", "Unauthorized access", 401)))
                .onStatus(status -> status.is5xxServerError(),
                        response -> response.bodyToMono(String.class)
                                .flatMap(body -> Mono.error(new BankingServiceException(
                                        "UPSTREAM_ERROR", "Banking API error: " + body, 502))))
                .bodyToMono(AccountBalance.class)
                .transformDeferred(TimeLimiterOperator.of(timeLimiter))
                .transformDeferred(CircuitBreakerOperator.of(circuitBreaker))
                .transformDeferred(RetryOperator.of(retry))
                .doOnSuccess(balance -> {
                    sample.stop(meterRegistry.timer("banking.account.balance.fetch",
                            "status", "success"));
                    log.info("Successfully fetched balance for accountId={}, availableBalance={}",
                            accountId, balance != null ? balance.getAvailableBalance() : null);
                })
                .doOnError(throwable -> {
                    sample.stop(meterRegistry.timer("banking.account.balance.fetch",
                            "status", "error"));
                    log.error("Failed to fetch balance for accountId={}: {}", accountId, throwable.getMessage());
                });
    }

    /**
     * Retrieves the transaction history for a given account.
     * Supports pagination via page and size parameters.
     * Applies circuit breaker, retry, and time limiter resilience patterns.
     */
    public Flux<Transaction> getTransactionHistory(String accountId, int page, int size) {
        log.debug("Fetching transaction history for accountId={}, page={}, size={}", accountId, page, size);
        Instant start = Instant.now();

        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/accounts/{accountId}/transactions")
                        .queryParam("page", page)
                        .queryParam("size", size)
                        .build(accountId))
                .retrieve()
                .onStatus(HttpStatus.NOT_FOUND::equals,
                        response -> Mono.error(new BankingServiceException(
                                "ACCOUNT_NOT_FOUND", "Account not found: " + accountId, 404)))
                .onStatus(HttpStatus.UNAUTHORIZED::equals,
                        response -> Mono.error(new BankingServiceException(
                                "UNAUTHORIZED", "Unauthorized access", 401)))
                .onStatus(status -> status.is5xxServerError(),
                        response -> response.bodyToMono(String.class)
                                .flatMap(body -> Mono.error(new BankingServiceException(
                                        "UPSTREAM_ERROR", "Banking API error: " + body, 502))))
                .bodyToFlux(Transaction.class)
                .transformDeferred(TimeLimiterOperator.of(timeLimiter))
                .transformDeferred(CircuitBreakerOperator.of(circuitBreaker))
                .transformDeferred(RetryOperator.of(retry))
                .doOnComplete(() -> {
                    long elapsed = Duration.between(start, Instant.now()).toMillis();
                    meterRegistry.timer("banking.transactions.fetch", "status", "success")
                            .record(Duration.ofMillis(elapsed));
                    log.info("Successfully fetched transactions for accountId={} in {}ms", accountId, elapsed);
                })
                .doOnError(throwable -> {
                    meterRegistry.timer("banking.transactions.fetch", "status", "error")
                            .record(Duration.between(start, Instant.now()));
                    log.error("Failed to fetch transactions for accountId={}: {}", accountId, throwable.getMessage());
                });
    }

    /**
     * Retrieves the KYC verification status for a given customer.
     * Applies circuit breaker, retry, and time limiter resilience patterns.
     */
    public Mono<KycStatus> getKycStatus(String customerId) {
        log.debug("Fetching KYC status for customerId={}", customerId);
        Timer.Sample sample = Timer.start(meterRegistry);

        return webClient.get()
                .uri("/customers/{customerId}/kyc", customerId)
                .retrieve()
                .onStatus(HttpStatus.NOT_FOUND::equals,
                        response -> Mono.error(new BankingServiceException(
                                "CUSTOMER_NOT_FOUND", "Customer not found: " + customerId, 404)))
                .onStatus(HttpStatus.UNAUTHORIZED::equals,
                        response -> Mono.error(new BankingServiceException(
                                "UNAUTHORIZED", "Unauthorized access", 401)))
                .onStatus(status -> status.is5xxServerError(),
                        response -> response.bodyToMono(String.class)
                                .flatMap(body -> Mono.error(new BankingServiceException(
                                        "UPSTREAM_ERROR", "Banking API error: " + body, 502))))
                .bodyToMono(KycStatus.class)
                .transformDeferred(TimeLimiterOperator.of(timeLimiter))
                .transformDeferred(CircuitBreakerOperator.of(circuitBreaker))
                .transformDeferred(RetryOperator.of(retry))
                .doOnSuccess(status -> {
                    sample.stop(meterRegistry.timer("banking.kyc.fetch", "status", "success"));
                    log.info("Successfully fetched KYC status for customerId={}, verificationStatus={}",
                            customerId, status != null ? status.getVerificationStatus() : null);
                })
                .doOnError(throwable -> {
                    sample.stop(meterRegistry.timer("banking.kyc.fetch", "status", "error"));
                    log.error("Failed to fetch KYC status for customerId={}: {}",
                            customerId, throwable.getMessage());
                });
    }

    /**
     * Custom exception type for banking service errors.
     */
    public static class BankingServiceException extends RuntimeException {

        private final String errorCode;
        private final int httpStatus;

        public BankingServiceException(String errorCode, String message, int httpStatus) {
            super(message);
            this.errorCode = errorCode;
            this.httpStatus = httpStatus;
        }

        public String getErrorCode() {
            return errorCode;
        }

        public int getHttpStatus() {
            return httpStatus;
        }
    }
}
