package com.iscm.iam.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.UUID;

@Slf4j
@Service
public class JwtBlacklistService {

    private final RedisTemplate<String, String> redisTemplate;
    private final RedisScript<Boolean> blacklistScript;
    private final JwtUtil jwtUtil;

    @Value("${app.jwt.blacklist.enabled:true}")
    private boolean blacklistEnabled;

    @Value("${app.jwt.blacklist.cleanup-interval:3600}")
    private int cleanupIntervalSeconds;

    @Value("${app.jwt.blacklist.max-size:10000}")
    private int maxBlacklistSize;

    private static final String BLACKLIST_PREFIX = "jwt:blacklist:";
    private static final String JTI_PREFIX = "jti:";

    // Precompiled Lua script for atomic operations
    private static final String BLACKLIST_SCRIPT = """
        local key = KEYS[1]
        local jti = ARGV[1]
        local expiration = tonumber(ARGV[2])
        local current_time = tonumber(ARGV[3])

        -- Check if token is already blacklisted
        local exists = redis.call('exists', key .. jti)
        if exists == 1 then
            return 1  -- Already blacklisted
        end

        -- Add to blacklist with expiration
        redis.call('set', key .. jti, 'blacklisted')
        redis.call('expire', key .. jti, expiration)

        -- Track blacklist size
        local size_key = key .. 'size'
        local current_size = redis.call('incr', size_key)
        redis.call('expire', size_key, 86400)  -- Size counter expires in 24 hours

        -- Cleanup old entries if size exceeds limit
        if current_size > tonumber(ARGV[4]) then
            local cutoff = current_time - 86400  -- Remove entries older than 24 hours
            redis.call('zremrangebyscore', key .. 'zset', 0, cutoff)
        end

        return 0  -- Successfully blacklisted
        """;

    public JwtBlacklistService(RedisTemplate<String, String> redisTemplate, JwtUtil jwtUtil) {
        this.redisTemplate = redisTemplate;
        this.jwtUtil = jwtUtil;
        this.blacklistScript = RedisScript.of(BLACKLIST_SCRIPT, Boolean.class);
    }

    /**
     * Blacklist a JWT token
     * @param token JWT token to blacklist
     * @param reason Reason for blacklisting
     */
    public void blacklistToken(String token, String reason) {
        if (!blacklistEnabled || token == null || token.trim().isEmpty()) {
            return;
        }

        try {
            // Extract token ID (jti claim) or generate from token hash
            String jti = extractTokenId(token);

            // Calculate expiration based on token expiration time
            Instant expiration = getTokenExpiration(token);
            long expirationSeconds = Duration.between(Instant.now(), expiration).getSeconds();

            if (expirationSeconds <= 0) {
                log.info("Token already expired, not blacklisting: {}", jti);
                return;
            }

            // Execute blacklist script
            Boolean result = redisTemplate.execute(
                blacklistScript,
                Collections.singletonList(BLACKLIST_PREFIX),
                jti,
                String.valueOf(expirationSeconds),
                String.valueOf(Instant.now().getEpochSecond()),
                String.valueOf(maxBlacklistSize)
            );

            if (result != null && !result) {
                log.info("Token blacklisted successfully: jti={}, reason={}, expiresAt={}", jti, reason, expiration);

                // Add to sorted set for tracking and cleanup
                redisTemplate.opsForZSet().add(
                    BLACKLIST_PREFIX + "zset",
                    jti,
                    Instant.now().getEpochSecond()
                );
                redisTemplate.expire(BLACKLIST_PREFIX + "zset", Duration.ofDays(7));

            } else {
                log.warn("Token was already blacklisted: jti={}", jti);
            }

        } catch (Exception e) {
            log.error("Failed to blacklist token: {}", token, e);
        }
    }

    /**
     * Blacklist all tokens for a user
     * @param userId User ID whose tokens should be blacklisted
     * @param reason Reason for blacklisting
     */
    public void blacklistAllUserTokens(UUID userId, String reason) {
        try {
            // Add user to blacklist with expiration
            String userKey = BLACKLIST_PREFIX + "user:" + userId;
            redisTemplate.opsForValue().set(userKey, reason, Duration.ofDays(7));

            log.info("All tokens blacklisted for user: {}, reason: {}", userId, reason);

        } catch (Exception e) {
            log.error("Failed to blacklist all user tokens for user: {}", userId, e);
        }
    }

    /**
     * Check if a token is blacklisted
     * @param token JWT token to check
     * @return true if token is blacklisted, false otherwise
     */
    public boolean isTokenBlacklisted(String token) {
        if (!blacklistEnabled || token == null || token.trim().isEmpty()) {
            return false;
        }

        try {
            String jti = extractTokenId(token);
            String key = BLACKLIST_PREFIX + jti;

            // Check if token is blacklisted
            Boolean exists = redisTemplate.hasKey(key);

            if (Boolean.TRUE.equals(exists)) {
                log.debug("Token is blacklisted: jti={}", jti);
                return true;
            }

            // Check if user is blacklisted
            if (jwtUtil.validateToken(token)) {
                try {
                    String userId = jwtUtil.getUserIdFromToken(token);
                    String userKey = BLACKLIST_PREFIX + "user:" + userId;

                    if (Boolean.TRUE.equals(redisTemplate.hasKey(userKey))) {
                        log.debug("Token belongs to blacklisted user: userId={}", userId);
                        return true;
                    }
                } catch (Exception e) {
                    log.debug("Failed to extract user ID from token for blacklist check", e);
                }
            }

            return false;

        } catch (Exception e) {
            log.error("Failed to check if token is blacklisted: {}", token, e);
            return false; // Fail open - allow token if check fails
        }
    }

