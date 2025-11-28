package com.gateway;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
public class GatewayCorsConfiguration {

    @Value("${cors.allowed.origins:http://localhost:5173,http://localhost:5174,http://127.0.0.1:5173,http://127.0.0.1:5174}")
    private String[] allowedOrigins;

    @Bean
    public CorsWebFilter corsWebFilter() {
        CorsConfiguration corsConfig = new CorsConfiguration();

        // Allowed origins from configuration
        List<String> origins = Arrays.asList(allowedOrigins);
        corsConfig.setAllowedOrigins(origins);

        corsConfig.setAllowedMethods(Arrays.asList(
            "GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"
        ));

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
        corsConfig.setMaxAge(7200L); // 2 hours
        corsConfig.setExposedHeaders(Arrays.asList(
            "Authorization",
            "Content-Type",
            "X-Rate-Limit-Remaining"
        ));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();

        // Apply CORS to gateway entry paths
        source.registerCorsConfiguration("/iam/**", corsConfig);
        source.registerCorsConfiguration("/vendor/**", corsConfig);

        // Health/actuator – more relaxed
        CorsConfiguration healthCorsConfig = new CorsConfiguration();
        healthCorsConfig.setAllowedOrigins(List.of("*"));
        healthCorsConfig.setAllowedMethods(List.of("GET", "OPTIONS"));
        healthCorsConfig.setMaxAge(3600L);
        source.registerCorsConfiguration("/actuator/**", healthCorsConfig);

        return new CorsWebFilter(source);
    }
}
