package com.gateway;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.beans.factory.annotation.Value;

import java.util.Arrays;
import java.util.List;

@Configuration
public class GatewayCorsConfiguration {

    @Value("${cors.allowed.origins:http://localhost:5173,http://localhost:5174,http://127.0.0.1:5173,http://127.0.0.1:5174}")
    private String[] allowedOrigins;

    @Bean
    public CorsWebFilter corsWebFilter() {
        CorsConfiguration corsConfig = new CorsConfiguration();

        // Optimized: Use configurable origins and reduce regex operations
        List<String> origins = Arrays.asList(allowedOrigins);
        corsConfig.setAllowedOrigins(origins);

        // Optimized: Pre-computed method list
        corsConfig.setAllowedMethods(Arrays.asList(
            "GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"
        ));

        // Optimized: Essential headers only for better performance
        corsConfig.setAllowedHeaders(Arrays.asList(
            "Authorization",
            "Content-Type",
            "X-Requested-With",
            "Accept",
            "Origin",
            "Access-Control-Request-Method",
            "Access-Control-Request-Headers",
            "X-User-ID",
            "X-Forwarded-For"
        ));

        corsConfig.setAllowCredentials(true);
        corsConfig.setMaxAge(7200L); // Increased from 3600L to reduce pre-flight requests
        corsConfig.setExposedHeaders(Arrays.asList("Authorization", "Content-Type", "X-Rate-Limit-Remaining"));

        // Optimized: Apply CORS only to API paths, not to health endpoints
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/iam/**", corsConfig);
        source.registerCorsConfiguration("/api/**", corsConfig);

        // For health checks, minimal CORS
        CorsConfiguration healthCorsConfig = new CorsConfiguration();
        healthCorsConfig.setAllowedOrigins(Arrays.asList("*"));
        healthCorsConfig.setAllowedMethods(Arrays.asList("GET", "OPTIONS"));
        healthCorsConfig.setMaxAge(3600L);
        source.registerCorsConfiguration("/actuator/**", healthCorsConfig);

        return new CorsWebFilter(source);
    }
}
