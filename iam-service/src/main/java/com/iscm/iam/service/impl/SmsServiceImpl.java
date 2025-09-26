package com.iscm.iam.service.impl;

import com.iscm.iam.service.SmsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class SmsServiceImpl implements SmsService {

    @Value("${app.sms.enabled:false}")
    private boolean smsEnabled;

    @Override
    public void sendSms(String phoneNumber, String message) {
        if (!smsEnabled) {
            log.info("SMS service disabled. Would send to: {} with message: {}", phoneNumber, message);
            return;
        }

        try {
            // Implement actual SMS sending logic here
            // For now, just log the SMS
            log.info("Sending SMS to: {} with message: {}", phoneNumber, message);

            // Integration with SMS service provider would go here
            // e.g., Twilio, AWS SNS, etc.

        } catch (Exception e) {
            log.error("Failed to send SMS to: {}", phoneNumber, e);
            throw new RuntimeException("Failed to send SMS", e);
        }
    }
}