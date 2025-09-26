package com.iscm.iam.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "Password reset request payload")
public class PasswordResetRequest {

    @NotBlank(message = "Reset token is required")
    @Schema(description = "Password reset token", example = "a1b2c3d4e5f6789012345678901234567890abcdef1234567890123456789012345",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private String token;

    @NotBlank(message = "New password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    @Schema(description = "New password (min 8 characters with uppercase, lowercase, digit, and special character)",
            example = "NewSecurePass123!", requiredMode = Schema.RequiredMode.REQUIRED, minLength = 8)
    private String newPassword;

    @NotBlank(message = "Password confirmation is required")
    @Schema(description = "Confirm new password", example = "NewSecurePass123!",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private String confirmPassword;
}