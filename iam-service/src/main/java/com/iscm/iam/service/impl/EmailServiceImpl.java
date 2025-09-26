package com.iscm.iam.service.impl;

import com.iscm.iam.service.EmailService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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
}