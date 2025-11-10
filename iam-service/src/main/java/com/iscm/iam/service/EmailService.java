package com.iscm.iam.service;

import com.iscm.iam.model.User;

public interface EmailService {
    void sendEmail(String to, String subject, String body);

    // Async email methods for security and user notifications
    void sendWelcomeEmail(User user);
    void sendLoginNotification(User user, String ipAddress, String userAgent);
    void sendSecurityAlert(User user, String alertType, String details);
}