package com.iscm.iam.dto;

import com.iscm.iam.model.OAuthAccount;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "OAuth account information")
public class OAuthAccountDto {

    @Schema(description = "OAuth provider", example = "GOOGLE")
    private String provider;

    @Schema(description = "Provider-specific user ID", example = "123456789")
    private String providerId;

    @Schema(description = "Provider username", example = "john.doe")
    private String providerUsername;

    @Schema(description = "Email associated with OAuth account", example = "john.doe@gmail.com")
    private String email;

    @Schema(description = "Whether the OAuth account is active", example = "true")
    private Boolean isActive;

    @Schema(description = "Last token expiry time", example = "2024-12-31T23:59:59")
    private LocalDateTime tokenExpiry;

    @Schema(description = "OAuth scopes granted", example = "email profile")
    private String scopes;

    public static OAuthAccountDto fromEntity(OAuthAccount account) {
        return new OAuthAccountDto(
            account.getProvider().name(),
            account.getProviderId(),
            account.getProviderUsername(),
            account.getEmail(),
            account.getIsActive(),
            account.getTokenExpiry(),
            account.getScopes()
        );
    }
}