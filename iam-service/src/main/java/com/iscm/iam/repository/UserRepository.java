package com.iscm.iam.repository;

import com.iscm.iam.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
public interface UserRepository extends JpaRepository<User, UUID> {
    
    Optional<User> findByEmail(String email);
    
    Boolean existsByEmail(String email);
    
    @Query("SELECT u FROM User u LEFT JOIN FETCH u.userRoles ur LEFT JOIN FETCH ur.role WHERE u.email = :email")
    Optional<User> findByEmailWithRoles(@Param("email") String email);

    // Fix MultipleBagFetchException by using separate queries or fixing the JOIN structure
    @Query("SELECT u FROM User u LEFT JOIN FETCH u.userRoles ur LEFT JOIN FETCH ur.role WHERE u.email = :email AND u.isActive = true")
    Optional<User> findByEmailWithAllDetails(@Param("email") String email);

    @Query("SELECT u FROM User u LEFT JOIN FETCH u.userRoles ur LEFT JOIN FETCH ur.role WHERE u.id = :id AND u.isActive = true")
    Optional<User> findByIdWithAllDetails(@Param("id") UUID id);

    
    @Query("SELECT u FROM User u LEFT JOIN FETCH u.userRoles ur LEFT JOIN FETCH ur.role WHERE u.id = :id")
    Optional<User> findByIdWithRoles(@Param("id") UUID id);
    
    @Query("SELECT COUNT(u) > 0 FROM User u WHERE u.email = :email AND u.id != :id")
    Boolean existsByEmailAndIdNot(@Param("email") String email, @Param("id") UUID id);

    // Search methods
    Page<User> findByEmailContainingIgnoreCaseOrFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCase(
            String email, String firstName, String lastName, Pageable pageable);

    // Count methods for statistics
    long countByIsActive(Boolean active);

    long countByAccountLockedUntilAfter(LocalDateTime dateTime);

    long countByMfaEnabled(Boolean mfaEnabled);

    long countByCreatedAtAfter(LocalDateTime dateTime);

    @Query("SELECT COUNT(DISTINCT u) FROM User u JOIN u.userRoles ur JOIN ur.role r WHERE r.name = :roleName")
    long countByRoleName(@Param("roleName") String roleName);

    // ========== Performance Optimized Queries ==========

    // Optimized authentication queries
    @Query("SELECT u FROM User u WHERE u.email = :email AND (u.accountLockedUntil IS NULL OR u.accountLockedUntil < :now) AND u.isActive = true")
    Optional<User> findActiveUserByEmail(@Param("email") String email, @Param("now") LocalDateTime now);

    @Query("SELECT u FROM User u WHERE u.email = :email AND (u.accountLockedUntil IS NULL OR u.accountLockedUntil < :now) AND u.isActive = true AND u.mfaEnabled = :mfaEnabled")
    Optional<User> findActiveUserByEmailAndMfaStatus(@Param("email") String email, @Param("now") LocalDateTime now, @Param("mfaEnabled") Boolean mfaEnabled);

    // Batch operations for better performance
    @Query("SELECT u FROM User u WHERE u.id IN :userIds")
    List<User> findByIdIn(@Param("userIds") List<UUID> userIds);

    @Query("SELECT u FROM User u WHERE u.email IN :emails")
    List<User> findByEmailIn(@Param("emails") List<String> emails);

    // Optimized queries for reporting and analytics
    @Query("SELECT u FROM User u WHERE u.lastLoginAt BETWEEN :startDate AND :endDate ORDER BY u.lastLoginAt DESC")
    List<User> findUsersByLastLoginRange(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    @Query("SELECT u FROM User u WHERE u.accountLockedUntil IS NOT NULL AND u.accountLockedUntil > :now")
    List<User> findLockedUsers(@Param("now") LocalDateTime now);

    @Query("SELECT u FROM User u WHERE u.failedLoginAttempts >= :threshold")
    List<User> findUsersWithFailedAttempts(@Param("threshold") int threshold);

    // Optimized count queries with caching hints
    @Query(value = "SELECT COUNT(*) FROM users WHERE is_active = true AND (account_locked_until IS NULL OR account_locked_until < NOW())", nativeQuery = true)
    long countActiveUsers();

    @Query(value = "SELECT COUNT(*) FROM users WHERE mfa_enabled = true", nativeQuery = true)
    long countMfaEnabledUsers();

    @Query(value = "SELECT COUNT(*) FROM users WHERE created_at >= :startDate", nativeQuery = true)
    long countUsersCreatedAfter(@Param("startDate") LocalDateTime startDate);

    // Bulk update operations for better performance
    @Modifying
    @Query("UPDATE User u SET u.failedLoginAttempts = 0, u.accountLockedUntil = null WHERE u.id = :userId")
    int resetFailedAttempts(@Param("userId") UUID userId);

    @Modifying
    @Query("UPDATE User u SET u.lastLoginAt = :loginTime WHERE u.id = :userId")
    int updateLastLogin(@Param("userId") UUID userId, @Param("loginTime") LocalDateTime loginTime);

    @Modifying
    @Query("UPDATE User u SET u.accountLockedUntil = :lockTime WHERE u.id = :userId")
    int lockAccount(@Param("userId") UUID userId, @Param("lockTime") LocalDateTime lockTime);

    // Tenant-aware queries for multi-tenancy
    @Query("SELECT u FROM User u WHERE u.tenantId = :tenantId AND u.isActive = true ORDER BY u.createdAt DESC")
    Page<User> findActiveUsersByTenant(@Param("tenantId") UUID tenantId, Pageable pageable);

    @Query("SELECT COUNT(u) FROM User u WHERE u.tenantId = :tenantId")
    long countUsersByTenant(@Param("tenantId") UUID tenantId);

    // Security monitoring queries
    @Query("SELECT u FROM User u WHERE u.lastLoginAt < :cutoffDate OR u.lastLoginAt IS NULL ORDER BY u.lastLoginAt ASC NULLS FIRST")
    List<User> findInactiveUsers(@Param("cutoffDate") LocalDateTime cutoffDate);

    @Query(value = """
        SELECT DISTINCT u.id, u.email, u.failed_login_attempts, u.account_locked_until
        FROM users u
        WHERE u.failed_login_attempts >= :threshold
        OR (u.account_locked_until IS NOT NULL AND u.account_locked_until > NOW())
        ORDER BY u.failed_login_attempts DESC, u.account_locked_until DESC
        LIMIT :limit
        """, nativeQuery = true)
    List<Object[]> findSuspiciousUsersNative(@Param("threshold") int threshold, @Param("limit") int limit);

    // Optimized session-related queries
    @Query("SELECT u FROM User u WHERE EXISTS (SELECT 1 FROM UserSession s WHERE s.user.id = u.id AND s.revoked = false AND s.expiresAt > :now)")
    List<User> findUsersWithActiveSessions(@Param("now") LocalDateTime now);

    // Cleanup queries
    @Modifying
    @Query("DELETE FROM User u WHERE u.isActive = false AND u.lastLoginAt < :cutoffDate AND u.createdAt < :cutoffDate")
    int deleteInactiveUsers(@Param("cutoffDate") LocalDateTime cutoffDate);
}