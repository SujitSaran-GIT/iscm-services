package com.iscm.iam.config;

// import org.springframework.cache.annotation.EnableCaching;
// import org.springframework.context.annotation.Bean;
// import org.springframework.context.annotation.Configuration;
// import org.springframework.data.redis.connection.RedisConnectionFactory;
// import org.springframework.data.redis.core.RedisTemplate;
// import org.springframework.data.redis.serializer.GenericToStringSerializer;
// import org.springframework.data.redis.serializer.StringRedisSerializer;

// @Configuration
// @EnableCaching
public class RateLimitingConfig {

    // @Bean
    // public RedisTemplate<String, Long> rateLimitRedisTemplate(RedisConnectionFactory connectionFactory) {
    //     RedisTemplate<String, Long> template = new RedisTemplate<>();
    //     template.setConnectionFactory(connectionFactory);
    //     template.setKeySerializer(new StringRedisSerializer());
    //     template.setValueSerializer(new GenericToStringSerializer<>(Long.class));
    //     template.setEnableTransactionSupport(true);
    //     return template;
    // }
}