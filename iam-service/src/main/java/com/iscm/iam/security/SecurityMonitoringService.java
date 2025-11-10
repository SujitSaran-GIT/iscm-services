package com.iscm.iam.security;

import com.iscm.iam.model.SuspiciousActivity;
import com.iscm.iam.model.User;
import com.iscm.iam.repository.SuspiciousActivityRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Service
@RequiredArgsConstructor
public class SecurityMonitoringService {

    private final SuspiciousActivityRepository suspiciousActivityRepository;
    private final RedisTemplate<String, String> redisTemplate;
    private final RateLimitingService rateLimitingService;

    @Value("${app.security.monitoring.enabled:true}")
    private boolean monitoringEnabled;

    @Value("${app.security.monitoring.alert-threshold-failed-logins:10}")
    private int failedLoginAlertThreshold;

    @Value("${app.security.monitoring.alert-threshold-suspicious-activities:20}")
    private int suspiciousActivityAlertThreshold;

    @Value("${app.security.monitoring.window-minutes:60}")
    private int monitoringWindowMinutes;

    // In-memory counters for real-time monitoring
    private final Map<String, AtomicLong> eventCounters = new ConcurrentHashMap<>();
    private final Map<String, LocalDateTime> lastAlertTimes = new ConcurrentHashMap<>();

    // Event types for monitoring
    public enum SecurityEventType {
        FAILED_LOGIN("failed_login"),
        SUCCESSFUL_LOGIN("successful_login"),
        PASSWORD_RESET("password_reset"),
        MFA_ATTEMPT("mfa_attempt"),
        ACCOUNT_LOCKED("account_locked"),
        SUSPICIOUS_ACTIVITY("suspicious_activity"),
        RATE_LIMIT_EXCEEDED("rate_limit_exceeded"),
        BRUTE_FORCE_DETECTED("brute_force_detected"),
        IP_CHANGE_DETECTED("ip_change_detected"),
        DEVICE_CHANGE_DETECTED("device_change_detected"),
        PRIVILEGE_ESCALATION("privilege_escalation"),
        DATA_ACCESS_ANOMALY("data_access_anomaly"),
        SECURITY_VIOLATION("security_violation");

        private final String value;

