package com.iscm.dtos.response;

import lombok.Data;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class WarehouseResponse {
    private UUID id;
    private String warehouseCode;
    private String name;
    private UUID organizationId;
    private String organizationName;
    private UUID vendorId;
    private String vendorName;
    private String type;
    private String addressLine1;
    private String addressLine2;
    private String city;
    private String state;
    private String country;
    private String postalCode;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private String timezone;
    private String contactPhone;
    private String contactEmail;
    private String managerName;
    private Integer capacitySqft;
    private String status;
    private Boolean isTemperatureControlled;
    private String operatingHours;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}