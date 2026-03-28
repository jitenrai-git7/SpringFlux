package com.springflux.banking.model;

import java.time.Instant;

public class KycVerification {

    private String customerId;
    private String status;
    private String riskLevel;
    private boolean verified;
    private Instant verifiedAt;
    private String remarks;

    public KycVerification() {
    }

    public KycVerification(String customerId, String status, String riskLevel,
                           boolean verified, Instant verifiedAt, String remarks) {
        this.customerId = customerId;
        this.status = status;
        this.riskLevel = riskLevel;
        this.verified = verified;
        this.verifiedAt = verifiedAt;
        this.remarks = remarks;
    }

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getRiskLevel() {
        return riskLevel;
    }

    public void setRiskLevel(String riskLevel) {
        this.riskLevel = riskLevel;
    }

    public boolean isVerified() {
        return verified;
    }

    public void setVerified(boolean verified) {
        this.verified = verified;
    }

    public Instant getVerifiedAt() {
        return verifiedAt;
    }

    public void setVerifiedAt(Instant verifiedAt) {
        this.verifiedAt = verifiedAt;
    }

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }
}