        SecurityEventType(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    /**
     * Record a security event
     */
    @Transactional
    public void recordSecurityEvent(SecurityEventType eventType, String description,
                                  String ipAddress, String userAgent, UUID userId) {
        if (!monitoringEnabled) {
            return;
        }

        try {
            // Log the event
            log.info("Security event recorded: type={}, description={}, ip={}, userId={}",
                    eventType, description, ipAddress, userId);

            // Update in-memory counter
            eventCounters.computeIfAbsent(eventType.getValue(), k -> new AtomicLong(0))
                        .incrementAndGet();

            // Store in Redis for real-time monitoring
            String redisKey = getRedisKey(eventType);
            redisTemplate.opsForValue().increment(redisKey);
            redisTemplate.expire(redisKey, Duration.ofHours(24));

            // Check if we need to trigger an alert
            checkAndTriggerAlert(eventType, ipAddress, userId);

            // Store in database for historical analysis
            SuspiciousActivity activity = new SuspiciousActivity();
            activity.setUserId(userId != null ? userId : UUID.fromString("00000000-0000-0000-0000-000000000000"));
            activity.setIpAddress(ipAddress != null ? ipAddress : "unknown");
            activity.setActivityType(eventType.getValue());
            activity.setDescription(description);
            activity.setSeverity(determineSeverity(eventType, description));
            activity.setTimestamp(LocalDateTime.now());
            activity.setInvestigated(false);

            suspiciousActivityRepository.save(activity);

        } catch (Exception e) {
            log.error("Failed to record security event", e);
        }
    }

    /**
     * Record a security event with user object
     */
    @Transactional
    public void recordSecurityEventWithUser(SecurityEventType eventType, String description,
                                  String ipAddress, String userAgent, User user) {
        recordSecurityEvent(eventType, description, ipAddress, userAgent,
                          user != null ? user.getId() : null);
    }

    /**
     * Check for potential brute force attacks
     */
    public void detectBruteForceAttack(String ipAddress, String email) {
        if (!monitoringEnabled) {
            return;
        }

        try {
            String key = "brute_force:" + ipAddress + ":" + email;
            String count = redisTemplate.opsForValue().get(key);

            if (count != null && Integer.parseInt(count) >= 5) {
                recordSecurityEvent(SecurityEventType.BRUTE_FORCE_DETECTED,
                    "Brute force attack detected from IP: " + ipAddress + " for email: " + email,
                    ipAddress, null, null);

                // Blacklist the IP temporarily
                rateLimitingService.recordFailedAttempt(ipAddress, "brute_force");
            }

        } catch (Exception e) {
            log.error("Failed to detect brute force attack", e);
        }
    }

    /**
     * Detect suspicious IP changes
     */
    public void detectIpChange(UUID userId, String newIpAddress, String previousIpAddress) {
        if (!monitoringEnabled || newIpAddress.equals(previousIpAddress)) {
            return;
        }

        try {
            // Check if this IP change is from a different geographic region
            // This is a simplified implementation - in practice, you'd use GeoIP lookup
            if (isSignificantIpChange(newIpAddress, previousIpAddress)) {
                recordSecurityEvent(SecurityEventType.IP_CHANGE_DETECTED,
                    String.format("Significant IP change detected for user %s: %s -> %s",
                        userId, previousIpAddress, newIpAddress),
                    newIpAddress, null, userId);
            }

        } catch (Exception e) {
            log.error("Failed to detect IP change", e);
        }
    }

    /**
     * Get security statistics
     */
    public SecurityStatistics getSecurityStatistics() {
        try {
            long totalEvents = eventCounters.values().stream()
                    .mapToLong(AtomicLong::get)
                    .sum();

            Map<String, Long> eventCounts = new ConcurrentHashMap<>();
            eventCounters.forEach((key, counter) -> eventCounts.put(key, counter.get()));

            // Get recent suspicious activities count
            long recentSuspiciousActivities = suspiciousActivityRepository
                    .count();

            // Get active threats
            long activeThreats = getActiveThreatsCount();

            return SecurityStatistics.builder()
                    .totalEvents(totalEvents)
                    .eventCounts(eventCounts)
                    .recentSuspiciousActivities((int) recentSuspiciousActivities)
                    .activeThreats((int) activeThreats)
                    .monitoringEnabled(monitoringEnabled)
                    .lastUpdated(LocalDateTime.now())
                    .build();

        } catch (Exception e) {
            log.error("Failed to get security statistics", e);
            return SecurityStatistics.builder()
                    .totalEvents(0)
                    .monitoringEnabled(monitoringEnabled)
                    .lastUpdated(LocalDateTime.now())
                    .build();
        }
    }

    /**
     * Get recent suspicious activities
     */
    @Transactional(readOnly = true)
    public Page<SuspiciousActivity> getRecentSuspiciousActivities(Pageable pageable) {
        return suspiciousActivityRepository.findAll(pageable);
    }

    /**
     * Mark suspicious activity as investigated
     */
    @Transactional
    public void markActivityAsInvestigated(UUID activityId) {
        suspiciousActivityRepository.findById(activityId).ifPresent(activity -> {
            activity.setInvestigated(true);
            suspiciousActivityRepository.save(activity);
        });
    }

    // Private helper methods

    private void checkAndTriggerAlert(SecurityEventType eventType, String ipAddress, UUID userId) {
        try {
            String alertKey = eventType.getValue() + ":" + (ipAddress != null ? ipAddress : "global");
            AtomicLong counter = eventCounters.get(eventType.getValue());

            if (counter == null) return;

            long currentCount = counter.get();
            LocalDateTime lastAlert = lastAlertTimes.get(alertKey);

            // Check if we should trigger an alert
            if (shouldTriggerAlert(eventType, currentCount, lastAlert)) {
                triggerAlert(eventType, ipAddress, userId, currentCount);
                lastAlertTimes.put(alertKey, LocalDateTime.now());
            }

        } catch (Exception e) {
            log.error("Failed to check and trigger alert", e);
        }
    }

    private boolean shouldTriggerAlert(SecurityEventType eventType, long currentCount, LocalDateTime lastAlert) {
        // Don't alert if we've already alerted in the last hour
        if (lastAlert != null && ChronoUnit.MINUTES.between(lastAlert, LocalDateTime.now()) < 60) {
            return false;
        }

        // Check threshold based on event type
        int threshold = switch (eventType) {
            case FAILED_LOGIN -> failedLoginAlertThreshold;
            case SUSPICIOUS_ACTIVITY -> suspiciousActivityAlertThreshold;
            case BRUTE_FORCE_DETECTED -> 1; // Always alert for brute force
            case RATE_LIMIT_EXCEEDED -> 5; // Alert after 5 rate limit violations
            default -> 20; // Default threshold for other events
        };

        return currentCount >= threshold;
    }

    private void triggerAlert(SecurityEventType eventType, String ipAddress, UUID userId, long count) {
        log.warn("SECURITY ALERT: {} event detected {} times from IP: {}, User: {}",
                eventType, count, ipAddress, userId);

        // In a real implementation, you would:
        // - Send email notifications
        // - Send Slack notifications
        // - Create alerts in monitoring systems
        // - Notify security team

        // Store alert in Redis for dashboard
        String alertKey = "security_alert:" + UUID.randomUUID().toString();
        String alertData = String.format("{\"type\":\"%s\",\"ip\":\"%s\",\"userId\":\"%s\",\"count\":%d,\"timestamp\":\"%s\"}",
                eventType, ipAddress, userId, count, LocalDateTime.now());

        redisTemplate.opsForValue().set(alertKey, alertData, Duration.ofHours(24));
    }

    private SuspiciousActivity.Severity determineSeverity(SecurityEventType eventType, String description) {
        return switch (eventType) {
            case BRUTE_FORCE_DETECTED, PRIVILEGE_ESCALATION, SECURITY_VIOLATION ->
                SuspiciousActivity.Severity.HIGH;
            case FAILED_LOGIN, SUSPICIOUS_ACTIVITY, IP_CHANGE_DETECTED, DEVICE_CHANGE_DETECTED ->
                SuspiciousActivity.Severity.MEDIUM;
            default -> SuspiciousActivity.Severity.LOW;
        };
    }

    private String getRedisKey(SecurityEventType eventType) {
        return "security:event:" + eventType.getValue();
    }

    private boolean isSignificantIpChange(String newIp, String previousIp) {
        // Simplified implementation - in practice, you'd use GeoIP database
        // to determine if the change is geographically significant

        // For now, just check if it's a completely different subnet
        String[] newParts = newIp.split("\\.");
        String[] prevParts = previousIp.split("\\.");

        if (newParts.length >= 3 && prevParts.length >= 3) {
            // Check if first three octets are different
            return !newParts[0].equals(prevParts[0]) ||
                   !newParts[1].equals(prevParts[1]) ||
                   !newParts[2].equals(prevParts[2]);
        }

        return !newIp.equals(previousIp);
    }

    private long getActiveThreatsCount() {
        try {
            // Simplified - count all high severity activities
            return suspiciousActivityRepository.findAll().stream()
                    .filter(activity -> activity.getSeverity() == SuspiciousActivity.Severity.HIGH &&
                                       !Boolean.TRUE.equals(activity.getInvestigated()))
                    .count();
        } catch (Exception e) {
            log.error("Failed to get active threats count", e);
            return 0;
        }
    }

    // Scheduled cleanup task
    @Scheduled(cron = "0 0 3 * * ?") // Run daily at 3 AM
    @Transactional
    public void cleanupOldSecurityData() {
        try {
            // Clean up old security events from Redis
            // (Redis TTL handles this automatically, but we can clean up in-memory counters)

            // Reset counters that are too old
            LocalDateTime cutoff = LocalDateTime.now().minusDays(7);
            lastAlertTimes.entrySet().removeIf(entry -> entry.getValue().isBefore(cutoff));

            log.info("Security monitoring cleanup completed");

        } catch (Exception e) {
            log.error("Failed to cleanup old security data", e);
        }
    }

    // DTO for security statistics
    @lombok.Data
    @lombok.Builder
    public static class SecurityStatistics {
        private long totalEvents;
        private Map<String, Long> eventCounts;
        private int recentSuspiciousActivities;
        private int activeThreats;
        private boolean monitoringEnabled;
        private LocalDateTime lastUpdated;
    }
}