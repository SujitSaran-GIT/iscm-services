package com.iscm.iam.repository;

import com.iscm.iam.model.UserSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserSessionRepository extends JpaRepository<UserSession, UUID> {
    
    Optional<UserSession> findByRefreshTokenHashAndRevokedFalse(String refreshTokenHash);
    
    List<UserSession> findByUserIdAndRevokedFalse(UUID userId);
    
    @Query("SELECT us FROM UserSession us WHERE us.expiresAt < :now AND us.revoked = false")
    List<UserSession> findExpiredSessions(@Param("now") LocalDateTime now);
    
    @Modifying
    @Query("UPDATE UserSession us SET us.revoked = true WHERE us.user.id = :userId")
    void revokeAllUserSessions(@Param("userId") UUID userId);
    
    @Modifying
    @Query("UPDATE UserSession us SET us.revoked = true WHERE us.id = :sessionId")
    void revokeSession(@Param("sessionId") UUID sessionId);
    
    @Modifying
    @Query("DELETE FROM UserSession us WHERE us.expiresAt < :cutoff")
    void deleteExpiredSessions(@Param("cutoff") LocalDateTime cutoff);

    void deleteByUserId(UUID userId);

    // ========== Performance Optimized Queries ==========

    // Critical fix: Find session by refresh token hash without full table scan
    @Query("SELECT us FROM UserSession us WHERE us.refreshTokenHash = :tokenHash AND us.revoked = false AND us.expiresAt > :now")
    Optional<UserSession> findByRefreshTokenHashAndValid(@Param("tokenHash") String tokenHash, @Param("now") LocalDateTime now);

    // Optimized session validation queries
    @Query("SELECT us FROM UserSession us WHERE us.user.id = :userId AND us.revoked = false AND us.expiresAt > :now ORDER BY us.createdAt DESC")
    List<UserSession> findActiveSessionsByUser(@Param("userId") UUID userId, @Param("now") LocalDateTime now);

    @Query("SELECT us FROM UserSession us WHERE us.ipAddress = :ipAddress AND us.revoked = false AND us.expiresAt > :now")
    List<UserSession> findActiveSessionsByIp(@Param("ipAddress") String ipAddress, @Param("now") LocalDateTime now);

    @Query(value = """
        SELECT us.* FROM user_sessions us
        WHERE us.user_id = :userId
        AND us.revoked = false
        AND us.expires_at > :now
        ORDER BY us.created_at DESC
        LIMIT :limit
        """, nativeQuery = true)
    List<UserSession> findRecentActiveSessionsByUserNative(@Param("userId") UUID userId, @Param("now") LocalDateTime now, @Param("limit") int limit);

    // Batch operations for better performance
    @Modifying
    @Query("UPDATE UserSession us SET us.revoked = true WHERE us.user.id IN :userIds")
    int revokeSessionsForUsers(@Param("userIds") List<UUID> userIds);

    // Note: updateLastAccessed removed since UserSession entity doesn't have lastAccessedAt field
    // Can be re-added if the field is added to the entity in the future

    // Performance monitoring queries
    @Query("SELECT COUNT(us) FROM UserSession us WHERE us.revoked = false AND us.expiresAt > :now")
    long countActiveSessions(@Param("now") LocalDateTime now);

    @Query("SELECT COUNT(DISTINCT us.user.id) FROM UserSession us WHERE us.revoked = false AND us.expiresAt > :now")
    long countActiveUsersWithSessions(@Param("now") LocalDateTime now);

    @Query("SELECT us.ipAddress, COUNT(us) as sessionCount FROM UserSession us WHERE us.revoked = false AND us.expiresAt > :now GROUP BY us.ipAddress HAVING COUNT(us) > :threshold")
    List<Object[]> findIpsWithMultipleSessions(@Param("now") LocalDateTime now, @Param("threshold") int threshold);

    // Cleanup and maintenance queries
    @Modifying
    @Query("DELETE FROM UserSession us WHERE us.revoked = true AND us.expiresAt < :cutoffDate")
    int cleanupOldRevokedSessions(@Param("cutoffDate") LocalDateTime cutoffDate);

    @Modifying
    @Query(value = """
        DELETE FROM user_sessions
        WHERE (revoked = true AND expires_at < :cutoffDate)
        OR (revoked = false AND expires_at < :cleanupDate)
        """, nativeQuery = true)
    int bulkCleanupSessions(@Param("cutoffDate") LocalDateTime cutoffDate, @Param("cleanupDate") LocalDateTime cleanupDate);

    // Session analytics queries
    @Query("SELECT DATE(us.createdAt) as sessionDate, COUNT(us) as sessionCount FROM UserSession us WHERE us.createdAt >= :startDate GROUP BY DATE(us.createdAt) ORDER BY sessionDate DESC")
    List<Object[]> getSessionStatsByDate(@Param("startDate") LocalDateTime startDate);

    @Query(value = """
        SELECT
            DATE_TRUNC('hour', created_at) as hour_bucket,
            COUNT(*) as sessions_created,
            COUNT(*) FILTER (WHERE revoked = false) as active_sessions
        FROM user_sessions
        WHERE created_at >= :startDate
        GROUP BY DATE_TRUNC('hour', created_at)
        ORDER BY hour_bucket DESC
        """, nativeQuery = true)
    List<Object[]> getSessionHourlyStats(@Param("startDate") LocalDateTime startDate);

    // Security monitoring queries
    @Query("SELECT us FROM UserSession us WHERE us.userAgent LIKE :userAgentPattern AND us.revoked = false AND us.expiresAt > :now")
    List<UserSession> findSessionsByUserAgentPattern(@Param("userAgentPattern") String userAgentPattern, @Param("now") LocalDateTime now);

    @Query("SELECT us FROM UserSession us WHERE us.createdAt < :inactiveThreshold AND us.revoked = false AND us.expiresAt > :now")
    List<UserSession> findInactiveSessions(@Param("inactiveThreshold") LocalDateTime inactiveThreshold, @Param("now") LocalDateTime now);

    // Tenant-aware session queries
    @Query("SELECT us FROM UserSession us JOIN us.user u WHERE u.tenantId = :tenantId AND us.revoked = false AND us.expiresAt > :now")
    List<UserSession> findActiveSessionsByTenant(@Param("tenantId") UUID tenantId, @Param("now") LocalDateTime now);
}