package com.iscm.iam.config;

// import com.iscm.iam.interceptor.RateLimitingInterceptor;
import com.iscm.iam.interceptor.SecurityHeadersInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final SecurityHeadersInterceptor securityHeadersInterceptor;

    public WebMvcConfig(SecurityHeadersInterceptor securityHeadersInterceptor) {
        this.securityHeadersInterceptor = securityHeadersInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(securityHeadersInterceptor)
                .addPathPatterns("/**");

        // Rate limiting interceptor disabled
        // registry.addInterceptor(new RateLimitingInterceptor(null, null))
        //         .addPathPatterns("/api/**")
        //         .excludePathPatterns("/actuator/health"); // Exclude health checks from rate limiting
    }
}
