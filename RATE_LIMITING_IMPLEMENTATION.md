# Rate Limiting Implementation - ISCM IAM Service

## Overview

This document provides a comprehensive overview of the rate limiting implementation in the ISCM Identity and Access Management (IAM) service. The rate limiting mechanism protects against abuse, brute-force attacks, and ensures fair usage of API endpoints.

## Architecture

### Components

1. **RateLimitingService** - Core rate limiting logic
2. **RateLimitingFilter** - Spring Security filter for HTTP request interception
3. **Redis** - Distributed cache for storing rate limit counters
4. **Configuration Properties** - Customizable rate limiting parameters

## Configuration

### Application Properties

```properties
# Rate Limiting Configuration
app.security.rate-limiting.enabled=true
app.security.rate-limiting.max-attempts=5
app.security.rate-limiting.window-seconds=60
```

### Redis Configuration

```properties
# Redis Configuration - OPTIMIZED
spring.data.redis.host=localhost
spring.data.redis.port=6379
spring.data.redis.password=
spring.data.redis.database=0
spring.data.redis.timeout=500ms

# Redis Connection Pool - OPTIMIZED
spring.data.redis.lettuce.pool.max-active=50
spring.data.redis.lettuce.pool.max-idle=20
spring.data.redis.lettuce.pool.min-idle=5
spring.data.redis.lettuce.pool.max-wait=1000ms
spring.data.redis.lettuce.shutdown-timeout=200ms
```

## Implementation Details

### 1. RateLimitingService.java

```java
@Service
@Slf4j
@RequiredArgsConstructor
public class RateLimitingService {

    private final RedisTemplate<String, String> redisTemplate;
    private final RateLimitingProperties properties;

    /**
     * Check if the request should be rate limited
     * @param key Unique identifier for the rate limit (IP address, email, etc.)
     * @param limit Maximum number of requests allowed
     * @param windowSeconds Time window in seconds
     * @return RateLimitResponse with current status
     */
    public RateLimitResponse checkRateLimit(String key, int limit, int windowSeconds) {
        if (!properties.isEnabled()) {
            return RateLimitResponse.allowed();
        }

        String redisKey = buildRedisKey(key);

        try {
            // Get current count and set expiration if key doesn't exist
            Long currentCount = redisTemplate.opsForValue().increment(redisKey);

            if (currentCount == 1) {
                // Set expiration for new keys
                redisTemplate.expire(redisKey, windowSeconds, TimeUnit.SECONDS);
            }

            int remaining = Math.max(0, limit - currentCount.intValue());
            boolean allowed = currentCount <= limit;

            log.debug("Rate limit check for key {}: count={}, allowed={}, remaining={}",
                     key, currentCount, allowed, remaining);

            return RateLimitResponse.builder()
                    .allowed(allowed)
                    .limit(limit)
                    .remaining(remaining)
                    .resetTime(windowSeconds)
                    .retryAfter(allowed ? 0 : windowSeconds)
                    .build();

        } catch (Exception e) {
            log.error("Error checking rate limit for key: {}", key, e);
            // Fail open - allow request if rate limiting fails
            return RateLimitResponse.allowed();
        }
    }

    /**
     * Reset rate limit for a specific key
     * @param key The key to reset
     */
    public void resetRateLimit(String key) {
        try {
            String redisKey = buildRedisKey(key);
            redisTemplate.delete(redisKey);
            log.info("Rate limit reset for key: {}", key);
        } catch (Exception e) {
            log.error("Error resetting rate limit for key: {}", key, e);
        }
    }

    private String buildRedisKey(String key) {
        return "rate-limit:" + key;
    }
}
```

### 2. RateLimitingProperties.java

```java
@ConfigurationProperties(prefix = "app.security.rate-limiting")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RateLimitingProperties {

    private boolean enabled = true;
    private int maxAttempts = 5;
    private int windowSeconds = 60;

    // Different limits for different operations
    private int loginMaxAttempts = 5;
    private int loginWindowSeconds = 300; // 5 minutes
    private int registrationMaxAttempts = 3;
    private int registrationWindowSeconds = 3600; // 1 hour
    private int passwordResetMaxAttempts = 3;
    private int passwordResetWindowSeconds = 900; // 15 minutes
}
```

### 3. RateLimitingFilter.java

