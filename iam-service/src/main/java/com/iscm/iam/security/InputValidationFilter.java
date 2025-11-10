package com.iscm.iam.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE) // Execute before other filters
public class InputValidationFilter extends OncePerRequestFilter {

    @Autowired
    private SecurityValidator securityValidator;

    private static final int MAX_REQUEST_SIZE = 10 * 1024 * 1024; // 10MB
    private static final int MAX_HEADER_SIZE = 8192; // 8KB
    private static final int MAX_HEADERS = 100;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        try {
            // Validate request size
            validateRequestSize(request);

            // Validate headers
            validateHeaders(request);

            // Validate request parameters
            validateParameters(request);

            // Validate URI path
            validateUriPath(request.getRequestURI());

            // Sanitize query parameters
            sanitizeQueryParameters(request);

            filterChain.doFilter(request, response);

        } catch (SecurityValidationException e) {
            log.warn("Input validation failed: {}", e.getMessage());
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"Invalid input: " + e.getMessage() + "\"}");
        }
    }

    private void validateRequestSize(HttpServletRequest request) {
        String contentLengthHeader = request.getHeader("Content-Length");
        if (contentLengthHeader != null) {
            try {
                long contentLength = Long.parseLong(contentLengthHeader);
                if (contentLength > MAX_REQUEST_SIZE) {
                    throw new SecurityValidationException("Request too large");
                }
            } catch (NumberFormatException e) {
                throw new SecurityValidationException("Invalid content-length header");
            }
        }
    }

    private void validateHeaders(HttpServletRequest request) {
        Enumeration<String> headerNames = request.getHeaderNames();
        int headerCount = 0;

        while (headerNames.hasMoreElements() && headerCount < MAX_HEADERS) {
            String headerName = headerNames.nextElement();
            headerCount++;

            // Validate header name
            validateHeaderName(headerName);

            // Validate header values
            Enumeration<String> headerValues = request.getHeaders(headerName);
            while (headerValues.hasMoreElements()) {
                String headerValue = headerValues.nextElement();
                validateHeaderValue(headerName, headerValue);
            }
        }

        if (headerNames.hasMoreElements()) {
            throw new SecurityValidationException("Too many headers");
        }
    }

    private void validateHeaderName(String headerName) {
        // Header names should only contain printable ASCII characters
        if (!headerName.matches("^[\\x20-\\x7E]+$")) {
            throw new SecurityValidationException("Invalid header name: " + headerName);
        }

        // Check for suspicious header names
        String lowerHeaderName = headerName.toLowerCase();
        if (lowerHeaderName.contains("script") ||
            lowerHeaderName.contains("javascript") ||
            lowerHeaderName.contains("vbscript") ||
            lowerHeaderName.contains("onload") ||
            lowerHeaderName.contains("onerror")) {
            throw new SecurityValidationException("Suspicious header name: " + headerName);
        }
    }

    private void validateHeaderValue(String headerName, String headerValue) {
        if (headerValue == null) {
            return;
        }

        // Check header value length
        if (headerValue.length() > MAX_HEADER_SIZE) {
            throw new SecurityValidationException("Header value too long: " + headerName);
        }

        // Sanitize header value
        String sanitizedValue = securityValidator.sanitizeInput(headerValue);

        // Check for suspicious content in specific headers
        if (headerName.equalsIgnoreCase("User-Agent") &&
            containsSuspiciousPatterns(sanitizedValue)) {
            log.warn("Suspicious User-Agent detected: {}", sanitizedValue);
        }
    }

    private void validateParameters(HttpServletRequest request) {
        Map<String, String[]> parameterMap = request.getParameterMap();

        for (Map.Entry<String, String[]> entry : parameterMap.entrySet()) {
            String paramName = entry.getKey();
            String[] paramValues = entry.getValue();

            // Validate parameter name
            validateParameterName(paramName);

            // Validate parameter values
            for (String paramValue : paramValues) {
                validateParameterValue(paramName, paramValue);
            }
        }
    }

    private void validateParameterName(String paramName) {
        // Parameter names should only contain safe characters
        if (!paramName.matches("^[a-zA-Z0-9_\\-\\[\\]\\.]+$")) {
            throw new SecurityValidationException("Invalid parameter name: " + paramName);
        }
    }

    private void validateParameterValue(String paramName, String paramValue) {
        if (paramValue == null) {
            return;
        }

        // Check parameter value length
        if (paramValue.length() > 1000) {
            throw new SecurityValidationException("Parameter value too long: " + paramName);
        }

        // Validate specific parameter types
        if (paramName.equalsIgnoreCase("email") || paramName.toLowerCase().contains("email")) {
            securityValidator.validateEmail(paramValue);
        } else if (paramName.equalsIgnoreCase("phone") || paramName.toLowerCase().contains("phone")) {
            securityValidator.validatePhoneNumber(paramValue);
        } else if (paramName.toLowerCase().contains("password")) {
            securityValidator.validatePassword(paramValue);
        } else if (paramName.toLowerCase().contains("uuid") || paramName.toLowerCase().contains("id")) {
            securityValidator.validateUUID(paramValue);
        } else {
            // General sanitization for other parameters
            String sanitized = securityValidator.sanitizeInput(paramValue);
            if (sanitized.length() != paramValue.length()) {
                log.warn("Parameter value sanitized: {}", paramName);
            }
        }
    }

    private void validateUriPath(String uri) {
        if (uri == null || uri.trim().isEmpty()) {
            throw new SecurityValidationException("Invalid URI path");
        }

        // Check for path traversal attempts
        if (uri.contains("../") || uri.contains("..\\") || uri.contains("%2e%2e")) {
            throw new SecurityValidationException("Path traversal attempt detected");
        }

        // Check for suspicious patterns
        String lowerUri = uri.toLowerCase();
        if (lowerUri.contains("<script") ||
            lowerUri.contains("javascript:") ||
            lowerUri.contains("vbscript:") ||
            lowerUri.contains("data:") ||
            lowerUri.contains("file:")) {
            throw new SecurityValidationException("Suspicious URI pattern detected");
        }

        // Check for excessive path length
        if (uri.length() > 2048) {
            throw new SecurityValidationException("URI path too long");
        }
    }

    private void sanitizeQueryParameters(HttpServletRequest request) {
        // This would require request wrapping for full implementation
        // For now, we'll just log suspicious query parameters
        String queryString = request.getQueryString();
        if (queryString != null) {
            String sanitizedQuery = securityValidator.sanitizeInput(queryString);
            if (sanitizedQuery.length() != queryString.length()) {
                log.warn("Query string contained suspicious content and was sanitized");
            }
        }
    }

    private boolean containsSuspiciousPatterns(String value) {
        String lowerValue = value.toLowerCase();
        return lowerValue.contains("sqlmap") ||
               lowerValue.contains("nikto") ||
               lowerValue.contains("nmap") ||
               lowerValue.contains("nessus") ||
               lowerValue.contains("burp") ||
               lowerValue.contains("metasploit") ||
               lowerValue.contains("shell") ||
               lowerValue.contains("cmd") ||
               lowerValue.contains("exec") ||
               lowerValue.contains("eval");
    }

    public static class SecurityValidationException extends RuntimeException {
        public SecurityValidationException(String message) {
            super(message);
        }
    }
}