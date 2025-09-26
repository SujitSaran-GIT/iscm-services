package com.iscm.iam.service;

import com.iscm.iam.model.User;
import com.iscm.iam.model.UserSession;
import com.iscm.iam.repository.UserSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserSessionService {

    private final UserSessionRepository sessionRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public UserSession createSession(User user, String refreshToken, String userAgent, String ipAddress) {
        // Hash the refresh token before storing
        String refreshTokenHash = passwordEncoder.encode(refreshToken);

        UserSession session = new UserSession();
        session.setUser(user);
        session.setRefreshTokenHash(refreshTokenHash);
        session.setUserAgent(userAgent);
        session.setIpAddress(ipAddress);
        session.setExpiresAt(LocalDateTime.now().plusDays(7)); // 7 days expiration
        session.setRevoked(false);

        return sessionRepository.save(session);
    }

    @Transactional
    public UserSession validateRefreshToken(String refreshToken) {
        // We need to find the session by comparing the raw token with hashed tokens
        // This is inefficient but necessary for security
        var activeSessions = sessionRepository.findAll().stream()
                .filter(session -> !session.getRevoked() && 
                                 session.getExpiresAt().isAfter(LocalDateTime.now()))
                .toList();

        for (UserSession session : activeSessions) {
            if (passwordEncoder.matches(refreshToken, session.getRefreshTokenHash())) {
                return session;
            }
        }

        throw new SecurityException("Invalid or expired refresh token");
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
    public void revokeAllUserSessions(UUID userId) {
        sessionRepository.revokeAllUserSessions(userId);
    }

    @Scheduled(cron = "0 0 2 * * ?") // Run daily at 2 AM
    @Transactional
    public void cleanupExpiredSessions() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(30); // Keep logs for 30 days
        sessionRepository.deleteExpiredSessions(cutoff);
        log.info("Cleaned up expired user sessions");
    }
}