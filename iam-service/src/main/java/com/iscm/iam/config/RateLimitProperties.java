package com.iscm.iam.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Data
@Component
@ConfigurationProperties(prefix = "app.rate-limit")
public class RateLimitProperties {
    
    private Auth auth = new Auth();
    private Api api = new Api();
    private Admin admin = new Admin();
    
    @Data
    public static class Auth {
        private int requests = 10;
        private Duration duration = Duration.ofMinutes(1);
        private int burstLimit = 5;
        private Duration burstRecovery = Duration.ofMinutes(5);
    }
    
    @Data
    public static class Api {
        private int requests = 100;
        private Duration duration = Duration.ofMinutes(1);
        private int burstLimit = 20;
        private Duration burstRecovery = Duration.ofMinutes(10);
    }
    
    @Data
    public static class Admin {
        private int requests = 50;
        private Duration duration = Duration.ofMinutes(1);
        private int burstLimit = 10;
        private Duration burstRecovery = Duration.ofMinutes(5);
    }
}