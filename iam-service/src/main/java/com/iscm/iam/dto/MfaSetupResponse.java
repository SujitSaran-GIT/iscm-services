package com.iscm.iam.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "MFA setup response containing secret and QR code URL")
public class MfaSetupResponse {

    @Schema(description = "TOTP secret key", example = "JBSWY3DPEHPK3PXP")
    private String secret;

    @Schema(description = "QR code URL for scanning with authenticator app",
            example = "otpauth://totp/ISCM%20Platform:user@example.com?secret=JBSWY3DPEHPK3PXP&issuer=ISCM%20Platform")
    private String qrCodeUrl;
}