```java
@Component
@Order(2)
@Slf4j
@RequiredArgsConstructor
public class RateLimitingFilter extends OncePerRequestFilter {

    private final RateLimitingService rateLimitingService;
    private final RateLimitingProperties properties;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                  HttpServletResponse response,
                                  FilterChain filterChain) throws ServletException, IOException {

        if (!properties.isEnabled()) {
            filterChain.doFilter(request, response);
            return;
        }

        String clientIp = getClientIpAddress(request);
        String requestUri = request.getRequestURI();
        String method = request.getMethod();

        // Generate rate limit key based on endpoint type
        String rateLimitKey = generateRateLimitKey(requestUri, method, clientIp);

        // Determine rate limit based on endpoint
        RateLimitConfig config = getRateLimitConfig(requestUri, method);

        // Check rate limit
        RateLimitResponse rateLimitResponse = rateLimitingService.checkRateLimit(
                rateLimitKey, config.getLimit(), config.getWindowSeconds());

        // Add rate limit headers
        addRateLimitHeaders(response, rateLimitResponse);

        if (rateLimitResponse.isAllowed()) {
            filterChain.doFilter(request, response);
        } else {
            handleRateLimitExceeded(response, rateLimitResponse);
        }
    }

    private String generateRateLimitKey(String requestUri, String method, String clientIp) {
        // Different rate limiting strategies for different endpoints
        if (requestUri.contains("/auth/login")) {
            return "login:" + clientIp;
        } else if (requestUri.contains("/auth/register")) {
            return "register:" + clientIp;
        } else if (requestUri.contains("/password/reset")) {
            return "password-reset:" + clientIp;
        } else {
            // General API rate limiting
            return "api:" + clientIp + ":" + method;
        }
    }

    private RateLimitConfig getRateLimitConfig(String requestUri, String method) {
        if (requestUri.contains("/auth/login")) {
            return RateLimitConfig.builder()
                    .limit(properties.getLoginMaxAttempts())
                    .windowSeconds(properties.getLoginWindowSeconds())
                    .build();
        } else if (requestUri.contains("/auth/register")) {
            return RateLimitConfig.builder()
                    .limit(properties.getRegistrationMaxAttempts())
                    .windowSeconds(properties.getRegistrationWindowSeconds())
                    .build();
        } else if (requestUri.contains("/password/reset")) {
            return RateLimitConfig.builder()
                    .limit(properties.getPasswordResetMaxAttempts())
                    .windowSeconds(properties.getPasswordResetWindowSeconds())
                    .build();
        } else {
            // Default rate limit for other endpoints
            return RateLimitConfig.builder()
                    .limit(properties.getMaxAttempts())
                    .windowSeconds(properties.getWindowSeconds())
                    .build();
        }
    }

    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(xForwardedFor)) {
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIp = request.getHeader("X-Real-IP");
        if (StringUtils.hasText(xRealIp)) {
            return xRealIp;
        }

        return request.getRemoteAddr();
    }

    private void addRateLimitHeaders(HttpServletResponse response, RateLimitResponse rateLimitResponse) {
        response.setHeader("X-RateLimit-Limit", String.valueOf(rateLimitResponse.getLimit()));
        response.setHeader("X-RateLimit-Remaining", String.valueOf(rateLimitResponse.getRemaining()));
        response.setHeader("X-RateLimit-Reset", String.valueOf(rateLimitResponse.getResetTime()));

        if (!rateLimitResponse.isAllowed()) {
            response.setHeader("X-RateLimit-Retry-After", String.valueOf(rateLimitResponse.getRetryAfter()));
        }
    }

    private void handleRateLimitExceeded(HttpServletResponse response, RateLimitResponse rateLimitResponse) throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        RateLimitErrorResponse errorResponse = RateLimitErrorResponse.builder()
                .error("RATE_LIMIT_EXCEEDED")
                .message("Rate limit exceeded. Please try again later.")
                .limit(rateLimitResponse.getLimit())
                .remaining(rateLimitResponse.getRemaining())
                .resetTime(rateLimitResponse.getResetTime())
                .retryAfter(rateLimitResponse.getRetryAfter())
                .timestamp(Instant.now())
                .build();

        ObjectMapper objectMapper = new ObjectMapper();
        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    }
}
```

### 4. Rate Limit DTOs

#### RateLimitResponse.java

```java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RateLimitResponse {
    private boolean allowed;
    private int limit;
    private int remaining;
    private int resetTime;
    private int retryAfter;

    public static RateLimitResponse allowed() {
        return RateLimitResponse.builder()
                .allowed(true)
                .limit(Integer.MAX_VALUE)
                .remaining(Integer.MAX_VALUE)
                .resetTime(0)
                .retryAfter(0)
                .build();
    }
}
```

#### RateLimitErrorResponse.java

```java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RateLimitErrorResponse {
    private String error;
    private String message;
    private int limit;
    private int remaining;
    private int resetTime;
    private int retryAfter;
    private Instant timestamp;
}
```

#### RateLimitConfig.java

```java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RateLimitConfig {
    private int limit;
    private int windowSeconds;
}
```

## Rate Limiting Strategy

### 1. Authentication Endpoints

#### Login Endpoint (`/api/v1/auth/login`)
- **Limit**: 5 attempts per 5 minutes per IP
- **Purpose**: Prevent brute-force login attacks
- **Key Strategy**: `login:{clientIp}`

