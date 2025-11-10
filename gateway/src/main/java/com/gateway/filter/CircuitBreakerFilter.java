package com.gateway.filter;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.concurrent.TimeoutException;

@Slf4j
@Component
@RequiredArgsConstructor
public class CircuitBreakerFilter implements GlobalFilter, Ordered {

    private final io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry circuitBreakerRegistry;
    private final io.github.resilience4j.retry.RetryRegistry retryRegistry;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String serviceName = extractServiceName(exchange.getRequest());

        if (serviceName != null) {
            CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker(serviceName);
            io.github.resilience4j.retry.Retry retry = retryRegistry.retry(serviceName + "-retry");

            log.debug("Applying circuit breaker {} to request: {}", serviceName, exchange.getRequest().getPath());

            return chain.filter(exchange)
                    .transform(CircuitBreakerOperator.of(circuitBreaker))
                    .transform(io.github.resilience4j.reactor.retry.RetryOperator.of(retry))
                    .onErrorResume(TimeoutException.class, e -> {
                        log.warn("Request timeout for service {}: {}", serviceName, e.getMessage());
                        return handleTimeout(exchange, serviceName);
                    })
                    .onErrorResume(e -> {
                        log.error("Request failed for service {}: {}", serviceName, e.getMessage());
                        return handleError(exchange, serviceName, e);
                    });
        }

        return chain.filter(exchange);
    }

    @Override
    public int getOrder() {
        return -1; // High precedence to apply circuit breaker early
    }

    private String extractServiceName(ServerHttpRequest request) {
        String path = request.getPath().value();

        // Extract service name based on path patterns
        if (path.startsWith("/iam/")) {
            return "iam-service";
        } else if (path.startsWith("/users/")) {
            return "user-service";
        } else if (path.startsWith("/auth/")) {
            return "auth-service";
        } else if (path.startsWith("/email/")) {
            return "email-service";
        } else if (path.startsWith("/notifications/")) {
            return "notification-service";
        }

        return null;
    }

    private Mono<Void> handleTimeout(ServerWebExchange exchange, String serviceName) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.GATEWAY_TIMEOUT);
        response.getHeaders().add("Content-Type", "application/json");

        String body = String.format("""
            {
                "error": "SERVICE_TIMEOUT",
                "message": "Service %s is currently experiencing high latency",
                "service": "%s",
                "timestamp": "%s"
            }
            """, serviceName, serviceName, java.time.Instant.now());

        DataBuffer buffer = response.bufferFactory().wrap(body.getBytes());
        return response.writeWith(Mono.just(buffer));
    }

    private Mono<Void> handleError(ServerWebExchange exchange, String serviceName, Throwable e) {
        ServerHttpResponse response = exchange.getResponse();

        // Check if circuit breaker is open
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker(serviceName);
        if (circuitBreaker.getState() == CircuitBreaker.State.OPEN) {
            response.setStatusCode(HttpStatus.SERVICE_UNAVAILABLE);
            response.getHeaders().add("Content-Type", "application/json");

            String body = String.format("""
                {
                    "error": "SERVICE_UNAVAILABLE",
                    "message": "Service %s is currently unavailable. Circuit breaker is OPEN.",
                    "service": "%s",
                    "retryAfter": "30s",
                    "timestamp": "%s"
                }
                """, serviceName, serviceName, java.time.Instant.now());

            DataBuffer buffer = response.bufferFactory().wrap(body.getBytes());
            return response.writeWith(Mono.just(buffer));
        }

        // Generic error response
        response.setStatusCode(HttpStatus.BAD_GATEWAY);
        response.getHeaders().add("Content-Type", "application/json");

        String body = String.format("""
            {
                "error": "SERVICE_ERROR",
                "message": "Service %s encountered an error",
                "service": "%s",
                "timestamp": "%s"
            }
            """, serviceName, serviceName, java.time.Instant.now());

        DataBuffer buffer = response.bufferFactory().wrap(body.getBytes());
        return response.writeWith(Mono.just(buffer));
    }
}