package com.iscm.dtos.request;

import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

@Data
public class CreateWarehouseRequest {
    
    @NotBlank(message = "Warehouse code is required")
    @Size(max = 50)
    private String warehouseCode;
    
    @NotBlank(message = "Name is required")
    @Size(max = 200)
    private String name;
    
    @NotNull(message = "Organization ID is required")
    private UUID organizationId;
    
    private UUID vendorId;
    
    private String type; // DISTRIBUTION, FULFILLMENT, CROSS_DOCK, COLD_STORAGE, THIRD_PARTY
    
    @NotBlank(message = "Address is required")
    @Size(max = 200)
    private String addressLine1;
    
    @Size(max = 200)
    private String addressLine2;
    
    @NotBlank(message = "City is required")
    @Size(max = 100)
    private String city;
    
    @Size(max = 100)
    private String state;
    
    @NotBlank(message = "Country is required")
    @Size(max = 50)
    private String country;
    
    @Size(max = 20)
    private String postalCode;
    
    @DecimalMin(value = "-90.0")
    @DecimalMax(value = "90.0")
    private BigDecimal latitude;
    
    @DecimalMin(value = "-180.0")
    @DecimalMax(value = "180.0")
    private BigDecimal longitude;
    
    @Size(max = 50)
    private String timezone;
    
    @Size(max = 20)
    private String contactPhone;
    
    @Email
    @Size(max = 100)
    private String contactEmail;
    
    @Size(max = 100)
    private String managerName;
    
    @Positive
    private Integer capacitySqft;
    
    private Boolean isTemperatureControlled;
    
    @Size(max = 100)
    private String operatingHours;
}
