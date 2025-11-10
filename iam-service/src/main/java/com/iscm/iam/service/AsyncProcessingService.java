package com.iscm.iam.service;

import com.iscm.iam.model.User;
import com.iscm.iam.model.UserSession;
import com.iscm.iam.repository.UserRepository;
import com.iscm.iam.security.SecurityMonitoringService;
import com.iscm.iam.cache.CacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

@Slf4j
@Service
@RequiredArgsConstructor
public class AsyncProcessingService {

    private final UserRepository userRepository;
    private final UserSessionService sessionService;
    private final SecurityMonitoringService securityMonitoringService;
    private final CacheService cacheService;
    private final EmailService emailService;

    @Value("${app.async.batch-size:100}")
    private int batchSize;

    // ========== Security Event Processing ==========

    @Async("securityEventExecutor")
    public void processSecurityEventAsync(SecurityMonitoringService.SecurityEventType eventType,
                                        String description, String ipAddress, String userAgent, UUID userId) {
        try {
            log.debug("Processing security event asynchronously: {} for user: {}", eventType, userId);
            securityMonitoringService.recordSecurityEvent(eventType, description, ipAddress, userAgent, userId);
        } catch (Exception e) {
            log.error("Failed to process security event asynchronously", e);
        }
    }

    @Async("securityEventExecutor")
    public void processFailedLoginAttemptsBatch(List<String> emails, String ipAddress) {
        try {
            log.debug("Processing batch of {} failed login attempts", emails.size());
            emails.forEach(email -> {
                securityMonitoringService.detectBruteForceAttack(ipAddress, email);
                cacheService.cacheFailedLoginAttempts(email,
                    cacheService.getFailedLoginAttempts(email) + 1);
            });
        } catch (Exception e) {
            log.error("Failed to process batch of failed login attempts", e);
        }
    }

    // ========== User Data Processing ==========

    @Async("taskExecutor")
    public CompletableFuture<Void> updateUserLastLoginAsync(UUID userId) {
        try {
            log.debug("Updating last login time for user: {}", userId);
            userRepository.updateLastLogin(userId, LocalDateTime.now());
            cacheService.evictUser(userId);
            return CompletableFuture.completedFuture(null);
        } catch (Exception e) {
            log.error("Failed to update last login for user: {}", userId, e);
            return CompletableFuture.failedFuture(e);
        }
    }

    @Async("taskExecutor")
    public CompletableFuture<Void> resetFailedAttemptsAsync(UUID userId) {
        try {
            log.debug("Resetting failed login attempts for user: {}", userId);
            userRepository.resetFailedAttempts(userId);
            cacheService.evictUser(userId);
            return CompletableFuture.completedFuture(null);
        } catch (Exception e) {
            log.error("Failed to reset failed attempts for user: {}", userId, e);
            return CompletableFuture.failedFuture(e);
        }
    }

    @Async("taskExecutor")
    public void processUserRegistrationAsync(User user, String ipAddress) {
        try {
            log.debug("Processing post-registration tasks for user: {}", user.getId());

            // Cache the user
            cacheService.cacheUser(user);

            // Record security event
            securityMonitoringService.recordSecurityEventWithUser(
                SecurityMonitoringService.SecurityEventType.SUCCESSFUL_LOGIN,
                "User registered successfully",
                ipAddress,
                null,
                user
            );

            // Send welcome email asynchronously
            emailService.sendWelcomeEmail(user);

        } catch (Exception e) {
            log.error("Failed to process user registration tasks for user: {}", user.getId(), e);
        }
    }

    // ========== Session Management ==========

    @Async("taskExecutor")
    public void processSessionCreationAsync(UserSession session) {
        try {
            log.debug("Processing session creation tasks for session: {}", session.getId());

            // Cache active session
            cacheService.cacheActiveSession(
                session.getUser().getId(),
                session.getId().toString(),
                session.getIpAddress()
            );

            // Clean up old sessions for the user
            List<UserSession> activeSessions = sessionService.getActiveSessions(session.getUser().getId());
            if (activeSessions.size() > 5) {
                // Revoke oldest sessions if too many
                activeSessions.stream()
                    .sorted((s1, s2) -> s1.getCreatedAt().compareTo(s2.getCreatedAt()))
                    .limit(activeSessions.size() - 5)
                    .forEach(s -> sessionService.revokeSession(s.getId()));
            }

        } catch (Exception e) {
            log.error("Failed to process session creation for session: {}", session.getId(), e);
        }
    }

    @Async("taskExecutor")
    public void processSessionCleanupAsync(UUID userId) {
        try {
            log.debug("Processing session cleanup for user: {}", userId);

            // Clear user cache
            cacheService.clearUserCache(userId);

            // Clean up old sessions
            sessionService.revokeAllUserSessions(userId);

        } catch (Exception e) {
            log.error("Failed to process session cleanup for user: {}", userId, e);
        }
    }

    // ========== Audit and Logging ==========

