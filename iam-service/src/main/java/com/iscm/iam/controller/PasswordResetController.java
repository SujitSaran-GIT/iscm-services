package com.iscm.iam.controller;

import com.iscm.iam.dto.PasswordResetInitiateRequest;
import com.iscm.iam.dto.PasswordResetRequest;
import com.iscm.iam.dto.PasswordResetValidateRequest;
import com.iscm.iam.service.PasswordResetService;
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

@Slf4j
@RestController
@RequestMapping("/api/v1/password-reset")
@RequiredArgsConstructor
@Tag(name = "Password Reset", description = "Password reset functionality endpoints")
public class PasswordResetController {

    private final PasswordResetService passwordResetService;

    @PostMapping("/initiate")
    @Operation(
        summary = "Initiate password reset",
        description = "Sends a password reset link to the user's email address"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Reset email sent"),
        @ApiResponse(responseCode = "400", description = "Invalid email address"),
        @ApiResponse(responseCode = "429", description = "Too many attempts")
    })
    public ResponseEntity<Void> initiatePasswordReset(
            @Parameter(description = "Password reset initiation data", required = true)
            @Valid @RequestBody PasswordResetInitiateRequest request,
            HttpServletRequest httpRequest) {

        String ipAddress = getClientIpAddress(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");

        passwordResetService.initiatePasswordReset(
            request.getEmail(),
            ipAddress,
            userAgent
        );

        // Always return success to prevent email enumeration
        return ResponseEntity.ok().build();
    }

    @PostMapping("/reset")
    @Operation(
        summary = "Reset password",
        description = "Resets the user's password using a valid reset token"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Password reset successful"),
        @ApiResponse(responseCode = "400", description = "Invalid token or weak password"),
        @ApiResponse(responseCode = "429", description = "Too many attempts")
    })
    public ResponseEntity<Void> resetPassword(
            @Parameter(description = "Password reset data", required = true)
            @Valid @RequestBody PasswordResetRequest request,
            HttpServletRequest httpRequest) {

        String ipAddress = getClientIpAddress(httpRequest);
        passwordResetService.resetPassword(
            request.getToken(),
            request.getNewPassword(),
            request.getConfirmPassword(),
            ipAddress
        );

        return ResponseEntity.ok().build();
    }

    @PostMapping("/validate-token")
    @Operation(
        summary = "Validate reset token",
        description = "Checks if a password reset token is valid"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Token validation result"),
        @ApiResponse(responseCode = "400", description = "Invalid token")
    })
    public ResponseEntity<Boolean> validateResetToken(
            @Parameter(description = "Token validation data", required = true)
            @Valid @RequestBody PasswordResetValidateRequest request) {

        boolean isValid = passwordResetService.validateResetToken(request.getToken());
        return ResponseEntity.ok(isValid);
    }

    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}