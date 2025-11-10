package com.iscm.iam.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class RateLimitingService {

    private final RedisTemplate<String, String> redisTemplate;
    private final RedisScript<Long> rateLimitScript;
    private final RedisScript<Long> slidingWindowScript;

    @Value("${app.security.rate-limiting.enabled:true}")
    private boolean rateLimitingEnabled;

    @Value("${app.security.rate-limiting.max-attempts:5}")
    private int maxAttempts;

    @Value("${app.security.rate-limiting.window-seconds:60}")
    private int windowSeconds;

    @Value("${app.security.rate-limiting.global.max-requests:1000}")
    private int globalMaxRequests;

    @Value("${app.security.rate-limiting.global.window-seconds:60}")
    private int globalWindowSeconds;

    // Precompiled Lua scripts for atomic operations
    private static final String RATE_LIMIT_SCRIPT = """
        local key = KEYS[1]
        local limit = tonumber(ARGV[1])
        local window = tonumber(ARGV[2])
        local current_time = tonumber(ARGV[3])

        redis.call('zremrangebyscore', key, 0, current_time - window)
        local current_requests = redis.call('zcard', key)

        if current_requests < limit then
            redis.call('zadd', key, current_time, current_time)
            redis.call('expire', key, window + 1)
            return 1
        else
            return 0
        end
        """;

    private static final String SLIDING_WINDOW_SCRIPT = """
        local key = KEYS[1]
        local limit = tonumber(ARGV[1])
        local window = tonumber(ARGV[2])
        local current_time = tonumber(ARGV[3])

        redis.call('zremrangebyscore', key, 0, current_time - window)
        local requests = redis.call('zrange', key, 0, -1)
        local count = 0

        for i, timestamp in ipairs(requests) do
            if tonumber(timestamp) >= current_time - window then
                count = count + 1
            end
        end

        if count < limit then
            redis.call('zadd', key, current_time, current_time)
            redis.call('expire', key, window + 1)
            return 1
        else
            return 0
        end
        """;

    public RateLimitingService(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.rateLimitScript = RedisScript.of(RATE_LIMIT_SCRIPT, Long.class);
        this.slidingWindowScript = RedisScript.of(SLIDING_WINDOW_SCRIPT, Long.class);
    }

    /**
     * Check if a specific key is rate limited
     * @param key Unique identifier (IP address, user ID, etc.)
     * @param limit Maximum allowed requests
     * @param window Time window in seconds
     * @return true if rate limited (should block), false if allowed
     */
    public boolean isRateLimited(String key, int limit, Duration window) {
        if (!rateLimitingEnabled) {
            return false;
        }

        try {
            Long result = redisTemplate.execute(
                rateLimitScript,
                Collections.singletonList(getRateLimitKey(key)),
                String.valueOf(limit),
                String.valueOf(window.getSeconds()),
                String.valueOf(Instant.now().getEpochSecond())
            );

            boolean isBlocked = result != null && result == 0;
            if (isBlocked) {
                log.warn("Rate limit exceeded for key: {}", key);
            }

            return isBlocked;

        } catch (Exception e) {
            log.error("Rate limiting check failed for key: {}, allowing request", key, e);
            return false; // Fail open - allow request if rate limiting fails
        }
    }

    /**
     * Check if a specific key is rate limited with sliding window
     * @param key Unique identifier
     * @param limit Maximum allowed requests
     * @param window Time window in seconds
     * @return true if rate limited (should block), false if allowed
     */
    public boolean isRateLimitedSlidingWindow(String key, int limit, Duration window) {
        if (!rateLimitingEnabled) {
            return false;
        }

        try {
            Long result = redisTemplate.execute(
                slidingWindowScript,
                Collections.singletonList(getSlidingWindowKey(key)),
                String.valueOf(limit),
                String.valueOf(window.getSeconds()),
                String.valueOf(Instant.now().getEpochSecond())
            );

            boolean isBlocked = result != null && result == 0;
            if (isBlocked) {
                log.warn("Sliding window rate limit exceeded for key: {}", key);
            }

            return isBlocked;

        } catch (Exception e) {
            log.error("Sliding window rate limiting check failed for key: {}, allowing request", key, e);
            return false;
        }
    }

    /**
     * Check if IP address is rate limited for login attempts
     */
    public boolean isLoginRateLimited(String ipAddress) {
        return isRateLimited(
            "login:" + ipAddress,
            maxAttempts,
            Duration.ofSeconds(windowSeconds)
        );
    }

    /**
     * Check if IP address is rate limited for registration attempts
     */
    public boolean isRegistrationRateLimited(String ipAddress) {
        return isRateLimited(
            "register:" + ipAddress,
            3, // Max 3 registration attempts per hour
            Duration.ofHours(1)
        );
    }

    /**
     * Check if user is rate limited for password reset attempts
     */
    public boolean isPasswordResetRateLimited(String email) {
        return isRateLimited(
            "password-reset:" + email.toLowerCase(),
            3, // Max 3 password reset attempts per hour
            Duration.ofHours(1)
        );
    }

    /**
     * Check if user is rate limited for MFA attempts
     */
    public boolean isMfaRateLimited(String userId) {
        return isRateLimited(
            "mfa:" + userId,
            10, // Max 10 MFA attempts per 5 minutes
            Duration.ofMinutes(5)
        );
    }

    /**
     * Check if IP address is rate limited globally
     */
    public boolean isGloballyRateLimited(String ipAddress) {
        return isRateLimited(
            "global:" + ipAddress,
            globalMaxRequests,
            Duration.ofSeconds(globalWindowSeconds)
        );
    }

    /**
     * Record a successful request (for monitoring)
     */
    public void recordRequest(String key) {
        try {
            String successKey = "success:" + key;
            redisTemplate.opsForValue().set(
                successKey,
                "1",
                Duration.ofSeconds(300) // Keep for 5 minutes
            );
        } catch (Exception e) {
            log.error("Failed to record request for key: {}", key, e);
        }
    }

    /**
     * Record a failed attempt
     */
    public void recordFailedAttempt(String key, String type) {
        try {
            String failedKey = "failed:" + type + ":" + key;
            redisTemplate.opsForValue().increment(failedKey);
            redisTemplate.expire(failedKey, Duration.ofHours(24)); // Keep for 24 hours
        } catch (Exception e) {
            log.error("Failed to record failed attempt for key: {}", key, e);
        }
    }

    /**
     * Get rate limit status for a key
     */
    public RateLimitStatus getRateLimitStatus(String key, int limit, Duration window) {
        try {
            String rateKey = getRateLimitKey(key);
            Long currentCount = redisTemplate.opsForZSet().count(
                rateKey,
                Instant.now().minusSeconds(window.getSeconds()).getEpochSecond(),
                Double.MAX_VALUE
            );

            if (currentCount == null) {
                currentCount = 0L;
            }

            Long ttl = redisTemplate.getExpire(rateKey, TimeUnit.SECONDS);

            return RateLimitStatus.builder()
                    .key(key)
                    .currentCount(currentCount.intValue())
                    .limit(limit)
                    .remaining(Math.max(0, limit - currentCount.intValue()))
                    .windowSeconds((int) window.getSeconds())
                    .resetTimeSeconds(ttl != null && ttl > 0 ? ttl.intValue() : (int) window.getSeconds())
                    .isRateLimited(currentCount >= limit)
                    .build();

        } catch (Exception e) {
            log.error("Failed to get rate limit status for key: {}", key, e);
            return RateLimitStatus.builder()
                    .key(key)
                    .currentCount(0)
                    .limit(limit)
                    .remaining(limit)
                    .windowSeconds((int) window.getSeconds())
                    .resetTimeSeconds((int) window.getSeconds())
                    .isRateLimited(false)
                    .build();
        }
    }

    /**
     * Clear rate limit for a key (admin function)
     */
    public void clearRateLimit(String key) {
        try {
            redisTemplate.delete(getRateLimitKey(key));
            redisTemplate.delete(getSlidingWindowKey(key));
            log.info("Rate limit cleared for key: {}", key);
        } catch (Exception e) {
            log.error("Failed to clear rate limit for key: {}", key, e);
        }
    }

    /**
     * Get failed attempt count for a key and type
     */
    public int getFailedAttemptCount(String key, String type) {
        try {
            String failedKey = "failed:" + type + ":" + key;
            String count = redisTemplate.opsForValue().get(failedKey);
            return count != null ? Integer.parseInt(count) : 0;
        } catch (Exception e) {
            log.error("Failed to get failed attempt count for key: {}", key, e);
            return 0;
        }
    }

    // Helper methods
    private String getRateLimitKey(String key) {
        return "rate-limit:" + key;
    }

    private String getSlidingWindowKey(String key) {
        return "sliding-limit:" + key;
    }

    // DTO for rate limit status
    @lombok.Data
    @lombok.Builder
    public static class RateLimitStatus {
        private String key;
        private int currentCount;
        private int limit;
        private int remaining;
        private int windowSeconds;
        private int resetTimeSeconds;
        private boolean isRateLimited;
    }
}