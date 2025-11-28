package com.iscm.dtos.request;


import lombok.Data;

import java.time.LocalDate;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

@Data
public class UpdateVendorRequest {
    
    @Size(max = 200)
    private String displayName;
    
    @Email
    @Size(max = 100)
    private String email;
    
    @Size(max = 20)
    private String phone;
    
    @Size(max = 100)
    private String website;
    
    private String status; // ACTIVE, INACTIVE, SUSPENDED, BLACKLISTED
    
    private String kycStatus; // NOT_STARTED, IN_PROGRESS, SUBMITTED, VERIFIED, REJECTED
    
    private String slaTier;
    
    private LocalDate contractStartDate;
    
    private LocalDate contractEndDate;
    
    @Size(max = 50)
    private String paymentTerms;
    
    @Size(max = 200)
    private String addressLine1;
    
    @Size(max = 200)
    private String addressLine2;
    
    @Size(max = 100)
    private String city;
    
    @Size(max = 100)
    private String state;
    
    @Size(max = 50)
    private String country;
    
    @Size(max = 20)
    private String postalCode;
    
    @Size(max = 1000)
    private String notes;
}