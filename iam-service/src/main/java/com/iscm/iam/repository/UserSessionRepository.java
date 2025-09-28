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
}