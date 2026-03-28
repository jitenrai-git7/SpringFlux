package com.banking.integration.config;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.time.Duration;

@Slf4j
@Configuration
public class Resilience4jConfig {

    public static final String BANKING_SERVICE = "bankingService";

    @Bean
    public CircuitBreakerRegistry circuitBreakerRegistry() {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
                .slidingWindowSize(10)
                .minimumNumberOfCalls(5)
                .failureRateThreshold(50.0f)
                .waitDurationInOpenState(Duration.ofSeconds(30))
                .permittedNumberOfCallsInHalfOpenState(3)
                .slowCallDurationThreshold(Duration.ofSeconds(4))
                .slowCallRateThreshold(80.0f)
                .recordExceptions(IOException.class,
                        org.springframework.web.reactive.function.client.WebClientRequestException.class)
                .build();

        CircuitBreakerRegistry registry = CircuitBreakerRegistry.of(config);

        registry.circuitBreaker(BANKING_SERVICE).getEventPublisher()
                .onStateTransition(event ->
                        log.warn("CircuitBreaker '{}' state transition: {} -> {}",
                                BANKING_SERVICE,
                                event.getStateTransition().getFromState(),
                                event.getStateTransition().getToState()))
                .onCallNotPermitted(event ->
                        log.warn("CircuitBreaker '{}' call not permitted", BANKING_SERVICE))
                .onError(event ->
                        log.error("CircuitBreaker '{}' recorded error: {}",
                                BANKING_SERVICE, event.getThrowable().getMessage()));

        return registry;
    }

    @Bean
    public RetryRegistry retryRegistry() {
        RetryConfig config = RetryConfig.custom()
                .maxAttempts(3)
                .intervalFunction(io.github.resilience4j.core.IntervalFunction
                        .ofExponentialBackoff(500, 2.0))
                .retryExceptions(IOException.class,
                        org.springframework.web.reactive.function.client.WebClientRequestException.class)
                .build();

        RetryRegistry registry = RetryRegistry.of(config);

        registry.retry(BANKING_SERVICE).getEventPublisher()
                .onRetry(event ->
                        log.warn("Retry attempt {} for '{}'", event.getNumberOfRetryAttempts(), BANKING_SERVICE))
                .onError(event ->
                        log.error("All retries exhausted for '{}': {}",
                                BANKING_SERVICE, event.getLastThrowable().getMessage()));

        return registry;
    }

    @Bean
    public TimeLimiterRegistry timeLimiterRegistry() {
        TimeLimiterConfig config = TimeLimiterConfig.custom()
                .timeoutDuration(Duration.ofSeconds(6))
                .cancelRunningFuture(true)
                .build();

        return TimeLimiterRegistry.of(config);
    }
}
