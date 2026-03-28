package com.banking.integration.exception;

import com.banking.integration.model.ErrorResponse;
import com.banking.integration.service.BankingService.BankingServiceException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.concurrent.TimeoutException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BankingServiceException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleBankingServiceException(
            BankingServiceException ex, ServerWebExchange exchange) {
        log.error("BankingServiceException [{}]: {}", ex.getErrorCode(), ex.getMessage());
        ErrorResponse error = ErrorResponse.builder()
                .errorCode(ex.getErrorCode())
                .message(ex.getMessage())
                .timestamp(Instant.now())
                .path(exchange.getRequest().getPath().value())
                .build();
        return Mono.just(ResponseEntity.status(ex.getHttpStatus()).body(error));
    }

    @ExceptionHandler(CallNotPermittedException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleCircuitBreakerOpen(
            CallNotPermittedException ex, ServerWebExchange exchange) {
        log.warn("Circuit breaker is OPEN - request rejected: {}", ex.getMessage());
        ErrorResponse error = ErrorResponse.builder()
                .errorCode("CIRCUIT_BREAKER_OPEN")
                .message("Banking service is temporarily unavailable. Please try again later.")
                .details(ex.getMessage())
                .timestamp(Instant.now())
                .path(exchange.getRequest().getPath().value())
                .build();
        return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(error));
    }

    @ExceptionHandler(TimeoutException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleTimeout(
            TimeoutException ex, ServerWebExchange exchange) {
        log.error("Request timed out: {}", ex.getMessage());
        ErrorResponse error = ErrorResponse.builder()
                .errorCode("REQUEST_TIMEOUT")
                .message("The banking service did not respond in time. Please try again.")
                .details(ex.getMessage())
                .timestamp(Instant.now())
                .path(exchange.getRequest().getPath().value())
                .build();
        return Mono.just(ResponseEntity.status(HttpStatus.GATEWAY_TIMEOUT).body(error));
    }

    @ExceptionHandler(WebClientResponseException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleWebClientResponseException(
            WebClientResponseException ex, ServerWebExchange exchange) {
        log.error("Upstream HTTP error {}: {}", ex.getStatusCode(), ex.getMessage());
        ErrorResponse error = ErrorResponse.builder()
                .errorCode("UPSTREAM_HTTP_ERROR")
                .message("Received error response from banking provider.")
                .details(ex.getResponseBodyAsString())
                .timestamp(Instant.now())
                .path(exchange.getRequest().getPath().value())
                .build();
        return Mono.just(ResponseEntity.status(ex.getStatusCode()).body(error));
    }

    @ExceptionHandler(WebClientRequestException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleWebClientRequestException(
            WebClientRequestException ex, ServerWebExchange exchange) {
        log.error("WebClient request failed: {}", ex.getMessage());
        ErrorResponse error = ErrorResponse.builder()
                .errorCode("SERVICE_UNREACHABLE")
                .message("Could not connect to the banking service.")
                .details(ex.getMessage())
                .timestamp(Instant.now())
                .path(exchange.getRequest().getPath().value())
                .build();
        return Mono.just(ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(error));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleIllegalArgumentException(
            IllegalArgumentException ex, ServerWebExchange exchange) {
        log.warn("Invalid request argument: {}", ex.getMessage());
        ErrorResponse error = ErrorResponse.builder()
                .errorCode("INVALID_REQUEST")
                .message(ex.getMessage())
                .timestamp(Instant.now())
                .path(exchange.getRequest().getPath().value())
                .build();
        return Mono.just(ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error));
    }

    @ExceptionHandler(Exception.class)
    public Mono<ResponseEntity<ErrorResponse>> handleGenericException(
            Exception ex, ServerWebExchange exchange) {
        log.error("Unexpected error: {}", ex.getMessage(), ex);
        ErrorResponse error = ErrorResponse.builder()
                .errorCode("INTERNAL_SERVER_ERROR")
                .message("An unexpected error occurred.")
                .timestamp(Instant.now())
                .path(exchange.getRequest().getPath().value())
                .build();
        return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error));
    }
}
