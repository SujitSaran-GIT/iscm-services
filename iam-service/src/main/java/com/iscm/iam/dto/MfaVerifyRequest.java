package com.iscm.iam.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "MFA verification request payload")
public class MfaVerifyRequest {

    @NotBlank(message = "Verification code is required")
    @Schema(description = "MFA verification code", example = "123456",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private String code;

    @NotBlank(message = "Code type is required")
    @Schema(description = "Type of verification code", example = "TOTP",
            allowableValues = {"TOTP", "SMS", "EMAIL", "BACKUP"},
            requiredMode = Schema.RequiredMode.REQUIRED)
    private String codeType;
}