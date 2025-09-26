package com.iscm.iam.controller;

import com.iscm.iam.dto.AuthRequest;
import com.iscm.iam.dto.AuthResponse;
import com.iscm.iam.dto.RefreshTokenRequest;
import com.iscm.iam.dto.RegisterRequest;
import com.iscm.iam.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
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
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "User authentication and registration endpoints")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @Operation(
        summary = "Register a new user",
        description = "Creates a new user account with the provided information. Password must meet security requirements."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "User registered successfully"),
        @ApiResponse(responseCode = "400", description = "Validation error or invalid input"),
        @ApiResponse(responseCode = "409", description = "Email already exists"),
        @ApiResponse(responseCode = "429", description = "Rate limit exceeded")
    })
    public ResponseEntity<AuthResponse> register(
            @Parameter(description = "User registration data", required = true)
            @Valid @RequestBody RegisterRequest request,
            HttpServletRequest httpRequest) {
        
        log.info("Registration attempt for email: {} from IP: {}", 
                request.getEmail(), getClientIpAddress(httpRequest));
        
        AuthResponse response = authService.register(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/login")
    @Operation(
        summary = "Authenticate user",
        description = "Authenticates user credentials and returns JWT tokens for API access."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Login successful"),
        @ApiResponse(responseCode = "401", description = "Invalid credentials"),
        @ApiResponse(responseCode = "423", description = "Account locked due to failed attempts"),
        @ApiResponse(responseCode = "429", description = "Rate limit exceeded")
    })
    public ResponseEntity<AuthResponse> login(
            @Parameter(description = "Authentication credentials", required = true)
            @Valid @RequestBody AuthRequest request,
            HttpServletRequest httpRequest) {
        
        // Extract client info from request
        request.setUserAgent(httpRequest.getHeader("User-Agent"));
        request.setIpAddress(getClientIpAddress(httpRequest));
        
        log.info("Login attempt for email: {} from IP: {}", 
                request.getEmail(), request.getIpAddress());
        
        AuthResponse response = authService.login(request);
        log.info("Successful login for user: {}", request.getEmail());
        
        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh")
    @Operation(
        summary = "Refresh access token",
        description = "Uses a valid refresh token to obtain a new access token without re-authentication."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Token refreshed successfully"),
        @ApiResponse(responseCode = "401", description = "Invalid or expired refresh token"),
        @ApiResponse(responseCode = "429", description = "Rate limit exceeded")
    })
    public ResponseEntity<AuthResponse> refreshToken(
            @Parameter(description = "Refresh token request", required = true)
            @Valid @RequestBody RefreshTokenRequest request) {
        
        AuthResponse response = authService.refreshToken(request.getRefreshToken());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    @Operation(
        summary = "Logout user",
        description = "Invalidates the provided refresh token, logging the user out from the current session."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Logout successful"),
        @ApiResponse(responseCode = "400", description = "Invalid request"),
        @ApiResponse(responseCode = "429", description = "Rate limit exceeded")
    })
    public ResponseEntity<Void> logout(
            @Parameter(description = "Refresh token to invalidate", required = true)
            @Valid @RequestBody RefreshTokenRequest request) {
        
        authService.logout(request.getRefreshToken());
        return ResponseEntity.ok().build();
    }

    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}