#### Registration Endpoint (`/api/v1/auth/register`)
- **Limit**: 3 attempts per hour per IP
- **Purpose**: Prevent spam registration
- **Key Strategy**: `register:{clientIp}`

#### Password Reset Endpoint (`/api/v1/password/reset/*`)
- **Limit**: 3 attempts per 15 minutes per IP
- **Purpose**: Prevent email bombing and password reset abuse
- **Key Strategy**: `password-reset:{clientIp}`

### 2. General API Endpoints

- **Limit**: 100 requests per minute per IP
- **Purpose**: Prevent general API abuse
- **Key Strategy**: `api:{clientIp}:{method}`

## Redis Key Structure

Rate limiting data is stored in Redis using the following key pattern:

```
rate-limit:{endpoint-type}:{client-identifier}:{additional-context}
```

Examples:
- `rate-limit:login:192.168.1.100`
- `rate-limit:register:192.168.1.100`
- `rate-limit:api:192.168.1.100:POST`
- `rate-limit:password-reset:192.168.1.100`

## Response Headers

All rate-limited responses include the following headers:

- `X-RateLimit-Limit`: Maximum requests allowed in the time window
- `X-RateLimit-Remaining`: Number of requests remaining in the current window
- `X-RateLimit-Reset`: Time until the rate limit window resets (in seconds)
- `X-RateLimit-Retry-After`: Time to wait before making another request (only when limited)

## Error Response Format

When rate limits are exceeded, the API returns:

```json
{
    "error": "RATE_LIMIT_EXCEEDED",
    "message": "Rate limit exceeded. Please try again later.",
    "limit": 5,
    "remaining": 0,
    "resetTime": 300,
    "retryAfter": 300,
    "timestamp": "2025-11-23T15:30:00.000Z"
}
```

## Integration with Security Monitoring

The rate limiting service integrates with the broader security monitoring system:

### 1. Suspicious Activity Detection

```java
@Service
@RequiredArgsConstructor
public class SecurityMonitoringService {

    private final RateLimitingService rateLimitingService;
    private final SuspiciousActivityRepository activityRepository;

    public void recordRateLimitViolation(String clientIp, String endpoint) {
        // Record suspicious activity
        SuspiciousActivity activity = SuspiciousActivity.builder()
                .clientIp(clientIp)
                .activityType("RATE_LIMIT_VIOLATION")
                .endpoint(endpoint)
                .timestamp(LocalDateTime.now())
                .severity("HIGH")
                .build();

        activityRepository.save(activity);

        // Optionally block the IP temporarily
        if (shouldBlockIp(clientIp)) {
            blockIpTemporarily(clientIp);
        }
    }
}
```

### 2. IP Blacklisting Integration

Rate limit violations can trigger IP blacklisting:

```java
public void handleRateLimitExceeded(String clientIp, String endpoint) {
    // Check if this is a repeated violation
    int violationCount = getRecentViolationCount(clientIp);

    if (violationCount > 3) {
        // Add to IP blacklist
        blacklistService.addToBlacklist(clientIp, Duration.ofHours(1));
    }

    // Record the violation
    securityMonitoringService.recordRateLimitViolation(clientIp, endpoint);
}
```

## Monitoring and Metrics

### 1. Actuator Endpoints

Rate limiting metrics are exposed through Spring Boot Actuator:

```
GET /actuator/metrics/rate.limiter.requests
GET /actuator/metrics/rate.limiter.blocks
```

### 2. Custom Metrics

```java
@Component
@RequiredArgsConstructor
public class RateLimitingMetrics {

    private final MeterRegistry meterRegistry;

    public void recordRateLimitHit(String endpoint) {
        Counter.builder("rate.limiter.requests")
                .tag("endpoint", endpoint)
                .tag("result", "blocked")
                .register(meterRegistry)
                .increment();
    }

    public void recordRateLimitPass(String endpoint) {
        Counter.builder("rate.limiter.requests")
                .tag("endpoint", endpoint)
                .tag("result", "allowed")
                .register(meterRegistry)
                .increment();
    }
}
```

## Testing

### 1. Unit Tests

```java
@ExtendWith(MockitoExtension.class)
class RateLimitingServiceTest {

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private RateLimitingService rateLimitingService;

    @Test
    void testRateLimit_WithinLimit_Allowed() {
        // Given
        String key = "test-key";
        int limit = 5;
        int window = 60;

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment(any())).thenReturn(3L);

        // When
        RateLimitResponse response = rateLimitingService.checkRateLimit(key, limit, window);

        // Then
        assertThat(response.isAllowed()).isTrue();
        assertThat(response.getRemaining()).isEqualTo(2);
    }

    @Test
    void testRateLimit_ExceedsLimit_Blocked() {
        // Given
        String key = "test-key";
        int limit = 5;
        int window = 60;

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment(any())).thenReturn(6L);

        // When
        RateLimitResponse response = rateLimitingService.checkRateLimit(key, limit, window);

        // Then
        assertThat(response.isAllowed()).isFalse();
        assertThat(response.getRemaining()).isEqualTo(0);
        assertThat(response.getRetryAfter()).isEqualTo(60);
    }
}
```

