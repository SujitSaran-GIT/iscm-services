package com.iscm.iam.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

@Slf4j
//@Component
//@Order(Ordered.HIGHEST_PRECEDENCE + 1) // Execute after input validation
public class RateLimitingFilter extends OncePerRequestFilter {

    // @Autowired - disabled since rate limiting is disabled
    private RateLimitingService rateLimitingService = null;

    // Cache for request patterns to improve performance
    private final Map<String, RequestPattern> patternCache = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        try {
            // Rate limiting is disabled - just continue
            filterChain.doFilter(request, response);

        } catch (Exception e) {
            log.error("Rate limiting filter error", e);
            filterChain.doFilter(request, response); // Continue on error
        }
    }

    private boolean shouldApplyRateLimiting(String path, String method) {
        // Don't rate limit health checks and static resources
        if (path.startsWith("/actuator") ||
            path.startsWith("/health") ||
            path.startsWith("/favicon.ico") ||
            path.startsWith("/static/") ||
            path.endsWith(".css") ||
            path.endsWith(".js") ||
            path.endsWith(".png") ||
            path.endsWith(".jpg") ||
            path.endsWith(".gif")) {
            return false;
        }

        return true;
    }

    private boolean applyRateLimiting(HttpServletRequest request, String clientIp, HttpServletResponse response)
            throws IOException {

        String path = request.getRequestURI();
        String method = request.getMethod();

        // Get request pattern
        RequestPattern pattern = getRequestPattern(path, method);

        // Check global rate limiting
        if (rateLimitingService.isGloballyRateLimited(clientIp)) {
            log.warn("Global rate limit exceeded for IP: {}", clientIp);
            sendRateLimitResponse(response, "Too many requests. Please try again later.",
                                rateLimitingService.getRateLimitStatus(clientIp, 1000, Duration.ofMinutes(1)));
            return true;
        }

        // Apply specific rate limiting based on request pattern
        boolean isRateLimited = switch (pattern.getType()) {
            case AUTH_LOGIN -> rateLimitingService.isLoginRateLimited(clientIp);
            case AUTH_REGISTER -> rateLimitingService.isRegistrationRateLimited(clientIp);
            case AUTH_PASSWORD_RESET -> rateLimitingService.isPasswordResetRateLimited(extractEmailFromRequest(request));
            case AUTH_MFA -> rateLimitingService.isMfaRateLimited(extractUserIdFromRequest(request));
            case API_SENSITIVE -> isRateLimited(clientIp, pattern);
            case API_GENERAL -> isRateLimited(clientIp, pattern);
            default -> false;
        };

        if (isRateLimited) {
            log.warn("Rate limit exceeded for IP: {}, path: {}, method: {}", clientIp, path, method);

            // Record failed attempt
            rateLimitingService.recordFailedAttempt(clientIp, pattern.getType().toString().toLowerCase());

            // Send rate limit response
            RateLimitingService.RateLimitStatus status = rateLimitingService.getRateLimitStatus(
                clientIp, pattern.getLimit(), Duration.ofSeconds(pattern.getWindowSeconds()));
            sendRateLimitResponse(response, "Rate limit exceeded. Please try again later.", status);
            return true;
        }

        // Record successful request
        rateLimitingService.recordRequest(clientIp);
        return false;
    }

    private boolean isRateLimited(String clientIp, RequestPattern pattern) {
        return rateLimitingService.isRateLimitedSlidingWindow(
            clientIp, pattern.getLimit(), Duration.ofSeconds(pattern.getWindowSeconds())
        );
    }

    private RequestPattern getRequestPattern(String path, String method) {
        String key = path + ":" + method;

        return patternCache.computeIfAbsent(key, k -> {
            if (path.startsWith("/iam/api/v1/auth/login")) {
                return RequestPattern.builder()
                    .type(RequestPattern.RequestType.AUTH_LOGIN)
                    .limit(5) // 5 login attempts per minute
                    .windowSeconds(60)
                    .build();
            } else if (path.startsWith("/iam/api/v1/auth/register")) {
                return RequestPattern.builder()
                    .type(RequestPattern.RequestType.AUTH_REGISTER)
                    .limit(3) // 3 registration attempts per hour
                    .windowSeconds(3600)
                    .build();
            } else if (path.startsWith("/iam/api/v1/auth/password/reset")) {
                return RequestPattern.builder()
                    .type(RequestPattern.RequestType.AUTH_PASSWORD_RESET)
                    .limit(3) // 3 password reset attempts per hour
                    .windowSeconds(3600)
                    .build();
            } else if (path.contains("/mfa/")) {
                return RequestPattern.builder()
                    .type(RequestPattern.RequestType.AUTH_MFA)
                    .limit(10) // 10 MFA attempts per 5 minutes
                    .windowSeconds(300)
                    .build();
            } else if (path.startsWith("/iam/api/v1/users/") && method.equals("POST")) {
                return RequestPattern.builder()
                    .type(RequestPattern.RequestType.API_SENSITIVE)
                    .limit(10) // 10 user creation attempts per minute
                    .windowSeconds(60)
                    .build();
            } else if (path.startsWith("/iam/api/v1/admin/")) {
                return RequestPattern.builder()
                    .type(RequestPattern.RequestType.API_SENSITIVE)
                    .limit(100) // 100 admin requests per minute
                    .windowSeconds(60)
                    .build();
            } else {
                return RequestPattern.builder()
                    .type(RequestPattern.RequestType.API_GENERAL)
                    .limit(1000) // 1000 general requests per minute
                    .windowSeconds(60)
                    .build();
            }
        });
    }

    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty() && !"unknown".equalsIgnoreCase(xForwardedFor)) {
            // X-Forwarded-For can contain multiple IPs, take the first one (original client)
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty() && !"unknown".equalsIgnoreCase(xRealIp)) {
            return xRealIp.trim();
        }

        return request.getRemoteAddr();
    }

    private String extractEmailFromRequest(HttpServletRequest request) {
        // Extract email from request body or parameters
        // This is a simplified implementation - in practice, you might want to parse the request body
        String email = request.getParameter("email");
        return email != null ? email : "unknown";
    }

    private String extractUserIdFromRequest(HttpServletRequest request) {
        // Extract user ID from request or JWT token
        // This is a simplified implementation
        String userId = request.getParameter("userId");
        if (userId == null) {
            // Try to extract from Authorization header (JWT token)
            String authHeader = request.getHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                // In practice, you would parse the JWT token here
                userId = "from-jwt";
            }
        }
        return userId != null ? userId : "unknown";
    }

    private void sendRateLimitResponse(HttpServletResponse response, String message, RateLimitingService.RateLimitStatus status)
            throws IOException {

        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType("application/json");
        response.setHeader("X-RateLimit-Limit", String.valueOf(status.getLimit()));
        response.setHeader("X-RateLimit-Remaining", String.valueOf(status.getRemaining()));
        response.setHeader("X-RateLimit-Reset", String.valueOf(status.getResetTimeSeconds()));

        String jsonResponse = String.format("""
            {
                "error": "RATE_LIMIT_EXCEEDED",
                "message": "%s",
                "limit": %d,
                "remaining": %d,
                "resetTime": %d,
                "retryAfter": %d
            }
            """, message, status.getLimit(), status.getRemaining(), status.getResetTimeSeconds(), status.getResetTimeSeconds());

        response.getWriter().write(jsonResponse);
    }

    private void sendRateLimitResponse(HttpServletResponse response, String message)
            throws IOException {

        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType("application/json");

        String jsonResponse = String.format("""
            {
                "error": "RATE_LIMIT_EXCEEDED",
                "message": "%s"
            }
            """, message);

        response.getWriter().write(jsonResponse);
    }

    // DTO for request patterns
    @lombok.Data
    @lombok.Builder
    @lombok.AllArgsConstructor
    public static class RequestPattern {
        public enum RequestType {
            AUTH_LOGIN,
            AUTH_REGISTER,
            AUTH_PASSWORD_RESET,
            AUTH_MFA,
            API_SENSITIVE,
            API_GENERAL
        }

        private RequestType type;
        private int limit;
        private int windowSeconds;
    }
}