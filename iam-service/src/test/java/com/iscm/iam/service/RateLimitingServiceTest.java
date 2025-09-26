package com.iscm.iam.service;

import com.iscm.iam.BaseIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

class RateLimitingServiceTest extends BaseIntegrationTest {

    @Autowired
    private RateLimitingService rateLimitingService;

    @Test
    void testRateLimitingWithinLimit() {
        String key = "test-client";
        
        for (int i = 0; i < 5; i++) {
            boolean limited = rateLimitingService.isRateLimited(key, 10, Duration.ofMinutes(1));
            assertFalse(limited, "Request " + (i + 1) + " should not be limited");
        }
    }

    @Test
    void testRateLimitingExceedsLimit() {
        String key = "test-client-2";
        
        // Use all available requests
        for (int i = 0; i < 10; i++) {
            rateLimitingService.isRateLimited(key, 10, Duration.ofMinutes(1));
        }
        
        // Next request should be limited
        boolean limited = rateLimitingService.isRateLimited(key, 10, Duration.ofMinutes(1));
        assertTrue(limited, "Request should be limited after exceeding rate limit");
    }
}