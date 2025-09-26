package com.iscm.iam.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "Authentication request payload")
public class AuthRequest {
    
    @NotBlank(message = "Email is required")
    @Email(message = "Email should be valid")
    @Schema(description = "User's email address", example = "user@example.com", requiredMode = Schema.RequiredMode.REQUIRED)
    private String email;
    
    @NotBlank(message = "Password is required")
    @Schema(description = "User's password", example = "SecurePass123!", requiredMode = Schema.RequiredMode.REQUIRED, minLength = 8)
    private String password;
    
    @Schema(description = "User agent from the client", example = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36", hidden = true)
    private String userAgent;
    
    @Schema(description = "Client IP address", example = "192.168.1.1", hidden = true)
    private String ipAddress;
}