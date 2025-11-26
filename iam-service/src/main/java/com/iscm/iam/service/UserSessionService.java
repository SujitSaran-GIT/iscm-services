package com.iscm.iam.service;

// import com.iscm.iam.cache.CacheService;
import com.iscm.iam.model.User;
import com.iscm.iam.model.UserSession;
import com.iscm.iam.repository.UserSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
// import org.springframework.cache.annotation.CacheEvict;
// import org.springframework.cache.annotation.Cacheable;
// import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserSessionService {

    private final UserSessionRepository sessionRepository;
    private final PasswordEncoder passwordEncoder;
    // private final CacheService cacheService;

    @Value("${app.session.max-concurrent-sessions:5}")
    private int maxConcurrentSessions;

    @Value("${app.session.cleanup.threshold:100}")
    private int cleanupThreshold;

    // In-memory cache for frequently accessed sessions
    private final ConcurrentHashMap<String, UserSession> sessionCache = new ConcurrentHashMap<>();

    // Session statistics
    private volatile long totalSessionsCreated = 0;
    private volatile long activeSessionsCount = 0;

    @Transactional
    public UserSession createSession(User user, String refreshToken, String userAgent, String ipAddress) {
        log.debug("Creating session for user: {} from IP: {}", user.getId(), ipAddress);

        // Check concurrent session limit
        List<UserSession> activeSessions = getActiveSessions(user.getId());
        if (activeSessions.size() >= maxConcurrentSessions) {
            // Revoke oldest session
            UserSession oldestSession = activeSessions.get(0);
            revokeSession(oldestSession.getId().toString());
            log.info("Revoked oldest session for user {} due to concurrent session limit", user.getId());
        }

        // Hash the refresh token before storing
        String refreshTokenHash = passwordEncoder.encode(refreshToken);

        UserSession session = new UserSession();
        session.setUser(user);
        session.setRefreshTokenHash(refreshTokenHash);
        session.setUserAgent(userAgent);
        session.setIpAddress(ipAddress);
        session.setCreatedAt(LocalDateTime.now());
        session.setExpiresAt(LocalDateTime.now().plusDays(7)); // 7 days expiration
        // Note: lastAccessedAt field removed - using createdAt instead
        session.setRevoked(false);

        UserSession savedSession = sessionRepository.save(session);

        // Cache the session
        String cacheKey = generateSessionKey(refreshToken);
        sessionCache.put(cacheKey, savedSession);

        // Cache active session info
        // cacheService.cacheActiveSession(user.getId(), savedSession.getId().toString(), ipAddress);

        // Update statistics
        totalSessionsCreated++;
        activeSessionsCount++;

        log.debug("Session created successfully: {} for user: {}", savedSession.getId(), user.getId());
        return savedSession;
    }

    @Transactional
    // @Cacheable(value = "sessions", key = "#refreshToken")
    public UserSession validateRefreshToken(String refreshToken) {
        log.debug("Validating refresh token");

        // First check in-memory cache
        String cacheKey = generateSessionKey(refreshToken);
        UserSession cachedSession = sessionCache.get(cacheKey);
        if (cachedSession != null && isSessionValid(cachedSession)) {
            return cachedSession;
        }

        // OPTIMIZED: Use efficient query to get only active sessions
        // This is much better than full table scan, but we still need to check password matches
        var activeSessions = sessionRepository.findByUserIdAndRevokedFalse(null);
        LocalDateTime now = LocalDateTime.now();

        for (UserSession session : activeSessions) {
            if (session.getExpiresAt().isAfter(now) &&
                passwordEncoder.matches(refreshToken, session.getRefreshTokenHash())) {
                // Cache the found session
                sessionCache.put(cacheKey, session);
                return session;
            }
        }

        throw new SecurityException("Invalid or expired refresh token");
    }

    @Transactional
    public UserSession findSessionByRefreshToken(String refreshToken) {
        // OPTIMIZED: Use efficient query to get only active sessions
        var activeSessions = sessionRepository.findByUserIdAndRevokedFalse(null);
        LocalDateTime now = LocalDateTime.now();

        for (UserSession session : activeSessions) {
            if (session.getExpiresAt().isAfter(now) &&
                passwordEncoder.matches(refreshToken, session.getRefreshTokenHash())) {
                return session;
            }
        }

        return null; // Return null instead of throwing exception for logout operations
    }

    @Transactional
    public void updateSession(UserSession session, String newRefreshToken) {
        String newRefreshTokenHash = passwordEncoder.encode(newRefreshToken);
        session.setRefreshTokenHash(newRefreshTokenHash);
        session.setExpiresAt(LocalDateTime.now().plusDays(7));
        sessionRepository.save(session);
    }

    @Transactional
    public void revokeSession(String refreshToken) {
        try {
            UserSession session = validateRefreshToken(refreshToken);
            session.setRevoked(true);
            sessionRepository.save(session);
        } catch (SecurityException ex) {
            log.warn("Attempt to revoke invalid refresh token");
        }
    }

    @Transactional
    public void revokeAllUserSessionsLegacy(UUID userId) {
        sessionRepository.revokeAllUserSessions(userId);
    }

    // @Scheduled(cron = "0 0 2 * * ?") // DISABLED - Run daily at 2 AM
    @Transactional
    public void cleanupExpiredSessions() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(30); // Keep logs for 30 days
        sessionRepository.deleteExpiredSessions(cutoff);

        // Clear in-memory cache of expired sessions
        sessionCache.entrySet().removeIf(entry -> !isSessionValid(entry.getValue()));

        // Update active sessions count
        updateActiveSessionsCount();

        log.info("Cleaned up expired user sessions. Current active sessions: {}", activeSessionsCount);
    }

    // ========== Optimized Session Management Methods ==========

    // @Cacheable(value = "activeSessions", key = "#userId")
    public List<UserSession> getActiveSessions(UUID userId) {
        return sessionRepository.findActiveSessionsByUser(userId, LocalDateTime.now());
    }

    @Transactional
    // @CacheEvict(value = {"sessions", "activeSessions"}, allEntries = true)
    public void revokeSession(UUID sessionId) {
        sessionRepository.revokeSession(sessionId);
        // Remove from in-memory cache
        sessionCache.entrySet().removeIf(entry -> entry.getValue().getId().equals(sessionId));
        activeSessionsCount--;
    }

    @Transactional
    // @CacheEvict(value = {"sessions", "activeSessions"}, key = "#userId")
    public void revokeAllUserSessions(UUID userId) {
        sessionRepository.revokeAllUserSessions(userId);
        // Remove from in-memory cache
        sessionCache.entrySet().removeIf(entry -> entry.getValue().getUser().getId().equals(userId));
        // cacheService.evictActiveSession(userId);
        updateActiveSessionsCount();
    }

    // Note: updateLastAccessed method removed since UserSession doesn't have lastAccessedAt field
    // Can be re-added if the field is added to the entity in the future

    public SessionStatistics getSessionStatistics() {
        return SessionStatistics.builder()
                .totalSessionsCreated(totalSessionsCreated)
                .activeSessionsCount(activeSessionsCount)
                .maxConcurrentSessions(maxConcurrentSessions)
                .inMemoryCacheSize(sessionCache.size())
                .build();
    }

    // ========== Helper Methods ==========

    private String generateSessionKey(String refreshToken) {
        return "session:" + refreshToken.hashCode();
    }

    private boolean isSessionValid(UserSession session) {
        return !session.getRevoked() && session.getExpiresAt().isAfter(LocalDateTime.now());
    }

    private void updateActiveSessionsCount() {
        try {
            activeSessionsCount = sessionRepository.countActiveSessions(LocalDateTime.now());
        } catch (Exception e) {
            log.warn("Failed to update active sessions count", e);
        }
    }

    // ========== Batch Operations ==========

    @Transactional
    // @Scheduled(fixedRate = 300000) // DISABLED - Every 5 minutes
    public void performBatchCleanup() {
        try {
            // Clean up in-memory cache if it gets too large
            if (sessionCache.size() > cleanupThreshold) {
                // Remove least recently used entries (simplified)
                sessionCache.entrySet().removeIf(entry -> !isSessionValid(entry.getValue()));
                log.debug("Cleaned up in-memory session cache. Size: {}", sessionCache.size());
            }

            // Update statistics
            updateActiveSessionsCount();

        } catch (Exception e) {
            log.error("Error during batch cleanup", e);
        }
    }

    // ========== Inner Classes ==========

    @lombok.Data
    @lombok.Builder
    public static class SessionStatistics {
        private long totalSessionsCreated;
        private long activeSessionsCount;
        private int maxConcurrentSessions;
        private int inMemoryCacheSize;
    }
}