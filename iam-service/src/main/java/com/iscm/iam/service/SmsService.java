package com.iscm.iam.service;

import org.springframework.stereotype.Service;

@Service
public interface SmsService {
    void sendSms(String phoneNumber, String message);
}