package com.springflux.banking.exception;

import com.springflux.banking.model.ErrorResponse;
import com.springflux.banking.service.BankingService.BankingServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.concurrent.TimeoutException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BankingServiceException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleBankingServiceException(
            BankingServiceException ex, ServerWebExchange exchange) {
        log.error("Banking service exception: {}", ex.getMessage(), ex);
        ErrorResponse error = new ErrorResponse(
                HttpStatus.SERVICE_UNAVAILABLE.value(),
                "Service Unavailable",
                ex.getMessage(),
                exchange.getRequest().getPath().value());
        return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(error));
    }

    @ExceptionHandler(WebClientResponseException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleWebClientResponseException(
            WebClientResponseException ex, ServerWebExchange exchange) {
        log.error("WebClient response error: status={}, body={}", ex.getStatusCode(), ex.getResponseBodyAsString());
        HttpStatus status = HttpStatus.valueOf(ex.getStatusCode().value());
        ErrorResponse error = new ErrorResponse(
                status.value(),
                status.getReasonPhrase(),
                "Banking API error: " + ex.getMessage(),
                exchange.getRequest().getPath().value());
        return Mono.just(ResponseEntity.status(status).body(error));
    }

    @ExceptionHandler(TimeoutException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleTimeoutException(
            TimeoutException ex, ServerWebExchange exchange) {
        log.error("Request timed out: {}", ex.getMessage());
        ErrorResponse error = new ErrorResponse(
                HttpStatus.GATEWAY_TIMEOUT.value(),
                "Gateway Timeout",
                "The banking service did not respond in time. Please try again later.",
                exchange.getRequest().getPath().value());
        return Mono.just(ResponseEntity.status(HttpStatus.GATEWAY_TIMEOUT).body(error));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleIllegalArgumentException(
            IllegalArgumentException ex, ServerWebExchange exchange) {
        log.warn("Bad request: {}", ex.getMessage());
        ErrorResponse error = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "Bad Request",
                ex.getMessage(),
                exchange.getRequest().getPath().value());
        return Mono.just(ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error));
    }

    @ExceptionHandler(Exception.class)
    public Mono<ResponseEntity<ErrorResponse>> handleGenericException(
            Exception ex, ServerWebExchange exchange) {
        log.error("Unexpected error: {}", ex.getMessage(), ex);
        ErrorResponse error = new ErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "Internal Server Error",
                "An unexpected error occurred. Please try again later.",
                exchange.getRequest().getPath().value());
        return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error));
    }
}
