package com.iscm.iam.interceptor;

import com.iscm.iam.config.RateLimitProperties;
import com.iscm.iam.service.RateLimitingService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.Duration;

@Slf4j
@Component
@RequiredArgsConstructor
public class RateLimitingInterceptor implements HandlerInterceptor {

    private final RateLimitingService rateLimitingService;
    private final RateLimitProperties rateLimitProperties;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String clientKey = getClientKey(request);
        String path = request.getRequestURI();
        
        RateLimitProperties.Auth authConfig = rateLimitProperties.getAuth();
        RateLimitProperties.Api apiConfig = rateLimitProperties.getApi();
        RateLimitProperties.Admin adminConfig = rateLimitProperties.getAdmin();

        // Determine rate limit configuration based on endpoint
        if (path.contains("/auth/")) {
            return handleRateLimit(response, clientKey, path, authConfig, "Authentication");
        } else if (path.contains("/admin/") || path.contains("/actuator")) {
            return handleRateLimit(response, clientKey, path, adminConfig, "Administration");
        } else {
            return handleRateLimit(response, clientKey, path, apiConfig, "API");
        }
    }

    private boolean handleRateLimit(HttpServletResponse response, String clientKey, String path,
                                   RateLimitProperties.RateLimitConfig config, String endpointType) throws Exception {
        
        // Check burst limit first
        if (rateLimitingService.isBurstLimited(clientKey, config.getBurstLimit(), config.getBurstRecovery())) {
            setRateLimitHeaders(response, 0, config.getDuration());
            response.sendError(HttpStatus.TOO_MANY_REQUESTS.value(), 
                    "Burst limit exceeded. Please try again later.");
            log.warn("Burst limit exceeded for {} endpoint: {}", endpointType, path);
            return false;
        }

        // Check rate limit
        if (rateLimitingService.isRateLimited(clientKey, config.getRequests(), config.getDuration())) {
            Duration timeUntilReset = rateLimitingService.getTimeUntilReset(clientKey, config.getDuration());
            setRateLimitHeaders(response, 0, timeUntilReset);
            response.sendError(HttpStatus.TOO_MANY_REQUESTS.value(), 
                    "Rate limit exceeded. Please try again later.");
            
            // Record burst if this is the first rate limit hit
            rateLimitingService.recordBurst(clientKey, config.getBurstRecovery());
            log.warn("Rate limit exceeded for {} endpoint: {}", endpointType, path);
            return false;
        }

        // Set rate limit headers for successful requests
        long remaining = rateLimitingService.getRemainingRequests(clientKey, config.getRequests(), config.getDuration());
        Duration timeUntilReset = rateLimitingService.getTimeUntilReset(clientKey, config.getDuration());
        setRateLimitHeaders(response, remaining, timeUntilReset);
        
        return true;
    }

    private String getClientKey(HttpServletRequest request) {
        // Use client IP address as key
        String clientIp = getClientIpAddress(request);
        
        // For authenticated users, combine IP with user ID for more granular limits
        String userId = (String) request.getAttribute("userId");
        if (userId != null) {
            return clientIp + ":" + userId;
        }
        
        return clientIp;
    }

    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }

    private void setRateLimitHeaders(HttpServletResponse response, long remaining, Duration resetAfter) {
        response.setHeader("X-Rate-Limit-Limit", String.valueOf(rateLimitProperties.getApi().getRequests()));
        response.setHeader("X-Rate-Limit-Remaining", String.valueOf(remaining));
        response.setHeader("X-Rate-Limit-Reset", String.valueOf(resetAfter.getSeconds()));
    }
}