    /**
     * Check if all tokens for a user are blacklisted
     * @param userId User ID to check
     * @return true if user's tokens are blacklisted, false otherwise
     */
    public boolean areUserTokensBlacklisted(UUID userId) {
        if (!blacklistEnabled) {
            return false;
        }

        try {
            String userKey = BLACKLIST_PREFIX + "user:" + userId;
            return Boolean.TRUE.equals(redisTemplate.hasKey(userKey));
        } catch (Exception e) {
            log.error("Failed to check if user tokens are blacklisted: {}", userId, e);
            return false;
        }
    }

    /**
     * Remove a token from blacklist (e.g., for testing)
     * @param token JWT token to remove from blacklist
     */
    public void removeFromBlacklist(String token) {
        if (!blacklistEnabled || token == null || token.trim().isEmpty()) {
            return;
        }

        try {
            String jti = extractTokenId(token);
            String key = BLACKLIST_PREFIX + jti;

            redisTemplate.delete(key);
            redisTemplate.opsForZSet().remove(BLACKLIST_PREFIX + "zset", jti);

            log.info("Token removed from blacklist: jti={}", jti);

        } catch (Exception e) {
            log.error("Failed to remove token from blacklist: {}", token, e);
        }
    }

    /**
     * Remove user from blacklist
     * @param userId User ID to remove from blacklist
     */
    public void removeFromUserBlacklist(UUID userId) {
        if (!blacklistEnabled) {
            return;
        }

        try {
            String userKey = BLACKLIST_PREFIX + "user:" + userId;
            redisTemplate.delete(userKey);

            log.info("User removed from blacklist: userId={}", userId);

        } catch (Exception e) {
            log.error("Failed to remove user from blacklist: {}", userId, e);
        }
    }

    /**
     * Clean up expired blacklisted tokens
     */
    public void cleanupExpiredTokens() {
        try {
            // Remove expired tokens from sorted set
            long cutoff = Instant.now().minusSeconds(86400).getEpochSecond(); // 24 hours ago
            Long removed = redisTemplate.opsForZSet().removeRangeByScore(
                BLACKLIST_PREFIX + "zset", 0, cutoff);

            if (removed != null && removed > 0) {
                log.info("Cleaned up {} expired blacklisted tokens", removed);
            }

            // Reset size counter
            redisTemplate.delete(BLACKLIST_PREFIX + "size");

        } catch (Exception e) {
            log.error("Failed to cleanup expired blacklisted tokens", e);
        }
    }

    /**
     * Get blacklist statistics
     */
    public BlacklistStatistics getBlacklistStatistics() {
        try {
            Long activeTokens = redisTemplate.opsForZSet().size(BLACKLIST_PREFIX + "zset");
            String sizeCount = redisTemplate.opsForValue().get(BLACKLIST_PREFIX + "size");
            long size = sizeCount != null ? Long.parseLong(sizeCount) : 0;

            return BlacklistStatistics.builder()
                    .activeBlacklistedTokens(activeTokens != null ? activeTokens.intValue() : 0)
                    .totalBlacklistedTokens((int) size)
                    .blacklistEnabled(blacklistEnabled)
                    .maxBlacklistSize(maxBlacklistSize)
                    .build();

        } catch (Exception e) {
            log.error("Failed to get blacklist statistics", e);
            return BlacklistStatistics.builder()
                    .activeBlacklistedTokens(0)
                    .totalBlacklistedTokens(0)
                    .blacklistEnabled(blacklistEnabled)
                    .maxBlacklistSize(maxBlacklistSize)
                    .build();
        }
    }

    // Helper methods

    private String extractTokenId(String token) {
        try {
            // Try to extract JTI claim from access token
            String jti = jwtUtil.getJtiFromToken(token);
            if (jti != null) {
                return jti;
            }

            // Try to extract JTI from refresh token
            jti = jwtUtil.getJtiFromRefreshToken(token);
            if (jti != null) {
                return jti;
            }

            // Fallback: generate deterministic hash
            log.debug("No JTI found in token, using hash fallback");
            return "jti:" + Integer.toHexString(token.hashCode());

        } catch (Exception e) {
            log.debug("Failed to extract JTI from token", e);
            return "jti:" + Integer.toHexString(token.hashCode());
        }
    }

    private Instant getTokenExpiration(String token) {
        try {
            // Try to get expiration from access token
            Instant expiration = jwtUtil.getExpirationFromToken(token);
            if (expiration != null) {
                return expiration;
            }

            // Fallback: use default expiration
            log.debug("No expiration found in token, using default");
            return Instant.now().plus(Duration.ofHours(1));

        } catch (Exception e) {
            log.debug("Failed to extract expiration from token", e);
            return Instant.now().plus(Duration.ofHours(1));
        }
    }

    // DTO for blacklist statistics
    @lombok.Data
    @lombok.Builder
    public static class BlacklistStatistics {
        private int activeBlacklistedTokens;
        private int totalBlacklistedTokens;
        private boolean blacklistEnabled;
        private int maxBlacklistSize;
    }
}