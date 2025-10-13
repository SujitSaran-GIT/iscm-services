package com.iscm.iam.config;

import com.iscm.iam.interceptor.RateLimitingInterceptor;
import com.iscm.iam.interceptor.SecurityHeadersInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final RateLimitingInterceptor rateLimitingInterceptor;
    private final SecurityHeadersInterceptor securityHeadersInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(securityHeadersInterceptor)
                .addPathPatterns("/**");

        registry.addInterceptor(rateLimitingInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns("/actuator/health"); // Exclude health checks from rate limiting
    }
}
