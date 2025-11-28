package com.iscm.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenAPIConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("ISCM Vendor Service API")
                .version("1.0.0")
                .description("Vendor and Organization Management Service for Intelligent Supply Chain Management Platform")
                .contact(new Contact()
                    .name("ISCM Team")
                    .email("support@iscm.com")));
            // Auth temporarily disabled
            // .components(new Components()
            //     .addSecuritySchemes("bearer-jwt", new SecurityScheme()
            //         .type(SecurityScheme.Type.HTTP)
            //         .scheme("bearer")
            //         .bearerFormat("JWT")
            //         .description("JWT token authentication")));
    }
}
