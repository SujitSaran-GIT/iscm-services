package com.iscm.iam.service.impl;

import com.iscm.iam.model.User;
import com.iscm.iam.service.EmailService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class EmailServiceImpl implements EmailService {

    @Value("${app.email.enabled:false}")
    private boolean emailEnabled;

    @Override
    public void sendEmail(String to, String subject, String body) {
        if (!emailEnabled) {
            log.info("Email service disabled. Would send to: {} with subject: {}", to, subject);
            return;
        }

        try {
            // Implement actual email sending logic here
            // For now, just log the email
            log.info("Sending email to: {} with subject: {}", to, subject);
            log.debug("Email body: {}", body);

            // Integration with email service provider would go here
            // e.g., Amazon SES, SendGrid, etc.

        } catch (Exception e) {
            log.error("Failed to send email to: {}", to, e);
            throw new RuntimeException("Failed to send email", e);
        }
    }

    @Override
    @Async("emailExecutor")
    public void sendWelcomeEmail(User user) {
        if (!emailEnabled) {
            log.info("Email service disabled. Would send welcome email to: {}", user.getEmail());
            return;
        }

        try {
            String subject = "Welcome to ISCM Identity & Access Management";
            String body = String.format(
                "Dear %s %s,\n\n" +
                "Welcome to the ISCM Identity & Access Management system!\n\n" +
                "Your account has been successfully created.\n\n" +
                "Best regards,\n" +
                "The ISCM Team",
                user.getFirstName(), user.getLastName()
            );

            sendEmail(user.getEmail(), subject, body);
            log.info("Welcome email sent to user: {} ({})", user.getEmail(), user.getId());

        } catch (Exception e) {
            log.error("Failed to send welcome email to user: {}", user.getEmail(), e);
        }
    }

    @Override
    @Async("emailExecutor")
    public void sendLoginNotification(User user, String ipAddress, String userAgent) {
        if (!emailEnabled) {
            log.info("Email service disabled. Would send login notification to: {}", user.getEmail());
            return;
        }

        try {
            String subject = "New Login to Your ISCM Account";
            String body = String.format(
                "Hello %s %s,\n\n" +
                "A new login to your ISCM account was detected:\n\n" +
                "IP Address: %s\n" +
                "Device/Browser: %s\n" +
                "Time: %s\n\n" +
                "If this was you, you can safely ignore this email.\n" +
                "If you did not perform this login, please secure your account immediately.\n\n" +
                "Best regards,\n" +
                "The ISCM Security Team",
                user.getFirstName(), user.getLastName(), ipAddress, userAgent, java.time.LocalDateTime.now()
            );

            sendEmail(user.getEmail(), subject, body);
            log.info("Login notification sent to user: {} ({}) from IP: {}", user.getEmail(), user.getId(), ipAddress);

        } catch (Exception e) {
            log.error("Failed to send login notification to user: {}", user.getEmail(), e);
        }
    }

    @Override
    @Async("emailExecutor")
    public void sendSecurityAlert(User user, String alertType, String details) {
        if (!emailEnabled) {
            log.info("Email service disabled. Would send security alert to: {}", user.getEmail());
            return;
        }

        try {
            String subject = "Security Alert for Your ISCM Account";
            String body = String.format(
                "Dear %s %s,\n\n" +
                "A security event has been detected on your ISCM account:\n\n" +
                "Alert Type: %s\n" +
                "Details: %s\n" +
                "Time: %s\n\n" +
                "If this was you, you can safely ignore this alert.\n" +
                "If you did not perform this action, please secure your account immediately.\n\n" +
                "Best regards,\n" +
                "The ISCM Security Team",
                user.getFirstName(), user.getLastName(), alertType, details, java.time.LocalDateTime.now()
            );

            sendEmail(user.getEmail(), subject, body);
            log.info("Security alert sent to user: {} ({}) for alert type: {}", user.getEmail(), user.getId(), alertType);

        } catch (Exception e) {
            log.error("Failed to send security alert to user: {}", user.getEmail(), e);
        }
    }
}