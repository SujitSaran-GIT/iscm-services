package com.iscm.iam.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.UUID;

@Data
@Schema(description = "User registration request payload")
public class RegisterRequest {
    
    @NotBlank(message = "Email is required")
    @Email(message = "Email should be valid")
    @Schema(description = "User's email address", example = "new.user@example.com", requiredMode = Schema.RequiredMode.REQUIRED)
    private String email;
    
    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    @Schema(description = "User's password (min 8 characters with uppercase, lowercase, digit, and special character)", 
            example = "SecurePass123!", requiredMode = Schema.RequiredMode.REQUIRED, minLength = 8)
    private String password;
    
    @NotBlank(message = "First name is required")
    @Schema(description = "User's first name", example = "John", requiredMode = Schema.RequiredMode.REQUIRED)
    private String firstName;
    
    @Schema(description = "User's last name", example = "Doe")
    private String lastName;
    
    @Schema(description = "User's phone number", example = "+1234567890")
    private String phone;
    
    @Schema(description = "Organization ID for user association", example = "123e4567-e89b-12d3-a456-426614174000")
    private UUID organizationId;
}