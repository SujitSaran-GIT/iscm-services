package com.iscm.iam.service;

import com.iscm.iam.model.LoginAttempt;
import com.iscm.iam.model.SuspiciousActivity;
import com.iscm.iam.repository.LoginAttemptRepository;
import com.iscm.iam.repository.SuspiciousActivityRepository;
import com.iscm.iam.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class FraudDetectionService {

    private final UserRepository userRepository;
    private final LoginAttemptRepository loginAttemptRepository;
    private final SuspiciousActivityRepository suspiciousActivityRepository;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${app.fraud.detection.enabled:false}")
    private boolean fraudDetectionEnabled;

    @Value("${app.fraud.service.url:}")
    private String fraudServiceUrl;

    @Value("${app.fraud.threshold.failed-attempts:5}")
    private int failedAttemptThreshold;

    @Value("${app.fraud.threshold.ip-changes:3}")
    private int ipChangeThreshold;

    @Value("${app.fraud.threshold.time-window-minutes:60}")
    private int timeWindowMinutes;

    @Transactional
    public void recordLoginAttempt(UUID userId, String ipAddress, String userAgent, boolean successful) {
        if (!fraudDetectionEnabled) {
            return;
        }

        LoginAttempt attempt = new LoginAttempt();
        attempt.setUserId(userId);
        attempt.setIpAddress(ipAddress);
        attempt.setUserAgent(userAgent);
        attempt.setSuccessful(successful);
        attempt.setAttemptTime(LocalDateTime.now());

        loginAttemptRepository.save(attempt);

        if (!successful) {
            analyzeFailedLoginPattern(userId, ipAddress);
        }

        // Hook to external fraud service
        notifyFraudService(userId, ipAddress, userAgent, successful);
    }

    @Transactional
    public boolean isSuspiciousLoginAttempt(UUID userId, String ipAddress, String userAgent) {
        if (!fraudDetectionEnabled) {
            return false;
        }

        // Check for rapid failed attempts
        List<LoginAttempt> recentAttempts = loginAttemptRepository
                .findByUserIdAndAttemptTimeAfterAndSuccessfulFalse(
                        userId,
                        LocalDateTime.now().minusMinutes(timeWindowMinutes)
                );

        if (recentAttempts.size() >= failedAttemptThreshold) {
            logSuspiciousActivity(userId, ipAddress, "Multiple failed login attempts");
            return true;
        }

        // Check for IP address changes
        List<LoginAttempt> previousLogins = loginAttemptRepository
                .findByUserIdAndSuccessfulTrueOrderByAttemptTimeDesc(userId);

        if (!previousLogins.isEmpty()) {
            String lastIpAddress = previousLogins.get(0).getIpAddress();
            if (!lastIpAddress.equals(ipAddress)) {
                // Check if this IP has been used before
                boolean ipUsedBefore = loginAttemptRepository
                        .findByUserIdAndIpAddress(userId, ipAddress)
                        .stream()
                        .anyMatch(attempt -> attempt.getSuccessful());

                if (!ipUsedBefore) {
                    logSuspiciousActivity(userId, ipAddress, "Login from new IP address");
                    return true;
                }
            }
        }

        // Check for suspicious user agent patterns
        if (isSuspiciousUserAgent(userAgent)) {
            logSuspiciousActivity(userId, ipAddress, "Suspicious user agent");
            return true;
        }

        return false;
    }

    @Transactional
    public boolean isSuspiciousResetAttempt(UUID userId, String ipAddress, String userAgent) {
        if (!fraudDetectionEnabled) {
            return false;
        }

        // Check for multiple reset attempts in short time
        LocalDateTime timeWindow = LocalDateTime.now().minusMinutes(30);
        List<SuspiciousActivity> recentResets = suspiciousActivityRepository
                .findByUserIdAndActivityTypeAndTimestampAfter(
                        userId, "PASSWORD_RESET_ATTEMPT", timeWindow
                );

        if (recentResets.size() >= 3) {
            logSuspiciousActivity(userId, ipAddress, "Multiple password reset attempts");
            return true;
        }

        return false;
    }

    @Transactional
    public void logPasswordReset(UUID userId, String ipAddress, boolean successful) {
        if (!fraudDetectionEnabled) {
            return;
        }

        String activityType = successful ? "PASSWORD_RESET_SUCCESS" : "PASSWORD_RESET_FAILURE";
        logSuspiciousActivity(userId, ipAddress, activityType);
    }

    private void analyzeFailedLoginPattern(UUID userId, String ipAddress) {
        List<LoginAttempt> failedAttempts = loginAttemptRepository
                .findByUserIdAndIpAddressAndSuccessfulFalseOrderByAttemptTimeDesc(
                        userId, ipAddress
                );

        if (failedAttempts.size() >= failedAttemptThreshold) {
            String description = String.format("Suspicious activity: %d failed attempts from IP %s",
                    failedAttempts.size(), ipAddress);
            logSuspiciousActivity(userId, ipAddress, description);
        }
    }

    private void logSuspiciousActivity(UUID userId, String ipAddress, String activityType) {
        SuspiciousActivity activity = new SuspiciousActivity();
        activity.setUserId(userId);
        activity.setIpAddress(ipAddress);
        activity.setActivityType(activityType);
        activity.setTimestamp(LocalDateTime.now());
        activity.setSeverity(SuspiciousActivity.Severity.valueOf(calculateSeverity(activityType)));

        suspiciousActivityRepository.save(activity);

        log.warn("Suspicious activity detected - User: {}, IP: {}, Type: {}",
                userId, ipAddress, activityType);
    }

    private String calculateSeverity(String activityType) {
        return switch (activityType) {
            case "Multiple failed login attempts" -> "HIGH";
            case "Login from new IP address" -> "MEDIUM";
            case "Suspicious user agent" -> "LOW";
            case "Multiple password reset attempts" -> "HIGH";
            default -> "MEDIUM";
        };
    }

    private boolean isSuspiciousUserAgent(String userAgent) {
        if (userAgent == null) {
            return true;
        }

        String lowerUserAgent = userAgent.toLowerCase();
        return lowerUserAgent.contains("bot") ||
               lowerUserAgent.contains("crawler") ||
               lowerUserAgent.contains("spider") ||
               lowerUserAgent.length() < 10;
    }

    private void notifyFraudService(UUID userId, String ipAddress, String userAgent, boolean successful) {
        if (!fraudDetectionEnabled || fraudServiceUrl == null || fraudServiceUrl.isEmpty()) {
            return;
        }

        try {
            Map<String, Object> payload = Map.of(
                "userId", userId.toString(),
                "ipAddress", ipAddress,
                "userAgent", userAgent,
                "successful", successful,
                "timestamp", LocalDateTime.now().toString(),
                "service", "IAM"
            );

            restTemplate.postForLocation(fraudServiceUrl + "/api/v1/events", payload);
        } catch (Exception e) {
            log.error("Failed to notify fraud service: {}", e.getMessage());
            // Don't fail the authentication if fraud service is unavailable
        }
    }

    @Transactional
    public List<SuspiciousActivity> getUserSuspiciousActivity(UUID userId, int days) {
        LocalDateTime startDate = LocalDateTime.now().minusDays(days);
        return suspiciousActivityRepository.findByUserIdAndTimestampAfterOrderByTimestampDesc(userId, startDate);
    }

    @Transactional
    public Map<String, Long> getSecurityStats(UUID userId) {
        LocalDateTime since = LocalDateTime.now().minusDays(30);

        long totalAttempts = loginAttemptRepository.countByUserIdAndAttemptTimeAfter(userId, since);
        long failedAttempts = loginAttemptRepository.countByUserIdAndAttemptTimeAfterAndSuccessfulFalse(userId, since);
        long suspiciousActivities = suspiciousActivityRepository.countByUserIdAndTimestampAfter(userId, since);

        return Map.of(
            "totalAttempts", totalAttempts,
            "failedAttempts", failedAttempts,
            "suspiciousActivities", suspiciousActivities
        );
    }
}