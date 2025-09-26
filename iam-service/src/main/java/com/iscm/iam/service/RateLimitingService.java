package com.iscm.iam.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class RateLimitingService {

    private final RedisTemplate<String, Long> redisTemplate;

    private static final String RATE_LIMIT_PREFIX = "rate_limit:";
    private static final String BURST_PREFIX = "burst_limit:";

    public boolean isRateLimited(String key, int maxRequests, Duration duration) {
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
    }

    public boolean isBurstLimited(String key, int burstLimit, Duration recoveryDuration) {
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
    }

    public void recordBurst(String key, Duration recoveryDuration) {
        String burstKey = BURST_PREFIX + key;
        long currentTime = Instant.now().getEpochSecond();
        
        redisTemplate.opsForValue().set(burstKey, currentTime, recoveryDuration);
        log.info("Burst recorded for key: {} with recovery duration: {}s", 
                key, recoveryDuration.getSeconds());
    }

    public long getRemainingRequests(String key, int maxRequests, Duration duration) {
        String redisKey = RATE_LIMIT_PREFIX + key;
        long currentTime = Instant.now().getEpochSecond();
        long windowStart = currentTime - duration.getSeconds();

        redisTemplate.opsForZSet().removeRangeByScore(redisKey, 0, windowStart);
        
        Long requestCount = redisTemplate.opsForZSet().zCard(redisKey);
        return Math.max(0, maxRequests - (requestCount != null ? requestCount : 0));
    }

    public Duration getTimeUntilReset(String key, Duration duration) {
        String redisKey = RATE_LIMIT_PREFIX + key;
        long currentTime = Instant.now().getEpochSecond();
        
        Long oldestRequest = redisTemplate.opsForZSet().range(redisKey, 0, 0).stream()
                .findFirst()
                .orElse((double) currentTime)
                .longValue();
        
        long resetTime = oldestRequest + duration.getSeconds();
        return Duration.ofSeconds(Math.max(0, resetTime - currentTime));
    }
}