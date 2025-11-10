package com.gateway;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/fallback")
public class FallbackController {

    @GetMapping("/iam")
    public Mono<ResponseEntity<Map<String, Object>>> iamFallback() {
        log.warn("IAM service fallback triggered at {}", LocalDateTime.now());

        Map<String, Object> response = new HashMap<>();
        response.put("service", "IAM Service");
        response.put("status", "TEMPORARILY_UNAVAILABLE");
        response.put("message", "The Identity and Access Management service is temporarily unavailable. Please try again later.");
        response.put("timestamp", LocalDateTime.now());
        response.put("retryAfter", "30 seconds");

        return Mono.just(ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(response));
    }

    @GetMapping("/default")
    public Mono<ResponseEntity<Map<String, Object>>> defaultFallback() {
        log.warn("Default fallback triggered at {}", LocalDateTime.now());

        Map<String, Object> response = new HashMap<>();
        response.put("service", "Gateway");
        response.put("status", "SERVICE_UNAVAILABLE");
        response.put("message", "A required service is temporarily unavailable. Please try again later.");
        response.put("timestamp", LocalDateTime.now());

        return Mono.just(ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(response));
    }
}