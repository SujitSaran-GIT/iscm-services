package com.iscm.iam.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iscm.iam.model.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CacheService {

    private final CacheManager cacheManager;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    @Value("${app.cache.enabled:true}")
    private boolean cacheEnabled;

    @Value("${app.cache.stats.enabled:true}")
    private boolean statsEnabled;

    // Cache statistics
    private final Map<String, CacheStats> cacheStats = new HashMap<>();

    // ========== User Caching ==========

    public void cacheUser(User user) {
        if (!cacheEnabled || user == null) return;

        try {
            Cache userCache = cacheManager.getCache("users");
            if (userCache != null) {
                userCache.put(user.getId(), user);
                userCache.put("email:" + user.getEmail(), user);
                incrementCacheHits("users");
            }
        } catch (Exception e) {
            log.error("Failed to cache user: {}", user.getId(), e);
        }
    }

    public Optional<User> getCachedUser(UUID userId) {
        if (!cacheEnabled) return Optional.empty();

        try {
            Cache userCache = cacheManager.getCache("users");
            if (userCache != null) {
                User user = userCache.get(userId, User.class);
                if (user != null) {
                    incrementCacheHits("users");
                    return Optional.of(user);
                }
            }
        } catch (Exception e) {
            log.error("Failed to get cached user: {}", userId, e);
        }
        return Optional.empty();
    }

    public Optional<User> getCachedUserByEmail(String email) {
        if (!cacheEnabled) return Optional.empty();

        try {
            Cache userCache = cacheManager.getCache("users");
            if (userCache != null) {
                User user = userCache.get("email:" + email, User.class);
                if (user != null) {
                    incrementCacheHits("users");
                    return Optional.of(user);
                }
            }
        } catch (Exception e) {
            log.error("Failed to get cached user by email: {}", email, e);
        }
        return Optional.empty();
    }

    public void evictUser(UUID userId) {
        if (!cacheEnabled) return;

        try {
            Cache userCache = cacheManager.getCache("users");
            if (userCache != null) {
                User user = userCache.get(userId, User.class);
                if (user != null) {
                    userCache.evict(userId);
                    userCache.evict("email:" + user.getEmail());
                }
            }
        } catch (Exception e) {
            log.error("Failed to evict user from cache: {}", userId, e);
        }
    }

    // ========== Session Caching ==========

    public void cacheActiveSession(UUID userId, String sessionId, String ipAddress) {
        if (!cacheEnabled) return;

        try {
            String key = "activeSession:" + userId;
            Map<String, Object> sessionData = new HashMap<>();
            sessionData.put("sessionId", sessionId);
            sessionData.put("ipAddress", ipAddress);
            sessionData.put("lastAccess", LocalDateTime.now().toString());

            redisTemplate.opsForValue().set(key, sessionData, Duration.ofMinutes(30));
            incrementCacheHits("activeSessions");
        } catch (Exception e) {
            log.error("Failed to cache active session: {}", userId, e);
        }
    }

    public Optional<Map<String, Object>> getActiveSession(UUID userId) {
        if (!cacheEnabled) return Optional.empty();

        try {
            String key = "activeSession:" + userId;
            Map<String, Object> sessionData = (Map<String, Object>) redisTemplate.opsForValue().get(key);
            if (sessionData != null) {
                incrementCacheHits("activeSessions");
                return Optional.of(sessionData);
            }
        } catch (Exception e) {
            log.error("Failed to get active session: {}", userId, e);
        }
        return Optional.empty();
    }

    public void evictActiveSession(UUID userId) {
        if (!cacheEnabled) return;

        try {
            String key = "activeSession:" + userId;
            redisTemplate.delete(key);
        } catch (Exception e) {
            log.error("Failed to evict active session: {}", userId, e);
        }
    }

    // ========== Security Caching ==========

    public void cacheFailedLoginAttempts(String email, int attempts) {
        if (!cacheEnabled) return;

        try {
            String key = "failedAttempts:" + email;
            redisTemplate.opsForValue().set(key, attempts, Duration.ofMinutes(15));
            incrementCacheHits("loginAttempts");
        } catch (Exception e) {
            log.error("Failed to cache failed login attempts: {}", email, e);
        }
    }

    public Integer getFailedLoginAttempts(String email) {
        if (!cacheEnabled) return 0;

        try {
            String key = "failedAttempts:" + email;
            Integer attempts = (Integer) redisTemplate.opsForValue().get(key);
            if (attempts != null) {
                incrementCacheHits("loginAttempts");
                return attempts;
            }
        } catch (Exception e) {
            log.error("Failed to get failed login attempts: {}", email, e);
        }
        return 0;
    }

    public void cacheSecurityEvent(UUID userId, String eventType, String data) {
        if (!cacheEnabled) return;

        try {
            String key = "securityEvent:" + userId + ":" + UUID.randomUUID();
            Map<String, Object> eventData = new HashMap<>();
            eventData.put("userId", userId.toString());
            eventData.put("eventType", eventType);
            eventData.put("data", data);
            eventData.put("timestamp", LocalDateTime.now().toString());

            redisTemplate.opsForValue().set(key, eventData, Duration.ofMinutes(5));
            incrementCacheHits("securityEvents");
        } catch (Exception e) {
            log.error("Failed to cache security event: {}", userId, e);
        }
    }

    // ========== Statistics Caching ==========

    public void cacheStatistics(String statsType, Object data) {
        if (!cacheEnabled) return;

        try {
            String key = "stats:" + statsType;
            redisTemplate.opsForValue().set(key, data, Duration.ofMinutes(2));
            incrementCacheHits("statistics");
        } catch (Exception e) {
            log.error("Failed to cache statistics: {}", statsType, e);
        }
    }

    public Optional<Object> getCachedStatistics(String statsType) {
        if (!cacheEnabled) return Optional.empty();

        try {
            String key = "stats:" + statsType;
            Object data = redisTemplate.opsForValue().get(key);
            if (data != null) {
                incrementCacheHits("statistics");
                return Optional.of(data);
            }
        } catch (Exception e) {
            log.error("Failed to get cached statistics: {}", statsType, e);
        }
        return Optional.empty();
    }

    // ========== Cache Management ==========

    public void clearUserCache(UUID userId) {
        if (!cacheEnabled) return;

        try {
            evictUser(userId);
            evictActiveSession(userId);

            // Clear related cache entries
            String pattern = "securityEvent:" + userId + ":*";
            Set<String> keys = redisTemplate.keys(pattern);
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
            }

            log.debug("Cleared all cache entries for user: {}", userId);
        } catch (Exception e) {
            log.error("Failed to clear user cache: {}", userId, e);
        }
    }

    public void clearAllCache() {
        if (!cacheEnabled) return;

        try {
            cacheManager.getCacheNames().forEach(cacheName -> {
                Cache cache = cacheManager.getCache(cacheName);
                if (cache != null) {
                    cache.clear();
                }
            });

            // Clear Redis cache
            redisTemplate.getConnectionFactory().getConnection().flushDb();

            cacheStats.clear();
            log.info("Cleared all cache entries");
        } catch (Exception e) {
            log.error("Failed to clear all cache", e);
        }
    }

    public Map<String, Object> getCacheStatistics() {
        Map<String, Object> stats = new HashMap<>();

        if (!cacheEnabled) {
            stats.put("enabled", false);
            return stats;
        }

        stats.put("enabled", true);
        stats.put("cacheNames", cacheManager.getCacheNames());

        // Add individual cache stats
        Map<String, Object> individualStats = new HashMap<>();
        cacheStats.forEach((cacheName, cacheStat) -> {
            Map<String, Object> stat = new HashMap<>();
            stat.put("hits", cacheStat.hits);
            stat.put("misses", cacheStat.misses);
            stat.put("hitRate", cacheStat.getHitRate());
            individualStats.put(cacheName, stat);
        });
        stats.put("individualStats", individualStats);

        // Add Redis info
        try {
            Properties info = redisTemplate.getConnectionFactory().getConnection().info();
            stats.put("redisInfo", info);
        } catch (Exception e) {
            log.error("Failed to get Redis info", e);
        }

        return stats;
    }

    // ========== Cache Health Check ==========

    public boolean isCacheHealthy() {
        if (!cacheEnabled) return true;

        try {
            // Test Redis connection
            redisTemplate.opsForValue().set("health:check", "test", Duration.ofSeconds(10));
            String result = (String) redisTemplate.opsForValue().get("health:check");
            redisTemplate.delete("health:check");

            return "test".equals(result);
        } catch (Exception e) {
            log.error("Cache health check failed", e);
            return false;
        }
    }

    // ========== Scheduled Tasks ==========

    @Scheduled(fixedRate = 60000) // Every minute
    public void updateCacheStatistics() {
        if (!statsEnabled) return;

        try {
            // Update hit rates for all caches
            cacheStats.forEach((cacheName, stats) -> {
                log.debug("Cache {} - Hits: {}, Misses: {}, Hit Rate: {:.2f}%",
                         cacheName, stats.hits, stats.misses, stats.getHitRate());
            });
        } catch (Exception e) {
            log.error("Failed to update cache statistics", e);
        }
    }

    @Scheduled(cron = "0 0 2 * * ?") // Daily at 2 AM
    public void cleanupExpiredCache() {
        if (!cacheEnabled) return;

        try {
            log.info("Starting cache cleanup");

            // Reset statistics
            cacheStats.values().forEach(CacheStats::reset);

            log.info("Cache cleanup completed");
        } catch (Exception e) {
            log.error("Failed to cleanup cache", e);
        }
    }

    // ========== Helper Methods ==========

    private void incrementCacheHits(String cacheName) {
        if (!statsEnabled) return;

        cacheStats.computeIfAbsent(cacheName, k -> new CacheStats()).hits++;
    }

    private void incrementCacheMisses(String cacheName) {
        if (!statsEnabled) return;

        cacheStats.computeIfAbsent(cacheName, k -> new CacheStats()).misses++;
    }

    // ========== Inner Classes ==========

    private static class CacheStats {
        long hits = 0;
        long misses = 0;

        double getHitRate() {
            long total = hits + misses;
            return total == 0 ? 0.0 : (double) hits / total * 100;
        }

        void reset() {
            hits = 0;
            misses = 0;
        }
    }
}