package com.iscm.dtos.response;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class VendorResponse {
    private UUID id;
    private String vendorCode;
    private String legalName;
    private String displayName;
    private UUID organizationId;
    private String organizationName;
    private String email;
    private String phone;
    private String website;
    private String taxId;
    private String status;
    private String kycStatus;
    private String slaTier;
    private LocalDate contractStartDate;
    private LocalDate contractEndDate;
    private String paymentTerms;
    private String currency;
    private String addressLine1;
    private String addressLine2;
    private String city;
    private String state;
    private String country;
    private String postalCode;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}


