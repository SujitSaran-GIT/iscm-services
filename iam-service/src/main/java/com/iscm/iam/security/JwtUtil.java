package com.iscm.iam.security;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import jakarta.annotation.PostConstruct;
import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.Keys;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class JwtUtil {

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    @Value("${app.jwt.refresh-secret}")
    private String jwtRefreshSecret;

    @Value("${app.jwt.expiration.access:900}") // 15 minutes
    private Long accessTokenExpiration;

    @Value("${app.jwt.expiration.refresh:604800}") // 7 days
    private Long refreshTokenExpiration;

    // Cached signing keys for performance optimization
    private SecretKey cachedSigningKey;
    private SecretKey cachedRefreshSigningKey;

    @PostConstruct
    private void init() {
        // Cache signing keys to avoid repeated key generation
        this.cachedSigningKey = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        this.cachedRefreshSigningKey = Keys.hmacShaKeyFor(jwtRefreshSecret.getBytes(StandardCharsets.UTF_8));
    }

    public Long getAccessTokenExpiration() {
        return accessTokenExpiration;
    }

    public Long getRefreshTokenExpiration() {
        return refreshTokenExpiration;
    }

    private SecretKey getSigningKey() {
        return cachedSigningKey;
    }

    private SecretKey getRefreshSigningKey() {
        return cachedRefreshSigningKey;
    }

    @Data
    public static class JwtClaims {
        private final String userId;
        private final String email;
        private final List<String> roles;
        private final String tenantId;
        private final String jti;
        private final Claims claims;

        public JwtClaims(Claims claims) {
            this.claims = claims;
            this.userId = claims.getSubject();
            this.email = claims.get("email", String.class);
            this.roles = claims.get("roles", List.class);
            this.tenantId = claims.get("tenantId", String.class);
            this.jti = claims.getId();
        }

        public boolean isValid() {
            return userId != null && email != null && roles != null;
        }
    }

    public String generateAccessToken(UUID userId, String email, List<String> roles, UUID tenantId) {
        String jti = UUID.randomUUID().toString();

        return Jwts.builder()
        .setId(jti) // Add JWT ID (jti) claim
        .setSubject(userId.toString())
        .claim("email", email)
        .claim("roles", roles)
        .claim("tenantId", tenantId != null ? tenantId.toString() : null)
        .setIssuedAt(Date.from(Instant.now()))
        .setExpiration(Date.from(Instant.now().plusSeconds(accessTokenExpiration)))
        .signWith(getSigningKey())
        .compact();
    }

    public String generateRefreshToken(UUID userId) {
        String jti = UUID.randomUUID().toString();

        return Jwts.builder()
        .setId(jti) // Add JWT ID (jti) claim
        .setSubject(userId.toString())
        .setExpiration(Date.from(Instant.now().plusSeconds(refreshTokenExpiration)))
        .signWith(getRefreshSigningKey())
        .compact();
    }

    // OPTIMIZED: Single-pass JWT parsing method
    public JwtClaims extractAllClaims(String token) {
        try {
            Claims claims = Jwts.parser()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            return new JwtClaims(claims);
        } catch (ExpiredJwtException ex) {
            log.error("JWT token expired: {}", ex.getMessage());
            return null;
        } catch (MalformedJwtException ex) {
            log.error("Invalid JWT token: {}", ex.getMessage());
            return null;
        } catch (Exception ex) {
            log.error("JWT parsing error: {}", ex.getMessage());
            return null;
        }
    }

    public boolean validateToken(String token) {
        return extractAllClaims(token) != null;
    }

    public String getUserIdFromToken(String token) {
        JwtClaims claims = extractAllClaims(token);
        return claims != null ? claims.getUserId() : null;
    }

    public List<String> getRolesFromToken(String token) {
        JwtClaims claims = extractAllClaims(token);
        return claims != null ? claims.getRoles() : null;
    }

    public String getEmailFromToken(String token) {
        JwtClaims claims = extractAllClaims(token);
        return claims != null ? claims.getEmail() : null;
    }

    public boolean validateRefreshToken(String token) {
        try {
            Jwts.parser().setSigningKey(getRefreshSigningKey()).build().parseClaimsJws(token);
            return true;
        } catch (ExpiredJwtException ex) {
            log.error("JWT refresh token expired: {}", ex.getMessage());
        } catch (MalformedJwtException ex) {
            log.error("Invalid JWT refresh token: {}", ex.getMessage());
        } catch (Exception ex) {
            log.error("JWT refresh token validation error: {}", ex.getMessage());
        }
        return false;
    }

    public String getUserIdFromRefreshToken(String token) {
        Claims claims = Jwts.parser()
                .setSigningKey(getRefreshSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
        return claims.getSubject();
    }

    public String getJtiFromToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            return claims.getId();
        } catch (Exception e) {
            log.error("Failed to extract JTI from token", e);
            return null;
        }
    }

    public String getJtiFromRefreshToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .setSigningKey(getRefreshSigningKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            return claims.getId();
        } catch (Exception e) {
            log.error("Failed to extract JTI from refresh token", e);
            return null;
        }
    }

    public Instant getExpirationFromToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            return claims.getExpiration().toInstant();
        } catch (Exception e) {
            log.error("Failed to extract expiration from token", e);
            return Instant.now().plus(Duration.ofHours(1)); // Default expiration
        }
    }
}
