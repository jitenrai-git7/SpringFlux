package com.springflux.banking.model;

import java.math.BigDecimal;
import java.time.Instant;

public class AccountBalance {

    private String accountId;
    private BigDecimal availableBalance;
    private BigDecimal currentBalance;
    private String currency;
    private Instant lastUpdated;

    public AccountBalance() {
    }

    public AccountBalance(String accountId, BigDecimal availableBalance,
                          BigDecimal currentBalance, String currency, Instant lastUpdated) {
        this.accountId = accountId;
        this.availableBalance = availableBalance;
        this.currentBalance = currentBalance;
        this.currency = currency;
        this.lastUpdated = lastUpdated;
    }

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public BigDecimal getAvailableBalance() {
        return availableBalance;
    }

    public void setAvailableBalance(BigDecimal availableBalance) {
        this.availableBalance = availableBalance;
    }

    public BigDecimal getCurrentBalance() {
        return currentBalance;
    }

    public void setCurrentBalance(BigDecimal currentBalance) {
        this.currentBalance = currentBalance;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public Instant getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(Instant lastUpdated) {
        this.lastUpdated = lastUpdated;
    }
}
