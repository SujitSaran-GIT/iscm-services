package com.iscm.iam.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

@Slf4j
@Component
public class SecurityValidator {

    // Email validation pattern
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
    );

    // Phone number validation pattern (E.164 format)
    private static final Pattern PHONE_PATTERN = Pattern.compile(
        "^\\+[1-9]\\d{1,14}$"
    );

    // Password complexity pattern
    private static final Pattern PASSWORD_PATTERN = Pattern.compile(
        "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$"
    );

    // Username pattern (alphanumeric with underscores and hyphens)
    private static final Pattern USERNAME_PATTERN = Pattern.compile(
        "^[a-zA-Z0-9_-]{3,50}$"
    );

    // UUID pattern
    private static final Pattern UUID_PATTERN = Pattern.compile(
        "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$"
    );

    /**
     * Validates email address format
     */
    public void validateEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            throw new SecurityValidationException("Email cannot be null or empty");
        }

        String cleanedEmail = email.trim().toLowerCase();

        if (cleanedEmail.length() > 254) {
            throw new SecurityValidationException("Email address is too long (max 254 characters)");
        }

        if (!EMAIL_PATTERN.matcher(cleanedEmail).matches()) {
            throw new SecurityValidationException("Invalid email format");
        }

        // Check for common disposable email domains
        if (isDisposableEmail(cleanedEmail)) {
            log.warn("Disposable email domain detected: {}", cleanedEmail);
            throw new SecurityValidationException("Disposable email addresses are not allowed");
        }
    }

    /**
     * Validates password strength and format
     */
    public void validatePassword(String password) {
        if (password == null || password.trim().isEmpty()) {
            throw new SecurityValidationException("Password cannot be null or empty");
        }

        String cleanedPassword = password;

        if (cleanedPassword.length() < 8) {
            throw new SecurityValidationException("Password must be at least 8 characters long");
        }

        if (cleanedPassword.length() > 128) {
            throw new SecurityValidationException("Password is too long (max 128 characters)");
        }

        if (!PASSWORD_PATTERN.matcher(cleanedPassword).matches()) {
            throw new SecurityValidationException(
                "Password must contain at least one lowercase letter, one uppercase letter, one digit, and one special character (@$!%*?&)"
            );
        }

        // Check for common weak passwords
        if (isCommonPassword(cleanedPassword)) {
            throw new SecurityValidationException("Password is too common. Please choose a stronger password.");
        }

        // Check for repeated characters
        if (hasRepeatedCharacters(cleanedPassword)) {
            throw new SecurityValidationException("Password cannot contain repeated characters");
        }
    }

    /**
     * Validates phone number format (E.164)
     */
    public void validatePhoneNumber(String phone) {
        if (phone == null || phone.trim().isEmpty()) {
            return; // Phone is optional
        }

        String cleanedPhone = phone.trim();

        if (!PHONE_PATTERN.matcher(cleanedPhone).matches()) {
            throw new SecurityValidationException("Invalid phone number format. Please use E.164 format (e.g., +1234567890)");
        }

        if (cleanedPhone.length() > 16) {
            throw new SecurityValidationException("Phone number is too long (max 16 characters)");
        }
    }

    /**
     * Validates username format
     */
    public void validateUsername(String username) {
        if (username == null || username.trim().isEmpty()) {
            throw new SecurityValidationException("Username cannot be null or empty");
        }

        String cleanedUsername = username.trim();

        if (!USERNAME_PATTERN.matcher(cleanedUsername).matches()) {
            throw new SecurityValidationException(
                "Username must be 3-50 characters long and contain only letters, numbers, underscores, and hyphens"
            );
        }

        // Check for reserved usernames
        if (isReservedUsername(cleanedUsername)) {
            throw new SecurityValidationException("Username is reserved and cannot be used");
        }
    }

    /**
     * Validates UUID format
     */
    public void validateUUID(String uuid) {
        if (uuid == null || uuid.trim().isEmpty()) {
            throw new SecurityValidationException("UUID cannot be null or empty");
        }

        String cleanedUUID = uuid.trim();

        if (!UUID_PATTERN.matcher(cleanedUUID).matches()) {
            throw new SecurityValidationException("Invalid UUID format");
        }
    }

    /**
     * Sanitizes input to prevent XSS attacks
     */
    public String sanitizeInput(String input) {
        if (input == null) {
            return null;
        }

        return input.trim()
                .replaceAll("<script[^>]*>.*?</script>", "") // Remove script tags
                .replaceAll("<", "&lt;")
                .replaceAll(">", "&gt;")
                .replaceAll("\"", "&quot;")
                .replaceAll("'", "&#x27;")
                .replaceAll("javascript:", "")
                .replaceAll("on\\w+\\s*=", ""); // Remove event handlers
    }

    /**
     * Validates MFA code format
     */
    public void validateMfaCode(String code) {
        if (code == null || code.trim().isEmpty()) {
            throw new SecurityValidationException("MFA code cannot be null or empty");
        }

        String cleanedCode = code.trim().replaceAll("\\s", "");

        if (!cleanedCode.matches("\\d{6}")) {
            throw new SecurityValidationException("MFA code must be exactly 6 digits");
        }
    }

    /**
     * Validates OAuth provider
     */
    public void validateOAuthProvider(String provider) {
        if (provider == null || provider.trim().isEmpty()) {
            throw new SecurityValidationException("OAuth provider cannot be null or empty");
        }

        String cleanedProvider = provider.trim().toLowerCase();

        if (!isValidOAuthProvider(cleanedProvider)) {
            throw new SecurityValidationException("Invalid OAuth provider: " + cleanedProvider);
        }
    }

    /**
     * Validates IP address format
     */
    public void validateIpAddress(String ipAddress) {
        if (ipAddress == null || ipAddress.trim().isEmpty()) {
            return; // IP is optional
        }

        String cleanedIp = ipAddress.trim();

        // IPv4 validation
        if (!cleanedIp.matches("^(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$") &&
            !cleanedIp.matches("^(?:[0-9a-fA-F]{1,4}:){7}[0-9a-fA-F]{1,4}$")) {
            throw new SecurityValidationException("Invalid IP address format");
        }

        // Check for private/internal IP ranges
        if (isPrivateIp(cleanedIp)) {
            log.warn("Private IP address detected in request: {}", cleanedIp);
        }
    }

    /**
     * Validates device fingerprint
     */
    public void validateDeviceFingerprint(String fingerprint) {
        if (fingerprint == null || fingerprint.trim().isEmpty()) {
            throw new SecurityValidationException("Device fingerprint cannot be null or empty");
        }

        String cleanedFingerprint = fingerprint.trim();

        if (cleanedFingerprint.length() < 32 || cleanedFingerprint.length() > 256) {
            throw new SecurityValidationException("Device fingerprint must be between 32 and 256 characters");
        }

        // Only allow alphanumeric characters and common symbols
        if (!cleanedFingerprint.matches("^[a-zA-Z0-9_-]+$")) {
            throw new SecurityValidationException("Invalid device fingerprint format");
        }
    }

    // Helper methods

    private boolean isDisposableEmail(String email) {
        String domain = email.substring(email.lastIndexOf('@') + 1).toLowerCase();
        return domain.startsWith("temp") ||
               domain.startsWith("10minutemail") ||
               domain.startsWith("guerrillamail") ||
               domain.startsWith("mailinator") ||
               domain.startsWith("yopmail");
    }

    private boolean isCommonPassword(String password) {
        String lowerPassword = password.toLowerCase();
        return lowerPassword.equals("password") ||
               lowerPassword.equals("12345678") ||
               lowerPassword.equals("qwerty") ||
               lowerPassword.equals("admin") ||
               lowerPassword.equals("letmein") ||
               lowerPassword.equals("welcome") ||
               lowerPassword.equals("monkey") ||
               lowerPassword.contains("password") ||
               lowerPassword.contains("123456");
    }

    private boolean hasRepeatedCharacters(String password) {
        for (int i = 0; i < password.length() - 2; i++) {
            if (password.charAt(i) == password.charAt(i + 1) &&
                password.charAt(i + 1) == password.charAt(i + 2)) {
                return true;
            }
        }
        return false;
    }

    private boolean isReservedUsername(String username) {
        String lowerUsername = username.toLowerCase();
        return lowerUsername.equals("admin") ||
               lowerUsername.equals("administrator") ||
               lowerUsername.equals("root") ||
               lowerUsername.equals("system") ||
               lowerUsername.equals("api") ||
               lowerUsername.equals("www") ||
               lowerUsername.equals("mail") ||
               lowerUsername.equals("support") ||
               lowerUsername.equals("info") ||
               lowerUsername.equals("help") ||
               lowerUsername.equals("test") ||
               lowerUsername.equals("demo");
    }

    private boolean isValidOAuthProvider(String provider) {
        return provider.equals("google") ||
               provider.equals("microsoft") ||
               provider.equals("linkedin") ||
               provider.equals("github") ||
               provider.equals("facebook");
    }

    private boolean isPrivateIp(String ip) {
        return ip.startsWith("10.") ||
               ip.startsWith("192.168.") ||
               ip.startsWith("172.") ||
               ip.equals("127.0.0.1") ||
               ip.equals("localhost");
    }

    public static class SecurityValidationException extends RuntimeException {
        public SecurityValidationException(String message) {
            super(message);
        }
    }
}