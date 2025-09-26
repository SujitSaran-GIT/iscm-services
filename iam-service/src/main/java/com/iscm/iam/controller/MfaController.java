package com.iscm.iam.controller;

import com.iscm.iam.dto.MfaEnableRequest;
import com.iscm.iam.dto.MfaSetupResponse;
import com.iscm.iam.dto.MfaVerifyRequest;
import com.iscm.iam.service.MfaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/mfa")
@RequiredArgsConstructor
@Tag(name = "Multi-Factor Authentication", description = "MFA setup and verification endpoints")
public class MfaController {

    private final MfaService mfaService;

    @PostMapping("/setup/totp")
    @Operation(
        summary = "Setup TOTP MFA",
        description = "Generates a TOTP secret and QR code URL for MFA setup"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "TOTP setup initiated"),
        @ApiResponse(responseCode = "400", description = "Invalid request"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<MfaSetupResponse> setupTotp(
            @AuthenticationPrincipal UserDetails userDetails) {

        UUID userId = extractUserIdFromDetails(userDetails);
        String secret = mfaService.generateTotpSecret(userId);
        String qrUrl = mfaService.getTotpQrUrl(userId, userDetails.getUsername());

        MfaSetupResponse response = new MfaSetupResponse(secret, qrUrl);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/enable")
    @Operation(
        summary = "Enable MFA",
        description = "Enables MFA for the user account after verification"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "MFA enabled successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid verification code"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<Void> enableMfa(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody MfaEnableRequest request) {

        UUID userId = extractUserIdFromDetails(userDetails);
        mfaService.enableMfa(userId, request.getVerificationCode(), request.getMfaType());

        return ResponseEntity.ok().build();
    }

    @PostMapping("/disable")
    @Operation(
        summary = "Disable MFA",
        description = "Disables MFA for the user account"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "MFA disabled successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<Void> disableMfa(
            @AuthenticationPrincipal UserDetails userDetails) {

        UUID userId = extractUserIdFromDetails(userDetails);
        mfaService.disableMfa(userId);

        return ResponseEntity.ok().build();
    }

    @PostMapping("/verify")
    @Operation(
        summary = "Verify MFA code",
        description = "Verifies MFA code during login"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "MFA code verified successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid verification code"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<Boolean> verifyMfa(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody MfaVerifyRequest request) {

        UUID userId = extractUserIdFromDetails(userDetails);
        boolean isValid = false;

        if ("BACKUP".equals(request.getCodeType())) {
            isValid = mfaService.validateBackupCode(userId, request.getCode());
        } else if ("TOTP".equals(request.getCodeType())) {
            isValid = mfaService.verifyTotp(userId, request.getCode());
        }

        return ResponseEntity.ok(isValid);
    }

    @PostMapping("/send-sms")
    @Operation(
        summary = "Send SMS verification code",
        description = "Sends a verification code via SMS for MFA"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "SMS code sent successfully"),
        @ApiResponse(responseCode = "400", description = "Phone number not set up"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<Void> sendSmsCode(
            @AuthenticationPrincipal UserDetails userDetails) {

        UUID userId = extractUserIdFromDetails(userDetails);
        mfaService.sendSmsCode(userId);

        return ResponseEntity.ok().build();
    }

    @PostMapping("/send-email")
    @Operation(
        summary = "Send email verification code",
        description = "Sends a verification code via email for MFA"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Email code sent successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<Void> sendEmailCode(
            @AuthenticationPrincipal UserDetails userDetails) {

        UUID userId = extractUserIdFromDetails(userDetails);
        mfaService.sendEmailCode(userId);

        return ResponseEntity.ok().build();
    }

    private UUID extractUserIdFromDetails(UserDetails userDetails) {
        // Extract user ID from the authentication details
        // This implementation depends on how you store user ID in the authentication
        return UUID.fromString(userDetails.getUsername()); // Simplified for example
    }
}