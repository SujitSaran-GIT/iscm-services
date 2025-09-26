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
        
        @Schema(description = "Full name", example = "John Doe")
        private String fullName;
        
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
                    .fullName(user.getFullName())
                    .roles(user.getRoles().stream().map(role -> role.getName()).toList())
                    .tenantId(user.getTenantId())
                    .build();
        }
    }
}