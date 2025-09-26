package com.iscm.iam.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "Password reset token validation request payload")
public class PasswordResetValidateRequest {

    @NotBlank(message = "Reset token is required")
    @Schema(description = "Password reset token to validate",
            example = "a1b2c3d4e5f6789012345678901234567890abcdef1234567890123456789012345",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private String token;
}