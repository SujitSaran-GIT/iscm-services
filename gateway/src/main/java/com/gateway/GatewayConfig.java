package com.gateway;

// import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import org.springframework.http.HttpMethod;
import org.springframework.beans.factory.annotation.Value;
// import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
// import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
// import org.springframework.context.annotation.Primary;
// import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
// import org.springframework.data.redis.core.ReactiveRedisTemplate;
// import org.springframework.data.redis.serializer.RedisSerializationContext;
// import org.springframework.data.redis.serializer.StringRedisSerializer;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.Set;

@Configuration
public class GatewayConfig {

    // @Value("${spring.redis.host:localhost}")
    // private String redisHost;

    // @Value("${spring.redis.port:6379}")
    // private int redisPort;

    @Value("${iam.service.url:http://localhost:8081}")
    private String iamServiceUrl;

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                .route("iam-service", r -> r.path("/iam/**")
                        .filters(f -> f
                                // Rewrite path to remove /iam prefix
                                .rewritePath("/iam/(?<segment>.*)", "/iam/${segment}")

                                // Circuit breaker configuration - DISABLED
                                // .circuitBreaker(c -> c
                                //         .setName("iam-circuit-breaker")
                                //         .setFallbackUri("forward:/fallback/iam")
                                //         .setStatusCodes(Set.of("500", "502", "503", "504"))
                                // )

                                // Optimized retry configuration
                                .retry(retryConfig -> retryConfig
                                        .setRetries(2)  // Reduced from 3 to 2
                                        .setBackoff(Duration.ofMillis(200), Duration.ofSeconds(2), 2, false)
                                        .setMethods(HttpMethod.GET, HttpMethod.POST)
                                        .setExceptions(
                                            org.springframework.cloud.gateway.support.TimeoutException.class,
                                            java.net.ConnectException.class,
                                            java.net.SocketTimeoutException.class
                                        )
                                )

                                // Rate limiting - DISABLED
                                // .requestRateLimiter(rateLimiter -> rateLimiter
                                //         .setRateLimiter(redisRateLimiter())
                                //         .setKeyResolver(userKeyResolver())
                                //         .setDenyEmptyKey(false)
                                // )

                                // Add request headers
                                .addRequestHeader("X-Gateway-Request", "true")
                                .addRequestHeader("X-Request-Time", String.valueOf(System.currentTimeMillis()))
                        )
                        .uri(iamServiceUrl))

                // Health check route
                .route("health", r -> r.path("/actuator/health")
                        .filters(f -> f
                                .stripPrefix(0)
                                // .circuitBreaker(c -> c.setName("health-circuit-breaker"))
                        )
                        .uri("http://localhost:7070"))

                .build();
    }

    // @Bean
    // public RedisRateLimiter redisRateLimiter() {
    //     // Increased to 20 requests per second, burst of 40 requests for better performance
    //     return new RedisRateLimiter(20, 40, 1);
    // }

    // @Bean
    // public KeyResolver userKeyResolver() {
    //     return exchange -> {
    //         String path = exchange.getRequest().getPath().value();
    //         String clientIp = exchange.getRequest().getRemoteAddress() != null
    //             ? exchange.getRequest().getRemoteAddress().getAddress().getHostAddress()
    //             : "anonymous";

    //         // Optimized key resolution with caching
    //         if (path.contains("/auth")) {
    //             // Faster IP resolution for auth endpoints
    //             String forwardedFor = exchange.getRequest().getHeaders().getFirst("X-Forwarded-For");
    //             String ip = forwardedFor != null ? forwardedFor.split(",")[0].trim() : clientIp;
    //             return Mono.just("auth:" + ip);
    //         }

    //         // For authenticated users
    //         String userId = exchange.getRequest().getHeaders().getFirst("X-User-ID");
    //         if (userId != null) {
    //             return Mono.just("user:" + userId);
    //         }

    //         // Fallback to IP-based rate limiting
    //         return Mono.just("ip:" + clientIp);
    //     };
    }


    // @Bean
    // @Primary
    // public ReactiveRedisTemplate<String, String> reactiveRedisTemplate(
    //         ReactiveRedisConnectionFactory factory) {
    //     StringRedisSerializer keySerializer = new StringRedisSerializer();
    //     RedisSerializationContext<String, String> context = RedisSerializationContext
    //             .<String, String>newSerializationContext()
    //             .key(keySerializer)
    //             .value(keySerializer)
    //             .hashKey(keySerializer)
    //             .hashValue(keySerializer)
    //             .build();

    //     return new ReactiveRedisTemplate<>(factory, context);
    // }

    // }