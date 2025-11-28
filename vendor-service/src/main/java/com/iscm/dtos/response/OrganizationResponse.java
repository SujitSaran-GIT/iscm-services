package com.iscm.dtos.response;

import lombok.Data;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class OrganizationResponse {
    private UUID id;
    private String name;
    private String domain;
    private UUID parentOrgId;
    private String description;
    private String phone;
    private String email;
    private String addressLine1;
    private String addressLine2;
    private String city;
    private String state;
    private String country;
    private String postalCode;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
