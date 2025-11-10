package com.gateway.controller;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/actuator/circuit-breakers")
@RequiredArgsConstructor
public class CircuitBreakerController implements HealthIndicator {

    private final CircuitBreakerRegistry circuitBreakerRegistry;

    @Override
    public Health health() {
        Map<String, Object> details = new HashMap<>();
        Map<String, CircuitBreaker.State> circuitStates = new HashMap<>();

        circuitBreakerRegistry.getAllCircuitBreakers().forEach(circuitBreaker -> {
            String name = circuitBreaker.getName();
            CircuitBreaker.State state = circuitBreaker.getState();
            circuitStates.put(name, state);

            details.put(name, Map.of(
                "state", state.toString(),
                "failureRate", circuitBreaker.getMetrics().getFailureRate(),
                "bufferedCalls", circuitBreaker.getMetrics().getNumberOfBufferedCalls(),
                "failedCalls", circuitBreaker.getMetrics().getNumberOfFailedCalls(),
                "successCalls", circuitBreaker.getMetrics().getNumberOfSuccessfulCalls(),
                "notPermittedCalls", circuitBreaker.getMetrics().getNumberOfNotPermittedCalls()
            ));
        });

        // Determine overall health
        boolean hasOpenCircuits = circuitStates.values().stream()
                .anyMatch(state -> state == CircuitBreaker.State.OPEN);

        Health.Builder healthBuilder = hasOpenCircuits ? Health.down() : Health.up();
        return healthBuilder
                .withDetail("circuitBreakers", details)
                .withDetail("totalCircuits", circuitStates.size())
                .withDetail("openCircuits", circuitStates.values().stream()
                        .filter(state -> state == CircuitBreaker.State.OPEN)
                        .count())
                .build();
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllCircuitBreakers() {
        Map<String, Object> response = new HashMap<>();

        circuitBreakerRegistry.getAllCircuitBreakers().forEach(circuitBreaker -> {
            Map<String, Object> details = new HashMap<>();
            details.put("state", circuitBreaker.getState().toString());
            details.put("failureRate", circuitBreaker.getMetrics().getFailureRate());
            details.put("bufferedCalls", circuitBreaker.getMetrics().getNumberOfBufferedCalls());
            details.put("failedCalls", circuitBreaker.getMetrics().getNumberOfFailedCalls());
            details.put("successCalls", circuitBreaker.getMetrics().getNumberOfSuccessfulCalls());
            details.put("notPermittedCalls", circuitBreaker.getMetrics().getNumberOfNotPermittedCalls());

            if (circuitBreaker.getState() == CircuitBreaker.State.OPEN) {
                details.put("retryAfter", "circuit is open");
            }

            response.put(circuitBreaker.getName(), details);
        });

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{serviceName}")
    public ResponseEntity<Map<String, Object>> getCircuitBreakerStatus(@PathVariable String serviceName) {
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker(serviceName);

        if (circuitBreaker == null) {
            return ResponseEntity.notFound().build();
        }

        Map<String, Object> details = new HashMap<>();
        details.put("name", circuitBreaker.getName());
        details.put("state", circuitBreaker.getState().toString());
        details.put("failureRate", circuitBreaker.getMetrics().getFailureRate());
        details.put("bufferedCalls", circuitBreaker.getMetrics().getNumberOfBufferedCalls());
        details.put("failedCalls", circuitBreaker.getMetrics().getNumberOfFailedCalls());
        details.put("successCalls", circuitBreaker.getMetrics().getNumberOfSuccessfulCalls());
        details.put("notPermittedCalls", circuitBreaker.getMetrics().getNumberOfNotPermittedCalls());
        details.put("slowCalls", circuitBreaker.getMetrics().getNumberOfSlowCalls());
        details.put("slowCallRate", circuitBreaker.getMetrics().getSlowCallRate());

        // Configuration details
        details.put("config", Map.of(
            "failureRateThreshold", circuitBreaker.getCircuitBreakerConfig().getFailureRateThreshold(),
            "slidingWindowSize", circuitBreaker.getCircuitBreakerConfig().getSlidingWindowSize(),
            "minimumNumberOfCalls", circuitBreaker.getCircuitBreakerConfig().getMinimumNumberOfCalls(),
            "permittedNumberOfCallsInHalfOpenState", circuitBreaker.getCircuitBreakerConfig().getPermittedNumberOfCallsInHalfOpenState(),
            "slowCallRateThreshold", circuitBreaker.getCircuitBreakerConfig().getSlowCallRateThreshold()
        ));

        return ResponseEntity.ok(details);
    }

    @GetMapping("/{serviceName}/reset")
    public ResponseEntity<Map<String, String>> resetCircuitBreaker(@PathVariable String serviceName) {
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker(serviceName);

        if (circuitBreaker == null) {
            return ResponseEntity.notFound().build();
        }

        // Transition to closed state
        if (circuitBreaker.getState() != CircuitBreaker.State.CLOSED) {
            circuitBreaker.transitionToClosedState();
            log.info("Circuit breaker {} manually reset to CLOSED state", serviceName);
        }

        return ResponseEntity.ok(Map.of(
            "message", "Circuit breaker " + serviceName + " has been reset to CLOSED state",
            "currentState", circuitBreaker.getState().toString()
        ));
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> getCircuitBreakersHealth() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", health().getStatus().getCode());
        health.put("details", health().getDetails());
        return ResponseEntity.ok(health);
    }
}