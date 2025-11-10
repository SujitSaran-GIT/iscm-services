package com.iscm.iam.config;

import com.warrenstrange.googleauth.GoogleAuthenticator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class MfaConfig {

    @Value("${app.mfa.totp.window-size:3}")
    private int totpWindowSize;

    @Bean
    public GoogleAuthenticator googleAuthenticator() {
        GoogleAuthenticator gAuth = new GoogleAuthenticator();
        // The window size will be configured via application properties
        // In the actual verification, we'll use the configured window size
        log.info("Google Authenticator configured with window size: {}", totpWindowSize);
        return gAuth;
    }
}