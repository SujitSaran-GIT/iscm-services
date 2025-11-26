package com.iscm.iam.security;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

@Component
public class UnlimitedLengthPasswordEncoder implements PasswordEncoder {

    private final org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder bcryptEncoder;

    public UnlimitedLengthPasswordEncoder() {
        this.bcryptEncoder = new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder(12);
    }

    @Override
    public String encode(CharSequence rawPassword) {
        if (rawPassword == null) {
            throw new IllegalArgumentException("Password cannot be null");
        }

        String password = rawPassword.toString();

        // Keep preprocessing for encoding very long passwords only
        if (password.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72) {
            return encodeLongPassword(password);
        }

        return bcryptEncoder.encode(password);
    }

    @Override
    public boolean matches(CharSequence rawPassword, String encodedPassword) {
        if (rawPassword == null || encodedPassword == null) {
            return false;
        }

        String password = rawPassword.toString();

        // OPTIMIZED: Always use bcrypt directly for validation
        // This eliminates the expensive SHA-256 preprocessing during login
        return bcryptEncoder.matches(password, encodedPassword);
    }

    private String encodeLongPassword(String password) {
        String preprocessedPassword = preprocessPassword(password);
        return bcryptEncoder.encode(preprocessedPassword);
    }

    private String preprocessPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(password.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }
}