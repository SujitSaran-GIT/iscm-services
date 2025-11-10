package com.gateway.config;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import io.github.resilience4j.timelimiter.TimeLimiter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Slf4j
@Configuration
public class CircuitBreakerConfig {

    @Value("${app.circuit-breaker.failure-rate-threshold:50}")
    private int failureRateThreshold;

    @Value("${app.circuit-breaker.wait-duration-in-open-state:30000}")
    private int waitDurationInOpenState;

    @Value("${app.circuit-breaker.sliding-window-size:10}")
    private int slidingWindowSize;

    @Value("${app.circuit-breaker.minimum-number-of-calls:5}")
    private int minimumNumberOfCalls;

    @Value("${app.circuit-breaker.permitted-number-of-calls-in-half-open-state:3}")
    private int permittedNumberOfCallsInHalfOpenState;

    @Value("${app.circuit-breaker.slow-call-duration-threshold:2000}")
    private int slowCallDurationThreshold;

    @Value("${app.circuit-breaker.slow-call-rate-threshold:50}")
    private int slowCallRateThreshold;

    @Value("${app.circuit-breaker.timeout-duration:5000}")
    private int timeoutDuration;

    @Bean
    public CircuitBreakerRegistry circuitBreakerRegistry() {
        io.github.resilience4j.circuitbreaker.CircuitBreakerConfig circuitBreakerConfig = io.github.resilience4j.circuitbreaker.CircuitBreakerConfig.custom()
                .failureRateThreshold(failureRateThreshold)
                .waitDurationInOpenState(Duration.ofMillis(waitDurationInOpenState))
                .slidingWindowSize(slidingWindowSize)
                .minimumNumberOfCalls(minimumNumberOfCalls)
                .permittedNumberOfCallsInHalfOpenState(permittedNumberOfCallsInHalfOpenState)
                .slowCallDurationThreshold(Duration.ofMillis(slowCallDurationThreshold))
                .slowCallRateThreshold(slowCallRateThreshold)
                .build();

        return CircuitBreakerRegistry.of(circuitBreakerConfig);
    }

    @Bean
    public RetryRegistry retryRegistry() {
        io.github.resilience4j.retry.RetryConfig retryConfig = io.github.resilience4j.retry.RetryConfig.custom()
                .maxAttempts(3)
                .waitDuration(Duration.ofMillis(1000))
                .retryExceptions(Exception.class)
                .build();

        return RetryRegistry.of(retryConfig);
    }

    @Bean
    public TimeLimiter timeLimiter() {
        io.github.resilience4j.timelimiter.TimeLimiterConfig timeLimiterConfig = io.github.resilience4j.timelimiter.TimeLimiterConfig.custom()
                .timeoutDuration(Duration.ofMillis(timeoutDuration))
                .build();

        return TimeLimiter.of(timeLimiterConfig);
    }

    @Bean
    public CircuitBreaker iamServiceCircuitBreaker(CircuitBreakerRegistry registry) {
        CircuitBreaker circuitBreaker = registry.circuitBreaker("iam-service");

        circuitBreaker.getEventPublisher()
                .onStateTransition(event ->
                    log.info("IAM Service Circuit Breaker state transition: {} -> {}",
                        event.getStateTransition().getFromState(),
                        event.getStateTransition().getToState()))
                .onFailureRateExceeded(event ->
                    log.warn("IAM Service Circuit Breaker failure rate exceeded: {}%",
                        event.getFailureRate()))
                .onSlowCallRateExceeded(event ->
                    log.warn("IAM Service Circuit Breaker slow call rate exceeded: {}%",
                        event.getSlowCallRate()))
                .onCallNotPermitted(event ->
                    log.warn("IAM Service Circuit Breaker call not permitted - Circuit is OPEN"));

        return circuitBreaker;
    }

    @Bean
    public CircuitBreaker userServiceCircuitBreaker(CircuitBreakerRegistry registry) {
        CircuitBreaker circuitBreaker = registry.circuitBreaker("user-service");

        circuitBreaker.getEventPublisher()
                .onStateTransition(event ->
                    log.info("User Service Circuit Breaker state transition: {} -> {}",
                        event.getStateTransition().getFromState(),
                        event.getStateTransition().getToState()));

        return circuitBreaker;
    }

    @Bean
    public CircuitBreaker authServiceCircuitBreaker(CircuitBreakerRegistry registry) {
        CircuitBreaker circuitBreaker = registry.circuitBreaker("auth-service");

        circuitBreaker.getEventPublisher()
                .onStateTransition(event ->
                    log.info("Auth Service Circuit Breaker state transition: {} -> {}",
                        event.getStateTransition().getFromState(),
                        event.getStateTransition().getToState()));

        return circuitBreaker;
    }

    @Bean
    public CircuitBreaker emailServiceCircuitBreaker(CircuitBreakerRegistry registry) {
        CircuitBreaker circuitBreaker = registry.circuitBreaker("email-service");

        circuitBreaker.getEventPublisher()
                .onStateTransition(event ->
                    log.info("Email Service Circuit Breaker state transition: {} -> {}",
                        event.getStateTransition().getFromState(),
                        event.getStateTransition().getToState()));

        return circuitBreaker;
    }

    @Bean
    public CircuitBreaker notificationServiceCircuitBreaker(CircuitBreakerRegistry registry) {
        CircuitBreaker circuitBreaker = registry.circuitBreaker("notification-service");

        circuitBreaker.getEventPublisher()
                .onStateTransition(event ->
                    log.info("Notification Service Circuit Breaker state transition: {} -> {}",
                        event.getStateTransition().getFromState(),
                        event.getStateTransition().getToState()));

        return circuitBreaker;
    }
}