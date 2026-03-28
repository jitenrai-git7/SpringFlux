package com.banking.integration.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KycStatus {

    @JsonProperty("customerId")
    private String customerId;

    @JsonProperty("fullName")
    private String fullName;

    @JsonProperty("dateOfBirth")
    private LocalDate dateOfBirth;

    @JsonProperty("nationality")
    private String nationality;

    @JsonProperty("verificationStatus")
    private VerificationStatus verificationStatus;

    @JsonProperty("riskLevel")
    private RiskLevel riskLevel;

    @JsonProperty("verifiedAt")
    private Instant verifiedAt;

    @JsonProperty("expiresAt")
    private Instant expiresAt;

    @JsonProperty("remarks")
    private String remarks;

    public enum VerificationStatus {
        PENDING, VERIFIED, REJECTED, EXPIRED, UNDER_REVIEW
    }

    public enum RiskLevel {
        LOW, MEDIUM, HIGH
    }
}