    @Async("auditExecutor")
    public void logAuditEventAsync(String eventType, String entityId, String userId,
                                 String details, String ipAddress) {
        try {
            log.debug("Logging audit event: {} for entity: {} by user: {}", eventType, entityId, userId);

            // This could be extended to write to an audit log table or external system
            String auditMessage = String.format("[%s] Event: %s, Entity: %s, User: %s, IP: %s, Details: %s",
                LocalDateTime.now(), eventType, entityId, userId, ipAddress, details);

            log.info("AUDIT: {}", auditMessage);

            // Store in cache for recent audit events
            cacheService.cacheSecurityEvent(
                userId != null ? UUID.fromString(userId) : null,
                eventType,
                auditMessage
            );

        } catch (Exception e) {
            log.error("Failed to log audit event asynchronously", e);
        }
    }

    // ========== Cleanup and Maintenance ==========

    @Async("cleanupExecutor")
    public CompletableFuture<Integer> cleanupExpiredSessionsAsync() {
        try {
            log.debug("Starting cleanup of expired sessions");

            sessionService.cleanupExpiredSessions();
            int deletedCount = 0; // The cleanupExpiredSessions method doesn't return a count

            log.info("Cleaned up {} expired sessions", deletedCount);
            return CompletableFuture.completedFuture(deletedCount);

        } catch (Exception e) {
            log.error("Failed to cleanup expired sessions", e);
            return CompletableFuture.failedFuture(e);
        }
    }

    @Async("cleanupExecutor")
    public CompletableFuture<Integer> cleanupInactiveUsersAsync() {
        try {
            log.debug("Starting cleanup of inactive users");

            LocalDateTime cutoff = LocalDateTime.now().minusMonths(12);
            int deletedCount = userRepository.deleteInactiveUsers(cutoff);

            log.info("Cleaned up {} inactive users", deletedCount);
            return CompletableFuture.completedFuture(deletedCount);

        } catch (Exception e) {
            log.error("Failed to cleanup inactive users", e);
            return CompletableFuture.failedFuture(e);
        }
    }

    @Async("cleanupExecutor")
    public void refreshStatisticsAsync() {
        try {
            log.debug("Refreshing system statistics");

            // This could trigger various statistics refresh operations
            // For example, updating materialized views, clearing old cache entries, etc.

            cacheService.clearAllCache();
            log.info("System statistics refreshed");

        } catch (Exception e) {
            log.error("Failed to refresh statistics", e);
        }
    }

    // ========== Email Notifications ==========

    @Async("emailExecutor")
    public void sendLoginNotificationAsync(User user, String ipAddress, String userAgent) {
        try {
            log.debug("Sending login notification to user: {}", user.getId());
            emailService.sendLoginNotification(user, ipAddress, userAgent);
        } catch (Exception e) {
            log.error("Failed to send login notification to user: {}", user.getId(), e);
        }
    }

    @Async("emailExecutor")
    public void sendSecurityAlertAsync(User user, String alertType, String details) {
        try {
            log.debug("Sending security alert to user: {} for alert type: {}", user.getId(), alertType);
            emailService.sendSecurityAlert(user, alertType, details);
        } catch (Exception e) {
            log.error("Failed to send security alert to user: {}", user.getId(), e);
        }
    }

    // ========== Health Check ==========

    @Async("taskExecutor")
    public CompletableFuture<Boolean> performHealthCheckAsync() {
        try {
            log.debug("Performing async health check");

            // Check database connectivity
            long userCount = userRepository.count();

            // Check cache health
            boolean cacheHealthy = cacheService.isCacheHealthy();

            // Check session service
            long sessionCount = sessionService.getSessionStatistics().getActiveSessionsCount();

            boolean healthy = userCount >= 0 && cacheHealthy && sessionCount >= 0;

            log.debug("Health check completed - Database: {}, Cache: {}, Sessions: {}",
                     userCount, cacheHealthy, sessionCount);

            return CompletableFuture.completedFuture(healthy);

        } catch (Exception e) {
            log.error("Health check failed", e);
            return CompletableFuture.completedFuture(false);
        }
    }

    // ========== Batch Operations ==========

    @Async("taskExecutor")
    public CompletableFuture<Integer> processBatchUserUpdates(List<UUID> userIds) {
        try {
            log.debug("Processing batch update for {} users", userIds.size());

            int processedCount = 0;
            for (int i = 0; i < userIds.size(); i += batchSize) {
                List<UUID> batch = userIds.subList(i, Math.min(i + batchSize, userIds.size()));

                // Process batch
                batch.forEach(userId -> {
                    cacheService.evictUser(userId);
                    // Add other batch operations here
                });

                processedCount += batch.size();
                log.debug("Processed batch of {} users, total: {}", batch.size(), processedCount);
            }

            return CompletableFuture.completedFuture(processedCount);

        } catch (Exception e) {
            log.error("Failed to process batch user updates", e);
            return CompletableFuture.failedFuture(e);
        }
    }
}