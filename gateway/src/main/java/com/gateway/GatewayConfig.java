package com.gateway;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;

import java.time.Duration;

@Configuration
public class GatewayConfig {

    @Value("${iam.service.url}")
    private String iamServiceUrl;      // e.g. http://localhost:8081

    @Value("${vendor.service.url}")
    private String vendorServiceUrl;   // e.g. http://localhost:8082

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()

            // ============= IAM SERVICE ROUTE =============
            .route("iam-service", r -> r
                .path("/iam/**")
                .filters(f -> f
                    // Remove /iam from the path sent to IAM service
                    // Gateway: /iam/api/auth/login -> Backend: /api/auth/login
                    .rewritePath("/iam/(?<segment>.*)", "/iam/${segment}")
                    .retry(retryConfig -> retryConfig
                        .setRetries(2)
                        .setBackoff(
                            Duration.ofMillis(200),
                            Duration.ofSeconds(2),
                            2,
                            false
                        )
                        .setMethods(HttpMethod.GET, HttpMethod.POST)
                        .setExceptions(
                            org.springframework.cloud.gateway.support.TimeoutException.class,
                            java.net.ConnectException.class,
                            java.net.SocketTimeoutException.class
                        )
                    )
                    .addRequestHeader("X-Gateway-Request", "iam-service")
                    .addRequestHeader("X-Request-Time", String.valueOf(System.currentTimeMillis()))
                )
                .uri(iamServiceUrl) // http://localhost:8081
            )

            // ============= VENDOR SERVICE ROUTE =============
            .route("vendor-service", r -> r
                .path("/vendor/**")
                .filters(f -> f
                    // Gateway: /vendor/api/v1/vendors -> Backend: /api/v1/vendors
                    .rewritePath("/vendor/(?<segment>.*)", "/vendor/${segment}")
                    .retry(retryConfig -> retryConfig
                        .setRetries(2)
                        .setBackoff(
                            Duration.ofMillis(200),
                            Duration.ofSeconds(2),
                            2,
                            false
                        )
                        .setMethods(
                            HttpMethod.GET,
                            HttpMethod.POST,
                            HttpMethod.PUT,
                            HttpMethod.PATCH,
                            HttpMethod.DELETE
                        )
                        .setExceptions(
                            org.springframework.cloud.gateway.support.TimeoutException.class,
                            java.net.ConnectException.class,
                            java.net.SocketTimeoutException.class
                        )
                    )
                    .addRequestHeader("X-Gateway-Request", "vendor-service")
                    .addRequestHeader("X-Request-Time", String.valueOf(System.currentTimeMillis()))
                )
                .uri(vendorServiceUrl) // http://localhost:8082
            )

            .build();
    }
}
