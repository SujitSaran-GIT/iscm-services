package com.iscm.iam.controller;

import com.iscm.iam.model.SuspiciousActivity;
import com.iscm.iam.security.SecurityMonitoringService;
import com.iscm.iam.security.RateLimitingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/security")
@RequiredArgsConstructor
public class SecurityController {

    private final SecurityMonitoringService securityMonitoringService;
    private final RateLimitingService rateLimitingService;

    /**
     * Get security statistics
     */
    @GetMapping("/statistics")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SECURITY_OFFICER')")
    public ResponseEntity<SecurityMonitoringService.SecurityStatistics> getSecurityStatistics() {
        SecurityMonitoringService.SecurityStatistics stats = securityMonitoringService.getSecurityStatistics();
        return ResponseEntity.ok(stats);
    }

    /**
     * Get recent suspicious activities
     */
    @GetMapping("/activities")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SECURITY_OFFICER')")
    public ResponseEntity<Page<SuspiciousActivity>> getRecentSuspiciousActivities(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        Sort sort = sortDir.equalsIgnoreCase("desc") ?
                Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<SuspiciousActivity> activities = securityMonitoringService.getRecentSuspiciousActivities(pageable);
        return ResponseEntity.ok(activities);
    }

    /**
     * Mark suspicious activity as investigated
     */
    @PostMapping("/activities/{activityId}/investigate")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SECURITY_OFFICER')")
    public ResponseEntity<Void> markActivityAsInvestigated(@PathVariable @NotNull UUID activityId) {
        securityMonitoringService.markActivityAsInvestigated(activityId);
        return ResponseEntity.ok().build();
    }

    /**
     * Get rate limit status for an IP
     */
    @GetMapping("/rate-limit/{ipAddress}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SECURITY_OFFICER')")
    public ResponseEntity<RateLimitingService.RateLimitStatus> getRateLimitStatus(
            @PathVariable @NotNull String ipAddress) {

        RateLimitingService.RateLimitStatus status = rateLimitingService.getRateLimitStatus(
                ipAddress, 1000, java.time.Duration.ofMinutes(1));

        return ResponseEntity.ok(status);
    }

    /**
     * Clear rate limit for an IP (admin function)
     */
    @DeleteMapping("/rate-limit/{ipAddress}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> clearRateLimit(@PathVariable @NotNull String ipAddress) {
        rateLimitingService.clearRateLimit(ipAddress);
        log.info("Rate limit cleared for IP: {} by admin", ipAddress);
        return ResponseEntity.ok().build();
    }

    /**
     * Manual security event logging
     */
    @PostMapping("/events")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SECURITY_OFFICER')")
    public ResponseEntity<Void> logSecurityEvent(
            @RequestParam @NotNull SecurityMonitoringService.SecurityEventType eventType,
            @RequestParam @NotNull String description,
            @RequestParam(required = false) String ipAddress,
            @RequestParam(required = false) String userAgent,
            @RequestParam(required = false) UUID userId) {

        securityMonitoringService.recordSecurityEvent(eventType, description, ipAddress, userAgent, userId);
        return ResponseEntity.ok().build();
    }
}