package com.iscm.iam.service;

import com.iscm.iam.model.PasswordResetToken;
import com.iscm.iam.model.User;
import com.iscm.iam.repository.PasswordResetTokenRepository;
import com.iscm.iam.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final EmailService emailService;
    private final FraudDetectionService fraudDetectionService;

    @Value("${app.password.reset.token.expiration.hours:1}")
    private int tokenExpirationHours;

    @Transactional
    public void initiatePasswordReset(String email, String ipAddress, String userAgent) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("If email exists, reset instructions will be sent"));

        // Check for suspicious activity
        if (fraudDetectionService.isSuspiciousResetAttempt(user.getId(), ipAddress, userAgent)) {
            log.warn("Suspicious password reset attempt detected for user: {} from IP: {}", email, ipAddress);
            throw new SecurityException("Too many reset attempts. Please try again later.");
        }

        // Invalidate any existing tokens for this user
        List<PasswordResetToken> existingTokens = tokenRepository.findByUserAndIsUsedFalse(user);
        existingTokens.forEach(token -> {
            token.setIsUsed(true);
            tokenRepository.save(token);
        });

        // Generate new reset token
        String token = generateSecureToken();
        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setToken(token);
        resetToken.setUser(user);
        resetToken.setExpiresAt(LocalDateTime.now().plusHours(tokenExpirationHours));

        tokenRepository.save(resetToken);

        // Send reset email
        String resetLink = String.format("https://your-app.com/reset-password?token=%s", token);
        emailService.sendEmail(
            user.getEmail(),
            "Password Reset Request",
            buildResetEmail(user.getFullName(), resetLink)
        );

        log.info("Password reset initiated for user: {}", email);
    }

    @Transactional
    public void resetPassword(String token, String newPassword, String confirmPassword, String ipAddress) {
        if (!newPassword.equals(confirmPassword)) {
            throw new IllegalArgumentException("Passwords do not match");
        }

        PasswordResetToken resetToken = tokenRepository.findByToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Invalid or expired reset token"));

        if (!resetToken.isValid()) {
            throw new IllegalArgumentException("Invalid or expired reset token");
        }

        User user = resetToken.getUser();

        // Validate new password strength
        var passwordValidation = passwordService.validatePassword(newPassword);
        if (!passwordValidation.isValid()) {
            throw new IllegalArgumentException(
                "Password does not meet requirements: " +
                passwordService.getPasswordValidationMessage(passwordValidation)
            );
        }

        // Update password
        user.setPasswordHash(passwordService.encodePassword(newPassword));
        userRepository.save(user);

        // Mark token as used
        resetToken.setIsUsed(true);
        resetToken.setUsedAt(LocalDateTime.now());
        tokenRepository.save(resetToken);

        // Send confirmation email
        emailService.sendEmail(
            user.getEmail(),
            "Password Reset Successful",
            buildConfirmationEmail(user.getFullName())
        );

        log.info("Password reset completed for user: {}", user.getEmail());

        // Log successful reset for fraud detection
        fraudDetectionService.logPasswordReset(user.getId(), ipAddress, true);
    }

    public boolean validateResetToken(String token) {
        return tokenRepository.findByToken(token)
                .map(PasswordResetToken::isValid)
                .orElse(false);
    }

    private String generateSecureToken() {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return bytesToHex(bytes);
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }

    private String buildResetEmail(String userName, String resetLink) {
        return String.format("""
            Hello %s,

            We received a request to reset your password for your ISCM account.

            Click the link below to reset your password:
            %s

            This link will expire in %d hours.

            If you did not request this password reset, please ignore this email.

            Best regards,
            ISCM Security Team
            """, userName, resetLink, tokenExpirationHours);
    }

    private String buildConfirmationEmail(String userName) {
        return String.format("""
            Hello %s,

            Your password has been successfully reset.

            If you did not initiate this change, please contact our support team immediately.

            Best regards,
            ISCM Security Team
            """, userName);
    }

    // This will be injected by Spring
    private PasswordService passwordService;

    public void setPasswordService(PasswordService passwordService) {
        this.passwordService = passwordService;
    }
}