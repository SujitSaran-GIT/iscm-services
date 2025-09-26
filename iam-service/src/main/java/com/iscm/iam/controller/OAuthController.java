package com.iscm.iam.controller;

import com.iscm.iam.dto.AuthResponse;
import com.iscm.iam.dto.OAuthAccountDto;
import com.iscm.iam.dto.OAuthCallbackRequest;
import com.iscm.iam.dto.OAuthUrlResponse;
import com.iscm.iam.service.OAuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/oauth")
@RequiredArgsConstructor
@Tag(name = "OAuth Authentication", description = "OAuth provider integration endpoints")
public class OAuthController {

    private final OAuthService oAuthService;

    @GetMapping("/{provider}/url")
    @Operation(
        summary = "Get OAuth authorization URL",
        description = "Returns the OAuth authorization URL for the specified provider"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OAuth URL generated"),
        @ApiResponse(responseCode = "400", description = "Invalid provider"),
        @ApiResponse(responseCode = "429", description = "Rate limit exceeded")
    })
    public ResponseEntity<OAuthUrlResponse> getOAuthUrl(
            @Parameter(description = "OAuth provider", required = true, example = "google")
            @PathVariable String provider,
            @Parameter(description = "Redirect URI after authentication", required = true)
            @RequestParam String redirectUri) {

        String oauthUrl = oAuthService.getOAuthUrl(provider, redirectUri);
        OAuthUrlResponse response = new OAuthUrlResponse(oauthUrl);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{provider}/callback")
    @Operation(
        summary = "Handle OAuth callback",
        description = "Processes the OAuth callback and authenticates the user"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OAuth authentication successful"),
        @ApiResponse(responseCode = "400", description = "Invalid OAuth response"),
        @ApiResponse(responseCode = "401", description = "Authentication failed"),
        @ApiResponse(responseCode = "429", description = "Rate limit exceeded")
    })
    public ResponseEntity<AuthResponse> handleOAuthCallback(
            @Parameter(description = "OAuth provider", required = true, example = "google")
            @PathVariable String provider,
            @Parameter(description = "OAuth callback data", required = true)
            @Valid @RequestBody OAuthCallbackRequest request,
            HttpServletRequest httpRequest) {

        String ipAddress = getClientIpAddress(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");

        AuthResponse response = oAuthService.handleOAuthCallback(
            provider,
            request.getCode(),
            request.getRedirectUri(),
            userAgent,
            ipAddress
        );

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{provider}/unlink")
    @Operation(
        summary = "Unlink OAuth account",
        description = "Removes the OAuth account linkage for the specified provider"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OAuth account unlinked"),
        @ApiResponse(responseCode = "400", description = "Invalid provider or account not found"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<Void> unlinkOAuthAccount(
            @Parameter(description = "OAuth provider", required = true, example = "google")
            @PathVariable String provider,
            @Parameter(description = "User ID", required = true)
            @RequestParam UUID userId) {

        oAuthService.unlinkOAuthAccount(userId, provider);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/accounts")
    @Operation(
        summary = "Get user OAuth accounts",
        description = "Returns all OAuth accounts linked to the user"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OAuth accounts retrieved"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<List<OAuthAccountDto>> getUserOAuthAccounts(
            @Parameter(description = "User ID", required = true)
            @RequestParam UUID userId) {

        List<OAuthAccountDto> accounts = oAuthService.getUserOAuthAccounts(userId)
                .stream()
                .map(OAuthAccountDto::fromEntity)
                .toList();

        return ResponseEntity.ok(accounts);
    }

    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}