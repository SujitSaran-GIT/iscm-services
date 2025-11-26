package com.iscm.iam.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * DISABLED STUB - Rate limiting service has been disabled for simplification.
 * This is a stub implementation that does not perform any rate limiting.
 * To re-enable rate limiting, restore the original implementation and add Redis dependencies.
 */
@Slf4j
//@Service("basicRateLimitingService")
public class BasicRateLimitingService {

    // Disabled stub - no rate limiting performed
    public boolean isRateLimited(String key, int maxRequests, Duration duration) {
        log.debug("Rate limiting disabled - allowing request for key: {}", key);
        return false; // Always allow requests when rate limiting is disabled
    }

    public boolean isBurstLimited(String key, int burstLimit, Duration recoveryDuration) {
        log.debug("Burst limiting disabled - allowing request for key: {}", key);
        return false; // Always allow requests when burst limiting is disabled
    }

    public void recordBurst(String key, Duration recoveryDuration) {
        // Stub implementation - no action taken
        log.debug("Burst recording disabled for key: {}", key);
    }

    public long getRemainingRequests(String key, int maxRequests, Duration duration) {
        // Return max requests since rate limiting is disabled
        log.debug("Rate limiting disabled - returning max requests for key: {}", key);
        return maxRequests;
    }

    public Duration getTimeUntilReset(String key, Duration duration) {
        // Return zero since rate limiting is disabled
        log.debug("Rate limiting disabled - no reset time for key: {}", key);
        return Duration.ZERO;
    }
}