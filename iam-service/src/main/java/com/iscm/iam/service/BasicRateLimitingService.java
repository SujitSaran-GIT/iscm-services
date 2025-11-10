package com.iscm.iam.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service("basicRateLimitingService")
@RequiredArgsConstructor
public class BasicRateLimitingService {

    private final RedisTemplate<String, Long> redisTemplate;

    private static final String RATE_LIMIT_PREFIX = "rate_limit:";
    private static final String BURST_PREFIX = "burst_limit:";

    public boolean isRateLimited(String key, int maxRequests, Duration duration) {
        try {
            String redisKey = RATE_LIMIT_PREFIX + key;
            long currentTime = Instant.now().getEpochSecond();
            long windowStart = currentTime - duration.getSeconds();

            // Remove old entries outside the current window
            redisTemplate.opsForZSet().removeRangeByScore(redisKey, 0, windowStart);

            // Count requests in current window
            Long requestCount = redisTemplate.opsForZSet().zCard(redisKey);

            if (requestCount != null && requestCount >= maxRequests) {
                log.warn("Rate limit exceeded for key: {}. Requests: {}/{}", key, requestCount, maxRequests);
                return true;
            }

            // Add current request
            redisTemplate.opsForZSet().add(redisKey, currentTime, currentTime);
            redisTemplate.expire(redisKey, duration.getSeconds() + 1, TimeUnit.SECONDS);

            return false;
        } catch (RedisConnectionFailureException e) {
            log.warn("Redis connection failed for rate limiting, allowing request to proceed. Key: {}", key, e);
            return false; // Allow request to proceed if Redis is unavailable
        }
    }

    public boolean isBurstLimited(String key, int burstLimit, Duration recoveryDuration) {
        try {
            String burstKey = BURST_PREFIX + key;
            long currentTime = Instant.now().getEpochSecond();

            Long lastBurst = redisTemplate.opsForValue().get(burstKey);

            if (lastBurst != null) {
                long timeSinceLastBurst = currentTime - lastBurst;
                if (timeSinceLastBurst < recoveryDuration.getSeconds()) {
                    log.warn("Burst limit active for key: {}. Time since last burst: {}s",
                            key, timeSinceLastBurst);
                    return true;
                } else {
                    // Recovery period passed, reset burst limit
                    redisTemplate.delete(burstKey);
                }
            }

            return false;
        } catch (RedisConnectionFailureException e) {
            log.warn("Redis connection failed for burst limiting, allowing request to proceed. Key: {}", key, e);
            return false; // Allow request to proceed if Redis is unavailable
        }
    }

    public void recordBurst(String key, Duration recoveryDuration) {
        try {
            String burstKey = BURST_PREFIX + key;
            long currentTime = Instant.now().getEpochSecond();

            redisTemplate.opsForValue().set(burstKey, currentTime, recoveryDuration);
            log.info("Burst recorded for key: {} with recovery duration: {}s",
                    key, recoveryDuration.getSeconds());
        } catch (RedisConnectionFailureException e) {
            log.warn("Redis connection failed for recording burst, skipping. Key: {}", key, e);
            // Silently fail if Redis is unavailable
        }
    }

    public long getRemainingRequests(String key, int maxRequests, Duration duration) {
        try {
            String redisKey = RATE_LIMIT_PREFIX + key;
            long currentTime = Instant.now().getEpochSecond();
            long windowStart = currentTime - duration.getSeconds();

            redisTemplate.opsForZSet().removeRangeByScore(redisKey, 0, windowStart);

            Long requestCount = redisTemplate.opsForZSet().zCard(redisKey);
            return Math.max(0, maxRequests - (requestCount != null ? requestCount : 0));
        } catch (RedisConnectionFailureException e) {
            log.warn("Redis connection failed for getting remaining requests, returning max. Key: {}", key, e);
            return maxRequests; // Return max requests if Redis is unavailable
        }
    }

    public Duration getTimeUntilReset(String key, Duration duration) {
        try {
            String redisKey = RATE_LIMIT_PREFIX + key;
            long currentTime = Instant.now().getEpochSecond();

            Long oldestRequest = redisTemplate.opsForZSet().rangeWithScores(redisKey, 0, 0).stream()
                    .findFirst()
                    .map(tuple -> tuple.getScore())
                    .orElse(Double.valueOf(currentTime))
                    .longValue();

            long resetTime = oldestRequest + duration.getSeconds();
            return Duration.ofSeconds(Math.max(0, resetTime - currentTime));
        } catch (RedisConnectionFailureException e) {
            log.warn("Redis connection failed for getting time until reset, returning 0. Key: {}", key, e);
            return Duration.ZERO; // Return 0 if Redis is unavailable
        }
    }
}