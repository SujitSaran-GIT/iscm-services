package com.iscm.iam.cache;

// import com.fasterxml.jackson.databind.ObjectMapper;
// import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
// import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
// import org.springframework.cache.CacheManager;
// import org.springframework.cache.annotation.EnableCaching;
// import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
// import org.springframework.data.redis.cache.RedisCacheConfiguration;
// import org.springframework.data.redis.cache.RedisCacheManager;
// import org.springframework.data.redis.connection.RedisConnectionFactory;
// import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
// import org.springframework.data.redis.serializer.RedisSerializationContext;
// import org.springframework.data.redis.serializer.StringRedisSerializer;
// import org.springframework.data.redis.core.RedisTemplate;

// import java.time.Duration;
// import java.util.HashMap;
// import java.util.Map;

@Configuration
// @EnableCaching
// @ConditionalOnProperty(name = "app.cache.enabled", havingValue = "true", matchIfMissing = false)
public class CacheConfig {

    // @Bean
    // public CacheManager cacheManager(RedisConnectionFactory redisConnectionFactory) {
    //     // Configure ObjectMapper for Java 8 time support
    //     ObjectMapper objectMapper = new ObjectMapper();
    //     objectMapper.registerModule(new JavaTimeModule());

    //     // Use JSON serializer with Java 8 time support
    //     GenericJackson2JsonRedisSerializer jsonSerializer = new GenericJackson2JsonRedisSerializer(objectMapper);

    //     // Default cache configuration
    //     RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
    //             .entryTtl(Duration.ofMinutes(30))
    //             .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
    //             .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(jsonSerializer))
    //             .disableCachingNullValues();

    //     // Specific cache configurations with different TTLs
    //     Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();

    //     // User cache - longer TTL for frequently accessed user data
    //     cacheConfigurations.put("users", defaultConfig.entryTtl(Duration.ofHours(1)));

    //     // User roles cache - medium TTL as roles don't change frequently
    //     cacheConfigurations.put("userRoles", defaultConfig.entryTtl(Duration.ofHours(2)));

    //     // Authentication cache - short TTL for security
    //     cacheConfigurations.put("authTokens", defaultConfig.entryTtl(Duration.ofMinutes(15)));
    //     cacheConfigurations.put("loginAttempts", defaultConfig.entryTtl(Duration.ofMinutes(5)));

    //     // Session cache - medium TTL to balance performance and security
    //     cacheConfigurations.put("sessions", defaultConfig.entryTtl(Duration.ofMinutes(30)));
    //     cacheConfigurations.put("activeSessions", defaultConfig.entryTtl(Duration.ofMinutes(10)));

    //     // Security-related cache - short TTL for real-time security monitoring
    //     cacheConfigurations.put("securityEvents", defaultConfig.entryTtl(Duration.ofMinutes(5)));
    //     cacheConfigurations.put("rateLimits", defaultConfig.entryTtl(Duration.ofMinutes(1)));
    //     cacheConfigurations.put("blacklistedTokens", defaultConfig.entryTtl(Duration.ofHours(24)));

    //     // Organization cache - long TTL as organizations rarely change
    //     cacheConfigurations.put("organizations", defaultConfig.entryTtl(Duration.ofHours(6)));

    //     // Permission cache - long TTL for performance
    //     cacheConfigurations.put("permissions", defaultConfig.entryTtl(Duration.ofHours(4)));

    //     // Statistics and metrics cache - short TTL for near real-time data
    //     cacheConfigurations.put("statistics", defaultConfig.entryTtl(Duration.ofMinutes(2)));
    //     cacheConfigurations.put("metrics", defaultConfig.entryTtl(Duration.ofMinutes(5)));

    //     // Password reset tokens - medium TTL
    //     cacheConfigurations.put("passwordResetTokens", defaultConfig.entryTtl(Duration.ofMinutes(30)));

    //     // MFA related cache - short TTL for security
    //     cacheConfigurations.put("mfaSecrets", defaultConfig.entryTtl(Duration.ofMinutes(10)));
    //     cacheConfigurations.put("mfaBackupCodes", defaultConfig.entryTtl(Duration.ofMinutes(15)));

    //     return RedisCacheManager.builder(redisConnectionFactory)
    //             .cacheDefaults(defaultConfig)
    //             .withInitialCacheConfigurations(cacheConfigurations)
    //             .transactionAware()
    //             .build();
    // }

    // @Bean
    // public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory redisConnectionFactory) {
    //     RedisTemplate<String, Object> template = new RedisTemplate<>();
    //     template.setConnectionFactory(redisConnectionFactory);

    //     // Configure ObjectMapper for Java 8 time support
    //     ObjectMapper objectMapper = new ObjectMapper();
    //     objectMapper.registerModule(new JavaTimeModule());

    //     // Use JSON serializer with Java 8 time support
    //     GenericJackson2JsonRedisSerializer jsonSerializer = new GenericJackson2JsonRedisSerializer(objectMapper);
    //     StringRedisSerializer stringSerializer = new StringRedisSerializer();

    //     template.setKeySerializer(stringSerializer);
    //     template.setValueSerializer(jsonSerializer);
    //     template.setHashKeySerializer(stringSerializer);
    //     template.setHashValueSerializer(jsonSerializer);
    //     template.afterPropertiesSet();
    //     return template;
    // }

    // @Bean
    // public CacheKeyGenerator cacheKeyGenerator() {
    //     return new CacheKeyGenerator();
    // }
}