package com.iscm.iam.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class SecurityHeadersFilter extends OncePerRequestFilter {

    private static final List<String> ALLOWED_ORIGINS = Arrays.asList(
        "http://localhost:5173",
        "http://localhost:5174",
        "http://127.0.0.1:5173",
        "http://127.0.0.1:5174"
    );

    private static final List<String> ALLOWED_METHODS = Arrays.asList(
        "GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"
    );

    private static final List<String> ALLOWED_HEADERS = Arrays.asList(
        "Authorization",
        "Content-Type",
        "X-Requested-With",
        "Accept",
        "Origin",
        "Access-Control-Request-Method",
        "Access-Control-Request-Headers",
        "X-Client-Version",
        "X-Device-ID",
        "X-Request-ID"
    );

    private static final List<String> EXPOSED_HEADERS = Arrays.asList(
        "X-Total-Count",
        "X-RateLimit-Limit",
        "X-RateLimit-Remaining",
        "X-RateLimit-Reset"
    );

    private static final long MAX_AGE = 3600; // 1 hour

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // Add security headers
        addSecurityHeaders(request, response);

        // Handle CORS preflight requests
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            handleCorsPreflight(request, response);
            return;
        }

        // Add CORS headers for actual requests
        addCorsHeaders(request, response);

        filterChain.doFilter(request, response);
    }

    private void addSecurityHeaders(HttpServletRequest request, HttpServletResponse response) {
        // Content Security Policy
        response.setHeader("Content-Security-Policy",
            "default-src 'self'; " +
            "script-src 'self' 'unsafe-inline' 'unsafe-eval'; " +
            "style-src 'self' 'unsafe-inline'; " +
            "img-src 'self' data: https:; " +
            "font-src 'self'; " +
            "connect-src 'self'; " +
            "frame-ancestors 'none'; " +
            "base-uri 'self'; " +
            "form-action 'self'");

        // Prevent clickjacking
        response.setHeader("X-Frame-Options", "DENY");

        // Prevent MIME type sniffing
        response.setHeader("X-Content-Type-Options", "nosniff");

        // XSS Protection
        response.setHeader("X-XSS-Protection", "1; mode=block");

        // Referrer Policy
        response.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");

        // Strict Transport Security (HTTPS only)
        if (request.isSecure()) {
            response.setHeader("Strict-Transport-Security",
                "max-age=31536000; includeSubDomains; preload");
        }

        // Permissions Policy
        response.setHeader("Permissions-Policy",
            "geolocation=(), microphone=(), camera=(), " +
            "payment=(), usb=(), magnetometer=(), gyroscope=()");

        // Content Type Options
        response.setHeader("X-Content-Type-Options", "nosniff");

        // Custom security headers
        response.setHeader("X-Server-Version", "1.0.0"); // Should be configurable
        response.setHeader("X-Powered-By", "Spring Boot");

        // Cache control for security-sensitive endpoints
        String path = request.getRequestURI();
        if (isSecuritySensitiveEndpoint(path)) {
            response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
            response.setHeader("Pragma", "no-cache");
            response.setHeader("Expires", "0");
        }

        log.debug("Security headers added for request: {} {}", request.getMethod(), path);
    }

    private void addCorsHeaders(HttpServletRequest request, HttpServletResponse response) {
        String origin = request.getHeader("Origin");

        // Check if origin is allowed
        if (origin != null && ALLOWED_ORIGINS.contains(origin)) {
            response.setHeader("Access-Control-Allow-Origin", origin);
        } else if (origin != null) {
            log.warn("Blocked CORS request from unauthorized origin: {}", origin);
            return;
        }

        response.setHeader("Access-Control-Allow-Methods", String.join(", ", ALLOWED_METHODS));
        response.setHeader("Access-Control-Allow-Headers", String.join(", ", ALLOWED_HEADERS));
        response.setHeader("Access-Control-Expose-Headers", String.join(", ", EXPOSED_HEADERS));
        response.setHeader("Access-Control-Allow-Credentials", "true");
        response.setHeader("Access-Control-Max-Age", String.valueOf(MAX_AGE));

        // Vary header for proper caching
        response.setHeader("Vary", "Origin");
    }

    private void handleCorsPreflight(HttpServletRequest request, HttpServletResponse response) {
        String origin = request.getHeader("Origin");
        String requestMethod = request.getHeader("Access-Control-Request-Method");
        String requestHeaders = request.getHeader("Access-Control-Request-Headers");

        log.debug("CORS preflight request: origin={}, method={}, headers={}",
                 origin, requestMethod, requestHeaders);

        // Check if origin is allowed
        if (origin == null || !ALLOWED_ORIGINS.contains(origin)) {
            log.warn("Blocked CORS preflight from unauthorized origin: {}", origin);
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        // Check if method is allowed
        if (requestMethod != null && !ALLOWED_METHODS.contains(requestMethod)) {
            log.warn("Blocked CORS preflight with unauthorized method: {}", requestMethod);
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        // Add CORS headers
        response.setHeader("Access-Control-Allow-Origin", origin);
        response.setHeader("Access-Control-Allow-Methods", String.join(", ", ALLOWED_METHODS));
        response.setHeader("Access-Control-Allow-Headers", String.join(", ", ALLOWED_HEADERS));
        response.setHeader("Access-Control-Expose-Headers", String.join(", ", EXPOSED_HEADERS));
        response.setHeader("Access-Control-Allow-Credentials", "true");
        response.setHeader("Access-Control-Max-Age", String.valueOf(MAX_AGE));
        response.setHeader("Vary", "Origin");

        response.setStatus(HttpServletResponse.SC_OK);
    }

    private boolean isSecuritySensitiveEndpoint(String path) {
        return path.contains("/auth/") ||
               path.contains("/mfa/") ||
               path.contains("/password/") ||
               path.contains("/admin/") ||
               path.contains("/oauth/");
    }
}