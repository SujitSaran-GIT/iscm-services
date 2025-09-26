package com.iscm.iam.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "OAuth callback request payload")
public class OAuthCallbackRequest {

    @NotBlank(message = "Authorization code is required")
    @Schema(description = "OAuth authorization code", example = "4/0Ade5...",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private String code;

    @NotBlank(message = "Redirect URI is required")
    @Schema(description = "Redirect URI used in the OAuth request",
            example = "https://your-app.com/oauth/callback",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private String redirectUri;

    @Schema(description = "OAuth state parameter (for CSRF protection)",
            example = "state_123456")
    private String state;
}