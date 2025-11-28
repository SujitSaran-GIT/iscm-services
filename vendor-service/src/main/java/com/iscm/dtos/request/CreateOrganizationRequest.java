package com.iscm.dtos.request;

import java.util.UUID;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateOrganizationRequest {
    
    @NotBlank(message = "Name is required")
    @Size(max = 200)
    private String name;
    
    @Size(max = 100)
    private String domain;
    
    private UUID parentOrgId;
    
    @Size(max = 500)
    private String description;
    
    @Size(max = 20)
    private String phone;
    
    @Email
    @Size(max = 100)
    private String email;
    
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
}
