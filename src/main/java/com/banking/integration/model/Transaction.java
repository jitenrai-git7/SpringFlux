package com.banking.integration.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Transaction {

    @JsonProperty("transactionId")
    private String transactionId;

    @JsonProperty("accountId")
    private String accountId;

    @JsonProperty("type")
    private TransactionType type;

    @JsonProperty("amount")
    private BigDecimal amount;

    @JsonProperty("currency")
    private String currency;

    @JsonProperty("description")
    private String description;

    @JsonProperty("referenceNumber")
    private String referenceNumber;

    @JsonProperty("status")
    private TransactionStatus status;

    @JsonProperty("timestamp")
    private Instant timestamp;

    @JsonProperty("balanceAfter")
    private BigDecimal balanceAfter;

    public enum TransactionType {
        CREDIT, DEBIT, TRANSFER, REVERSAL
    }

    public enum TransactionStatus {
        PENDING, COMPLETED, FAILED, REVERSED
    }
}
