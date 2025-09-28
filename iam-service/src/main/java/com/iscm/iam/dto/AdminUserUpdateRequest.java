package com.iscm.iam.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "User update request for admin operations")
public class AdminUserUpdateRequest {

    @Schema(description = "User email", example = "user@example.com")
    private String email;

    @Schema(description = "First name", example = "John")
    private String firstName;

    @Schema(description = "Last name", example = "Doe")
    private String lastName;

    @Schema(description = "Phone number", example = "+1234567890")
    private String phone;

    @Schema(description = "Account active status", example = "true")
    private Boolean active;

    @Schema(description = "MFA enabled status", example = "false")
    private Boolean mfaEnabled;

    @Schema(description = "List of role names to assign", example = "[\"USER\", \"ADMIN\"]")
    private List<String> roles;

    @Schema(description = "Tenant ID for multi-tenancy", example = "123e4567-e89b-12d3-a456-426614174000")
    private UUID tenantId;
}