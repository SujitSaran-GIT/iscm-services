package com.iscm.iam.config;

import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.servers.Server;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.method.HandlerMethod;

import java.util.Arrays;
import java.util.List;

@Configuration
@SecurityScheme(
    name = "bearerAuth",
    type = SecuritySchemeType.HTTP,
    scheme = "bearer",
    bearerFormat = "JWT"
)
@Slf4j
public class OpenApiConfig {

    @Value("${app.version:1.0.0}")
    private String appVersion;

    @Value("${spring.profiles.active:default}")
    private String activeProfile;

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("ISCM Identity & Access Management API")
                        .version(appVersion)
                        .description("""
                            ## Intelligent Supply Chain Management - IAM Service
                            
                            Secure identity and access management microservice providing:
                            - User registration and authentication
                            - JWT-based security with refresh tokens
                            - Role-based access control (RBAC)
                            - Organization and user management
                            
                            ### Authentication
                            Most endpoints require JWT authentication. Use the `/auth/login` endpoint 
                            to obtain an access token and include it in the `Authorization` header as:
                            `Bearer <your-access-token>`
                            
                            ### Rate Limiting
                            API endpoints are rate limited for security. Default limits:
                            - Authentication endpoints: 10 requests per minute
                            - Other endpoints: 100 requests per minute
                            
                            ### Error Codes
                            - `400` - Bad Request (validation errors)
                            - `401` - Unauthorized (invalid/missing token)
                            - `403` - Forbidden (insufficient permissions)
                            - `429` - Too Many Requests (rate limit exceeded)
                            - `500` - Internal Server Error
                            """)
                        .termsOfService("https://iscm.com/terms")
                        .contact(new Contact()
                                .name("ISCM API Support")
                                .email("api-support@iscm.com")
                                .url("https://iscm.com/support"))
                        .license(new License()
                                .name("Proprietary")
                                .url("https://iscm.com/license")))
                .servers(getServers())
                .components(new Components()
                        .addSecuritySchemes("bearerAuth",
                                new io.swagger.v3.oas.models.security.SecurityScheme()
                                        .type(io.swagger.v3.oas.models.security.SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("JWT authentication with Bearer token"))
                        .addSchemas("ErrorResponse", new Schema<>()
                                .type("object")
                                .addProperty("timestamp", new StringSchema().example("2023-12-07T10:30:00Z"))
                                .addProperty("status", new StringSchema().example("400"))
                                .addProperty("error", new StringSchema().example("Bad Request"))
                                .addProperty("message", new StringSchema().example("Validation failed"))
                                .addProperty("path", new StringSchema().example("/api/v1/auth/login"))))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
    }

    private List<Server> getServers() {
        Server devServer = new Server()
                .url("http://localhost:8081/iam")
                .description("Development Server");

        Server prodServer = new Server()
                .url("https://iam.iscm.com")
                .description("Production Server");

        return "prod".equals(activeProfile) ? List.of(prodServer) : List.of(devServer, prodServer);
    }

    @Bean
    public GroupedOpenApi publicApi() {
        return GroupedOpenApi.builder()
                .group("public")
                .pathsToMatch("/api/v1/auth/**")
                .addOperationCustomizer(publicOperationCustomizer())
                .build();
    }

    @Bean
    public GroupedOpenApi protectedApi() {
        return GroupedOpenApi.builder()
                .group("protected")
                .pathsToMatch("/api/v1/users/**", "/api/v1/roles/**", "/api/v1/organizations/**")
                .addOperationCustomizer(protectedOperationCustomizer())
                .addOpenApiCustomizer(protectedOpenApiCustomizer())
                .build();
    }

    @Bean
    public GroupedOpenApi adminApi() {
        return GroupedOpenApi.builder()
                .group("admin")
                .pathsToMatch("/api/v1/admin/**", "/actuator/**")
                .addOperationCustomizer(adminOperationCustomizer())
                .build();
    }

    @Bean
    public OperationCustomizer publicOperationCustomizer() {
        return (operation, handlerMethod) -> {
            operation.addTagsItem("Authentication");
            operation.setDescription("Public authentication endpoints (no auth required)");
            
            // Add rate limiting info for public endpoints
            if (operation.getExtensions() == null) {
                operation.addExtension("x-rate-limit", "10 requests per minute");
            }
            
            return operation;
        };
    }

    @Bean
    public OperationCustomizer protectedOperationCustomizer() {
        return (operation, handlerMethod) -> {
            operation.addTagsItem("User Management");
            operation.setDescription("Protected endpoints requiring authentication");
            
            // Add security requirement
            operation.addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
            
            // Add rate limiting info
            operation.addExtension("x-rate-limit", "100 requests per minute");
            
            // Add common responses
            addCommonResponses(operation);
            
            return operation;
        };
    }

    @Bean
    public OperationCustomizer adminOperationCustomizer() {
        return (operation, handlerMethod) -> {
            operation.addTagsItem("Administration");
            operation.setDescription("Admin-only endpoints requiring SUPER_ADMIN role");
            
            operation.addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
            operation.addExtension("x-rate-limit", "50 requests per minute");
            operation.addExtension("x-required-role", "SUPER_ADMIN");
            
            addCommonResponses(operation);
            
            return operation;
        };
    }

    @Bean
    public OpenApiCustomizer protectedOpenApiCustomizer() {
        return openApi -> {
            openApi.getPaths().forEach((path, pathItem) -> {
                pathItem.readOperations().forEach(operation -> {
                    // Add pagination parameters to GET operations
                    if ("get".equalsIgnoreCase(operation.getOperationId()) || 
                        path.contains("search") || path.contains("list")) {
                        
                        operation.addParametersItem(new io.swagger.v3.oas.models.parameters.Parameter()
                                .name("page")
                                .in("query")
                                .description("Page number (0-based)")
                                .schema(new StringSchema().example("0")));
                        
                        operation.addParametersItem(new io.swagger.v3.oas.models.parameters.Parameter()
                                .name("size")
                                .in("query")
                                .description("Page size")
                                .schema(new StringSchema().example("20")));
                        
                        operation.addParametersItem(new io.swagger.v3.oas.models.parameters.Parameter()
                                .name("sort")
                                .in("query")
                                .description("Sort by field (fieldName,asc|desc)")
                                .schema(new StringSchema().example("createdAt,desc")));
                    }
                });
            });
        };
    }

    private void addCommonResponses(io.swagger.v3.oas.models.Operation operation) {
        ApiResponses responses = operation.getResponses();
        if (responses == null) {
            responses = new ApiResponses();
            operation.setResponses(responses);
        }

        responses.addApiResponse("400", new ApiResponse()
                .description("Bad Request - Validation error or invalid input"));
        
        responses.addApiResponse("401", new ApiResponse()
                .description("Unauthorized - Invalid or missing authentication token"));
        
        responses.addApiResponse("403", new ApiResponse()
                .description("Forbidden - Insufficient permissions"));
        
        responses.addApiResponse("429", new ApiResponse()
                .description("Too Many Requests - Rate limit exceeded"));
        
        responses.addApiResponse("500", new ApiResponse()
                .description("Internal Server Error"));
    }
}