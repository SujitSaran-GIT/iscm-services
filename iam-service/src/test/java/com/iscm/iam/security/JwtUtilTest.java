package com.iscm.iam.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class JwtUtilTest {

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "jwtSecret", 
            "test-secret-key-that-is-long-enough-for-hs512-123!");
        ReflectionTestUtils.setField(jwtUtil, "accessTokenExpiration", 300L);
        ReflectionTestUtils.setField(jwtUtil, "refreshTokenExpiration", 1800L);
    }

    @Test
    void testGenerateAndValidateToken() {
        // Given
        UUID userId = UUID.randomUUID();
        String email = "test@example.com";
        List<String> roles = List.of("USER", "ADMIN");
        UUID tenantId = UUID.randomUUID();

        // When
        String token = jwtUtil.generateAccessToken(userId, email, roles, tenantId);
        boolean isValid = jwtUtil.validateToken(token);

        // Then
        assertTrue(isValid);
        assertEquals(userId.toString(), jwtUtil.getUserIdFromToken(token));
        assertEquals(email, jwtUtil.getEmailFromToken(token));
        assertEquals(roles, jwtUtil.getRolesFromToken(token));
    }

    @Test
    void testInvalidToken() {
        // Given
        String invalidToken = "invalid.jwt.token";

        // When
        boolean isValid = jwtUtil.validateToken(invalidToken);

        // Then
        assertFalse(isValid);
    }

    @Test
    void testGenerateRefreshToken() {
        // Given
        UUID userId = UUID.randomUUID();

        // When
        String refreshToken = jwtUtil.generateRefreshToken(userId);
        boolean isValid = jwtUtil.validateToken(refreshToken);

        // Then
        assertTrue(isValid);
        assertEquals(userId.toString(), jwtUtil.getUserIdFromToken(refreshToken));
    }
}