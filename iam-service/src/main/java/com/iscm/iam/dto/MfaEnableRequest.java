package com.iscm.iam.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "MFA enable request payload")
public class MfaEnableRequest {

    @NotBlank(message = "Verification code is required")
    @Schema(description = "Verification code from authenticator app/SMS/email",
            example = "123456", requiredMode = Schema.RequiredMode.REQUIRED)
    private String verificationCode;

    @NotBlank(message = "MFA type is required")
    @Schema(description = "Type of MFA to enable", example = "TOTP",
            allowableValues = {"TOTP", "SMS", "EMAIL"}, requiredMode = Schema.RequiredMode.REQUIRED)
    private String mfaType;

    @Schema(description = "Phone number for SMS MFA", example = "+1234567890")
    private String phoneNumber;
}