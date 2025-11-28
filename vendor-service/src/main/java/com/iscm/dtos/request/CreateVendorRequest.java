package com.iscm.dtos.request;

import java.time.LocalDate;
import java.util.UUID;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateVendorRequest {
    
    @NotBlank(message = "Vendor code is required")
    @Size(max = 50)
    private String vendorCode;
    
    @NotBlank(message = "Legal name is required")
    @Size(max = 300)
    private String legalName;
    
    @NotBlank(message = "Display name is required")
    @Size(max = 200)
    private String displayName;
    
    @NotNull(message = "Organization ID is required")
    private UUID organizationId;
    
    @Email(message = "Invalid email format")
    @Size(max = 100)
    private String email;
    
    @Size(max = 20)
    private String phone;
    
    @Size(max = 100)
    private String website;
    
    @Size(max = 50)
    private String taxId;
    
    private String slaTier; // BASIC, STANDARD, PREMIUM, ENTERPRISE
    
    private LocalDate contractStartDate;
    
    private LocalDate contractEndDate;
    
    @Size(max = 50)
    private String paymentTerms;
    
    @Size(min = 3, max = 3)
    private String currency;
    
    // Address
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
