package com.iscm.iam.dto;

import com.iscm.iam.model.User;
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
@Schema(description = "Authentication response with JWT tokens")
public class AuthResponse {
    
    @Schema(description = "JWT access token for API authentication", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
    private String accessToken;
    
    @Schema(description = "JWT refresh token for obtaining new access tokens", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
    private String refreshToken;
    
    @Schema(description = "Token type", example = "Bearer")
    private String tokenType;
    
    @Schema(description = "Access token expiration time in seconds", example = "900")
    private Long expiresIn;
    
    @Schema(description = "Authenticated user information")
    private UserDto user;

    @Schema(description = "Indicates if this is a new user registration (OAuth only)", example = "false")
    @Builder.Default
    private Boolean isNewUser = false;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "User information")
    public static class UserDto {
        @Schema(description = "User ID", example = "123e4567-e89b-12d3-a456-426614174000")
        private String id;

        @Schema(description = "User email", example = "user@example.com")
        private String email;

        @Schema(description = "First name", example = "John")
        private String firstName;

        @Schema(description = "Last name", example = "Doe")
        private String lastName;

        @Schema(description = "Phone number", example = "+1234567890")
        private String phoneNumber;

        @Schema(description = "Email verification status", example = "false")
        private Boolean emailVerified;

        @Schema(description = "MFA enabled status", example = "false")
        private Boolean mfaEnabled;

        @Schema(description = "Account active status", example = "true")
        private Boolean active;

        @Schema(description = "Account creation timestamp", example = "2024-01-01T00:00:00Z")
        private String createdAt;

        @Schema(description = "Account last update timestamp", example = "2024-01-01T00:00:00Z")
        private String updatedAt;

        @Schema(description = "User roles", example = "[\"USER\", \"ADMIN\"]")
        private List<String> roles;

        @Schema(description = "Tenant ID for multi-tenancy", example = "123e4567-e89b-12d3-a456-426614174000")
        private UUID tenantId;

        public static UserDto fromEntity(User user) {
            return UserDto.builder()
                    .id(user.getId().toString())
                    .email(user.getEmail())
                    .firstName(user.getFirstName())
                    .lastName(user.getLastName())
                    .phoneNumber(user.getPhone())
                    .emailVerified(false) // You can implement email verification logic
                    .mfaEnabled(user.getMfaEnabled())
                    .active(user.getIsActive())
                    .createdAt(user.getCreatedAt() != null ? user.getCreatedAt().toString() : null)
                    .updatedAt(user.getUpdatedAt() != null ? user.getUpdatedAt().toString() : null)
                    .roles(user.getUserRoles().stream().map(userRole -> userRole.getRole().getName()).toList())
                    .tenantId(user.getTenantId())
                    .build();
        }
    }
}