### 2. Integration Tests

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
    "app.security.rate-limiting.enabled=true",
    "app.security.rate-limiting.max-attempts=2"
})
class RateLimitingIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void testRegistrationRateLimit() {
        String url = "/iam/api/v1/auth/register";
        String requestJson = """
            {
                "email": "test@example.com",
                "password": "TestPassword123!",
                "firstName": "Test",
                "lastName": "User"
            }
            """;

        // First request should succeed
        ResponseEntity<String> response1 = restTemplate.postForEntity(url,
                new HttpEntity<>(requestJson, getHeaders()), String.class);
        assertThat(response1.getStatusCode()).isNotEqualTo(HttpStatus.TOO_MANY_REQUESTS);

        // Second request should succeed
        ResponseEntity<String> response2 = restTemplate.postForEntity(url,
                new HttpEntity<>(requestJson, getHeaders()), String.class);
        assertThat(response2.getStatusCode()).isNotEqualTo(HttpStatus.TOO_MANY_REQUESTS);

        // Third request should be rate limited
        ResponseEntity<String> response3 = restTemplate.postForEntity(url,
                new HttpEntity<>(requestJson, getHeaders()), String.class);
        assertThat(response3.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);

        // Verify rate limit headers
        String remaining = response3.getHeaders().getFirst("X-RateLimit-Remaining");
        assertThat(remaining).isEqualTo("0");
    }
}
```

## Performance Considerations

### 1. Redis Connection Pool

- **Max Active Connections**: 50
- **Max Idle Connections**: 20
- **Min Idle Connections**: 5
- **Max Wait Time**: 1000ms

### 2. Key Expiration

- Rate limit keys automatically expire after their time window
- Redis memory usage is automatically managed through TTL

### 3. Fail-Safe Behavior

- If Redis is unavailable, rate limiting fails open (allows requests)
- Prevents service degradation due to rate limiting failures

## Security Considerations

### 1. IP Spoofing Protection

- Uses `X-Forwarded-For` header when behind load balancers
- Validates IP addresses before processing

### 2. Distributed Rate Limiting

- Redis ensures consistent rate limiting across multiple instances
- Prevents race conditions in distributed environments

### 3. Rate Limit Bypass Prevention

- Keys are based on multiple factors (IP, endpoint, method)
- Prevents simple bypass techniques

## Configuration Examples

### Development Environment

```properties
app.security.rate-limiting.enabled=false
```

### Production Environment

```properties
app.security.rate-limiting.enabled=true
app.security.rate-limiting.max-attempts=100
app.security.rate-limiting.window-seconds=60
app.security.rate-limiting.login-max-attempts=5
app.security.rate-limiting.login-window-seconds=300
app.security.rate-limiting.registration-max-attempts=3
app.security.rate-limiting.registration-window-seconds=3600
```

### High-Traffic Environment

```properties
app.security.rate-limiting.enabled=true
app.security.rate-limiting.max-attempts=1000
app.security.rate-limiting.window-seconds=60
spring.data.redis.lettuce.pool.max-active=200
spring.data.redis.lettuce.pool.max-idle=50
```

## Troubleshooting

### Common Issues

1. **Rate Limiting Not Working**
   - Check if `app.security.rate-limiting.enabled=true`
   - Verify Redis connection
   - Check filter ordering in security configuration

2. **All Requests Blocked**
   - Check Redis key expiration
   - Verify time window configuration
   - Check for key collisions

3. **Performance Issues**
   - Monitor Redis connection pool metrics
   - Check Redis memory usage
   - Consider key prefix optimization

### Debug Logging

```properties
logging.level.com.iscm.iam.security.RateLimitingService=DEBUG
logging.level.com.iscm.iam.security.RateLimitingFilter=DEBUG
```

## Future Enhancements

### 1. User-Based Rate Limiting

- Implement rate limiting per user ID for authenticated requests
- Higher limits for premium users

### 2. Adaptive Rate Limiting

- Dynamic rate limit adjustment based on system load
- Machine learning-based abuse detection

### 3. Geographic Rate Limiting

- Different rate limits based on geographic regions
- Country-specific rate limit policies

### 4. API Key Rate Limiting

- Rate limiting based on API keys for external integrations
- Tiered rate limiting based on subscription plans

---

**Last Updated**: November 23, 2025
**Version**: 1.0
**Author**: ISCM Development Team