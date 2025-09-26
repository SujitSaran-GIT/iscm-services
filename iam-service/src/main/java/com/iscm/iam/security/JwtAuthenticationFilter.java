package com.iscm.iam.security;

import com.iscm.iam.service.UserService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.regex.Pattern;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserService userService;
    
    private static final Pattern JWT_PATTERN = Pattern.compile("^[A-Za-z0-9-_=]+\\.[A-Za-z0-9-_=]+\\.?[A-Za-z0-9-_.+/=]*$");

    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                  HttpServletResponse response, 
                                  FilterChain filterChain) 
            throws ServletException, IOException {
        
        try {
            String jwt = getJwtFromRequest(request);
            
            if (StringUtils.isNotEmpty(jwt)) {
                // Validate JWT format before processing
                if (!isValidJwtFormat(jwt)) {
                    log.warn("Invalid JWT format from IP: {}", getClientIpAddress(request));
                    filterChain.doFilter(request, response);
                    return;
                }
                
                if (jwtUtil.validateToken(jwt)) {
                    String userId = jwtUtil.getUserIdFromToken(jwt);
                    
                    UserDetails userDetails = userService.loadUserByUserId(userId);
                    
                    // Set user ID in request for rate limiting
                    request.setAttribute("userId", userId);
                    
                    UsernamePasswordAuthenticationToken authentication = 
                        new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    
                    log.debug("Authenticated user: {} from IP: {}", 
                            userDetails.getUsername(), getClientIpAddress(request));
                } else {
                    log.warn("Invalid JWT token from IP: {}", getClientIpAddress(request));
                }
            }
        } catch (Exception ex) {
            log.error("Security error in JWT filter: {}", ex.getMessage());
            // Don't throw exception to avoid breaking the filter chain
        }
        
        filterChain.doFilter(request, response);
    }

    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.isNotEmpty(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

    private boolean isValidJwtFormat(String token) {
        return JWT_PATTERN.matcher(token).matches() && token.length() > 50; // Basic format validation
    }

    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}