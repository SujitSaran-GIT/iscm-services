# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Development Commands

### Build and Run
```bash
# Navigate to IAM service directory
cd iam-service

# Clean and compile
mvn clean compile

# Run tests
mvn test

# Run specific test class
mvn test -Dtest=HealthCheckTest

# Build package
mvn clean package

# Run application locally
mvn spring-boot:run

# Run with specific profile
mvn spring-boot:run -Dspring-boot.run.profiles=test
```

### Docker Commands
```bash
# Development environment
docker-compose -f docker-compose.dev.yml up -d
docker-compose -f docker-compose.dev.yml logs -f
docker-compose -f docker-compose.dev.yml down

# Production environment
docker-compose -f docker-compose.prod.yml --env-file .env up -d
```

### Database Commands
```bash
# Start PostgreSQL for local development
docker run -d --name iscm-postgres -p 5432:5432 \
  -e POSTGRES_DB=iscm_iam -e POSTGRES_USER=postgres -e POSTGRES_PASSWORD=password \
  postgres:15-alpine

# Start Redis for local development
docker run -d --name iscm-redis -p 6379:6379 redis:7-alpine
```

## Architecture Overview

### Core Security Features
This IAM service implements comprehensive security features including:

- **JWT Authentication**: Access and refresh tokens with configurable expiration
- **Multi-Factor Authentication (MFA)**: TOTP, SMS, and Email-based MFA with backup codes
- **Fraud Detection**: Suspicious activity monitoring, IP change detection, failed login tracking
- **Device Management**: Device fingerprinting, trust scoring, and binding
- **OAuth Integration**: Google, Microsoft, LinkedIn SSO support
- **RBAC System**: Role-based access control with organizational and platform scopes

### Key Architecture Components

**Entity Framework**:
- `BaseEntity`: Common fields (id, createdAt, updatedAt, version, tenantId)
- All security entities extend BaseEntity for multi-tenancy and auditing
- Entities use JPA annotations with proper relationships and constraints

**Service Layer Architecture**:
- `AuthService`: Core authentication logic, JWT generation, session management
- `UserService`: User CRUD operations, role management, password policies
- `MfaService`: Multi-factor authentication setup and verification
- `PasswordResetService`: Secure password reset flow with fraud detection
- `FraudDetectionService`: Suspicious activity monitoring and alerts
- `DeviceService`: Device registration, trust scoring, fingerprinting
- `OAuthService`: External OAuth provider integration

**Security Infrastructure**:
- `JwtUtil`: JWT token generation, validation, and parsing
- `JwtAuthenticationFilter`: Spring Security filter for JWT authentication
- `RateLimitingService`: Redis-based rate limiting with configurable thresholds
- `PasswordValidator`: Strong password validation and policy enforcement

### Database Schema

The service uses Liquibase for database migrations with the following key tables:
- `users` - Core user data with MFA fields and account status
- `roles` - RBAC roles with organizational/platform scope
- `user_roles` - Many-to-many user-role relationships
- `organizations` - Multi-tenant organization management
- `login_attempts` - Failed login tracking for fraud detection
- `suspicious_activities` - Security event logging
- `user_devices` - Device registration and trust management
- `password_reset_tokens` - Secure password reset tokens
- `oauth_accounts` - External OAuth provider account linking

### Configuration Structure

**Main Configuration**: `src/main/resources/application.properties`
**Database Migrations**: `src/main/resources/db/changelog/`
- Master changelog: `db.changelog.master.yaml`
- Security features: `002-security-features.yaml`

**Key Configuration Properties**:
- JWT secret and expiration times
- Database connection settings
- Redis configuration for caching and rate limiting
- MFA configuration (TOTP window, backup codes count)
- Device binding settings (max trusted devices)
- Fraud detection thresholds

### Testing Infrastructure

**Test Configuration**: Uses TestContainers for integration testing
- PostgreSQL and Redis containers for testing
- Test-specific properties in `application-test.properties`
- Base test classes: `TestContainersConfig`, `BaseIntegrationTest`

**Test Categories**:
- Health check tests (`HealthCheckTest`)
- Authentication controller tests (`AuthControllerIntegrationTest`)
- Service layer tests (`AuthServiceTest`, `UserServiceTest`)
- Repository tests (`UserRepositoryTest`)

### API Structure

**Base Path**: `/iam`
**Authentication Endpoints**:
- `POST /api/v1/auth/register` - User registration
- `POST /api/v1/auth/login` - User login
- `POST /api/v1/auth/refresh` - Token refresh
- `POST /api/v1/auth/logout` - User logout

**User Management**:
- `GET /api/v1/users/me` - Current user profile
- `GET /api/v1/users/{id}` - Get user by ID
- `PUT /api/v1/users/{id}` - Update user
- `DELETE /api/v1/users/{id}` - Delete user

**Security Features**:
- `POST /api/v1/mfa/setup` - Setup MFA
- `POST /api/v1/mfa/verify` - Verify MFA code
- `POST /api/v1/password/reset/request` - Request password reset
- `POST /api/v1/password/reset/confirm` - Confirm password reset
- `POST /api/v1/oauth/{provider}/url` - Get OAuth URL
- `GET /api/v1/oauth/{provider}/callback` - OAuth callback

### Multi-Tenancy Support

The service supports multi-tenancy through:
- Tenant ID field in BaseEntity for data isolation
- Organization-based tenant management
- Tenant-aware user roles and permissions
- Configurable tenant isolation strategies

### Monitoring and Observability

**Actuator Endpoints**:
- `/actuator/health` - Service health status
- `/actuator/info` - Application information
- `/actuator/metrics` - Prometheus metrics
- `/actuator/prometheus` - Metrics export

**Key Metrics**:
- HTTP request counts and durations
- Authentication success/failure rates
- Database connection pool metrics
- Redis cache performance
- JVM memory and CPU usage

## Development Notes

### Database Schema Changes
- Always create Liquibase changelogs for database changes
- Use `addColumn` with proper constraints for new fields
- Include `tenant_id` column in all new entities for multi-tenancy
- Add foreign key constraints for relationships
- Create indexes for frequently queried fields

### Security Implementation
- Use `@PreAuthorize` annotations for method-level security
- Implement proper input validation in DTOs
- Rate limit authentication endpoints using Redis
- Log security events for audit trails
- Use HTTPS in production environments

### Code Patterns
- Services follow Spring Boot conventions with `@Service` annotation
- Use Lombok for boilerplate code reduction
- Implement proper exception handling with custom exceptions
- Use MapStruct for DTO mappings where needed
- Follow RESTful API design principles

### Testing Guidelines
- Write integration tests for all new features
- Test both success and error scenarios
- Use TestContainers for database-dependent tests
- Mock external services appropriately
- Include security testing for authentication flows