package com.iscm.iam.service;

import com.iscm.iam.model.User;
import com.iscm.iam.repository.UserRepository;
import com.warrenstrange.googleauth.GoogleAuthenticator;
import com.warrenstrange.googleauth.GoogleAuthenticatorKey;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Slf4j
@Service
@RequiredArgsConstructor
public class MfaService {

    private final UserRepository userRepository;
    private final EmailService emailService;
    private final SmsService smsService;
    private final GoogleAuthenticator googleAuthenticator;

    @Value("${app.mfa.totp.window-size:3}")
    private int totpWindowSize;

    private static final String TOTP_ISSUER = "ISCM Platform";
    private static final int BACKUP_CODES_COUNT = 10;
    private static final int BACKUP_CODE_LENGTH = 8;

    @Transactional
    public String generateTotpSecret(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // Generate proper TOTP secret using Google Authenticator
        GoogleAuthenticatorKey key = googleAuthenticator.createCredentials();
        String secret = key.getKey();

        user.setMfaSecret(secret);
        user.setMfaType("TOTP");
        userRepository.save(user);

        log.info("TOTP secret generated for user: {}", userId);
        return secret;
    }

    public String getTotpQrUrl(UUID userId, String email) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (user.getMfaSecret() == null) {
            throw new IllegalArgumentException("MFA not set up for user");
        }

        return String.format("otpauth://totp/%s:%s?secret=%s&issuer=%s",
                TOTP_ISSUER, email, user.getMfaSecret(), TOTP_ISSUER);
    }

    @Transactional
    public boolean verifyTotp(UUID userId, String code) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (!"TOTP".equals(user.getMfaType()) || user.getMfaSecret() == null) {
            log.warn("MFA not set up for user: {}", userId);
            return false;
        }

        try {
            // Input validation
            if (code == null || code.trim().isEmpty()) {
                log.warn("Empty TOTP code provided for user: {}", userId);
                return false;
            }

            // Remove any spaces and validate format
            String cleanCode = code.trim().replaceAll("\\s", "");
            if (!cleanCode.matches("\\d{6}")) {
                log.warn("Invalid TOTP code format for user: {}", userId);
                return false;
            }

            // Convert to integer
            int codeValue = Integer.parseInt(cleanCode);

            // Verify TOTP using Google Authenticator library with configured window size
            boolean isValid = googleAuthenticator.authorize(user.getMfaSecret(), codeValue, totpWindowSize);

            if (isValid) {
                log.info("TOTP verification successful for user: {}", userId);
            } else {
                log.warn("Invalid TOTP code for user: {}", userId);
            }

            return isValid;

        } catch (NumberFormatException e) {
            log.warn("Invalid TOTP code format for user: {}", userId);
            return false;
        } catch (Exception e) {
            log.error("Error verifying TOTP code for user: {}", userId, e);
            return false;
        }
    }

    @Transactional
    public void enableMfa(UUID userId, String verificationCode, String mfaType) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        boolean isValid = false;

        switch (mfaType.toUpperCase()) {
            case "TOTP":
                isValid = verifyTotp(userId, verificationCode);
                break;
            case "SMS":
                isValid = verifySmsCode(userId, verificationCode);
                break;
            case "EMAIL":
                isValid = verifyEmailCode(userId, verificationCode);
                break;
        }

        if (!isValid) {
            throw new IllegalArgumentException("Invalid verification code");
        }

        user.setMfaEnabled(true);
        user.setMfaType(mfaType.toUpperCase());

        // Generate backup codes
        List<String> backupCodes = generateBackupCodes();
        user.setMfaBackupCodes(String.join(",", backupCodes));

        userRepository.save(user);
        log.info("MFA enabled for user: {}", userId);
    }

    @Transactional
    public void disableMfa(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        user.setMfaEnabled(false);
        user.setMfaSecret(null);
        user.setMfaType(null);
        user.setMfaPhone(null);
        user.setMfaBackupCodes(null);

        userRepository.save(user);
        log.info("MFA disabled for user: {}", userId);
    }

    @Transactional
    public void sendSmsCode(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (user.getMfaPhone() == null) {
            throw new IllegalArgumentException("Phone number not set up for MFA");
        }

        String code = generateNumericCode(6);
        // Store code with expiration (you might want to create a separate table for this)
        smsService.sendSms(user.getMfaPhone(),
                String.format("Your ISCM verification code is: %s", code));

        log.info("SMS code sent to user: {}", userId);
    }

    @Transactional
    public void sendEmailCode(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        String code = generateNumericCode(6);
        // Store code with expiration
        emailService.sendEmail(user.getEmail(),
                "Your ISCM Verification Code",
                String.format("Your verification code is: %s\nThis code expires in 10 minutes.", code));

        log.info("Email code sent to user: {}", userId);
    }

    public boolean validateBackupCode(UUID userId, String code) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (user.getMfaBackupCodes() == null) {
            return false;
        }

        List<String> backupCodes = List.of(user.getMfaBackupCodes().split(","));
        if (backupCodes.contains(code)) {
            // Remove used backup code
            List<String> remainingCodes = backupCodes.stream()
                    .filter(c -> !c.equals(code))
                    .collect(Collectors.toList());
            user.setMfaBackupCodes(String.join(",", remainingCodes));
            userRepository.save(user);
            return true;
        }

        return false;
    }

    private boolean verifySmsCode(UUID userId, String code) {
        // Implement SMS code verification logic
        // This would check against stored codes with expiration
        return true; // Placeholder
    }

    private boolean verifyEmailCode(UUID userId, String code) {
        // Implement email code verification logic
        // This would check against stored codes with expiration
        return true; // Placeholder
    }

    private List<String> generateBackupCodes() {
        SecureRandom random = new SecureRandom();
        return IntStream.range(0, BACKUP_CODES_COUNT)
                .mapToObj(i -> {
                    StringBuilder code = new StringBuilder();
                    for (int j = 0; j < BACKUP_CODE_LENGTH; j++) {
                        code.append(random.nextInt(10));
                    }
                    return code.toString();
                })
                .collect(Collectors.toList());
    }

    private String generateNumericCode(int length) {
        SecureRandom random = new SecureRandom();
        StringBuilder code = new StringBuilder();
        for (int i = 0; i < length; i++) {
            code.append(random.nextInt(10));
        }
        return code.toString();
    }
}