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
public class AccountBalance {

    @JsonProperty("accountId")
    private String accountId;

    @JsonProperty("accountNumber")
    private String accountNumber;

    @JsonProperty("accountType")
    private String accountType;

    @JsonProperty("currency")
    private String currency;

    @JsonProperty("availableBalance")
    private BigDecimal availableBalance;

    @JsonProperty("currentBalance")
    private BigDecimal currentBalance;

    @JsonProperty("holdAmount")
    private BigDecimal holdAmount;

    @JsonProperty("lastUpdated")
    private Instant lastUpdated;
}
