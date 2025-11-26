package com.iscm.iam.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/**
 * DISABLED STUB - Security controller has been disabled for simplification.
 * This controller provides placeholder responses for security endpoints.
 * To re-enable security features, restore the original implementation.
 */
@Slf4j
//@RestController
//@RequestMapping("/api/v1/security")
public class SecurityController {

    /**
     * Get security statistics - DISABLED
     */
    @GetMapping("/statistics")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SECURITY_OFFICER')")
    public ResponseEntity<String> getSecurityStatistics() {
        log.info("Security statistics endpoint called - monitoring disabled");
        return ResponseEntity.ok("{\"message\": \"Security monitoring disabled\", \"statistics\": {}}");
    }

    /**
     * Get recent suspicious activities - DISABLED
     */
    @GetMapping("/activities")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SECURITY_OFFICER')")
    public ResponseEntity<String> getRecentSuspiciousActivities(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "timestamp") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        log.info("Security activities endpoint called - monitoring disabled");
        return ResponseEntity.ok("{\"message\": \"Security monitoring disabled\", \"activities\": []}");
    }

    /**
     * Mark suspicious activity as investigated - DISABLED
     */
    @PostMapping("/activities/{activityId}/investigate")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SECURITY_OFFICER')")
    public ResponseEntity<String> markActivityAsInvestigated(@PathVariable @NotNull UUID activityId) {
        log.info("Activity investigation endpoint called - monitoring disabled");
        return ResponseEntity.ok("{\"message\": \"Security monitoring disabled\"}");
    }

    /**
     * Get rate limit status for an IP - DISABLED
     */
    @GetMapping("/rate-limit/{ipAddress}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SECURITY_OFFICER')")
    public ResponseEntity<String> getRateLimitStatus(
            @PathVariable @NotNull String ipAddress) {

        log.info("Rate limit status endpoint called - rate limiting disabled");
        return ResponseEntity.ok("{\"message\": \"Rate limiting disabled\", \"ip\": \"" + ipAddress + "\"}");
    }

    /**
     * Clear rate limit for an IP (admin function) - DISABLED
     */
    @DeleteMapping("/rate-limit/{ipAddress}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SECURITY_OFFICER')")
    public ResponseEntity<String> clearRateLimit(@PathVariable @NotNull String ipAddress) {
        log.info("Rate limit clear endpoint called - rate limiting disabled");
        return ResponseEntity.ok("{\"message\": \"Rate limiting disabled\"}");
    }

    /**
     * Manual security event logging - DISABLED
     */
    @PostMapping("/events")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SECURITY_OFFICER')")
    public ResponseEntity<String> logSecurityEvent(
            @RequestParam String eventType,
            @RequestParam @NotNull String description,
            @RequestParam(required = false) String ipAddress,
            @RequestParam(required = false) String userAgent,
            @RequestParam(required = false) UUID userId) {

        log.info("Security event logging endpoint called - monitoring disabled. Event: {}, Description: {}",
                eventType, description);
        return ResponseEntity.ok("{\"message\": \"Security event logging disabled\"}");
    }
}