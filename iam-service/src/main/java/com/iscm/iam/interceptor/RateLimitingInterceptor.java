package com.iscm.iam.interceptor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * DISABLED STUB - Rate limiting interceptor has been disabled for simplification.
 * This interceptor does not perform any rate limiting.
 * To re-enable rate limiting, restore the original implementation.
 */
@Slf4j
//@Component
public class RateLimitingInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // Rate limiting disabled - allow all requests
        log.debug("Rate limiting interceptor disabled - allowing request to: {}", request.getRequestURI());
        return true;
    }
}