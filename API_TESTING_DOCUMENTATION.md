# IAM Service API Testing Documentation

## Postman Collection Setup

### Environment Variables

Create a Postman environment with the following variables:

```json
{
  "base_url": "http://localhost:8080",
  "test_user_email": "test@example.com",
  "test_user_password": "SecurePass123!",
  "test_user_firstName": "Test",
  "test_user_lastName": "User",
  "access_token": "",
  "refresh_token": "",
  "user_id": "",
  "oauth_code": "",
  "oauth_redirect_uri": "http://localhost:3000/oauth/callback"
}
```

### Collection Variables

```json
{
  "api_version": "v1",
  "auth_endpoint": "/iam/api/v1/auth",
  "users_endpoint": "/iam/api/v1/users",
  "mfa_endpoint": "/iam/api/v1/mfa",
  "password_reset_endpoint": "/iam/api/v1/password-reset",
  "oauth_endpoint": "/iam/api/v1/oauth"
}
```

## Pre-request Script

Add this pre-request script to the collection:

```javascript
// Set common headers
pm.request.headers.add({
    key: "Content-Type",
    value: "application/json"
});

// Add authorization header if access token exists
if (pm.environment.get("access_token")) {
    pm.request.headers.add({
        key: "Authorization",
        value: "Bearer " + pm.environment.get("access_token")
    });
}

// Store response time for performance monitoring
pm.globals.set("response_time", pm.response.responseTime);
```

## Test Script

Add this test script to the collection:

```javascript
// Common tests for all requests
pm.test("Status code is 2xx or 4xx", function () {
    pm.expect(pm.response.code).to.be.oneOf([200, 201, 400, 401, 403, 404, 429]);
});

// Log response for debugging
console.log("Response:", pm.response.json());
console.log("Response Time:", pm.response.responseTime + "ms");

// Store access token if present
if (pm.response.json().accessToken) {
    pm.environment.set("access_token", pm.response.json().accessToken);
    pm.environment.set("refresh_token", pm.response.json().refreshToken);
    console.log("Access token stored");
}

// Store user ID if present
if (pm.response.json().userId) {
    pm.environment.set("user_id", pm.response.json().userId);
    console.log("User ID stored:", pm.response.json().userId);
}
```

## API Testing Sequence

### 1. Health Check (Always run first)
**GET** `{{base_url}}/actuator/health`

**Purpose**: Verify the service is running and healthy

**Headers**:
```
Content-Type: application/json
```

**Expected Response** (200 OK):
```json
{
  "status": "UP",
  "components": {
    "db": {"status": "UP"},
    "redis": {"status": "UP"},
    "diskSpace": {"status": "UP"}
  }
}
```

### 2. User Registration
**POST** `{{base_url}}{{auth_endpoint}}/register`

**Purpose**: Create a new user account

**Headers**:
```
Content-Type: application/json
User-Agent: PostmanTest
X-Forwarded-For: 127.0.0.1
```

**Body**:
```json
{
  "email": "{{test_user_email}}",
  "password": "{{test_user_password}}",
  "firstName": "{{test_user_firstName}}",
  "lastName": "{{test_user_lastName}}",
  "phoneNumber": "+1234567890"
}
```

**Expected Response** (200 OK):
```json
{
  "userId": "uuid-here",
  "email": "{{test_user_email}}",
  "accessToken": "jwt-token-here",
  "refreshToken": "refresh-token-here",
  "tokenType": "Bearer",
  "expiresIn": 3600,
  "mfaEnabled": false,
  "roles": ["USER"]
}
```

### 3. User Login
**POST** `{{base_url}}{{auth_endpoint}}/login`

**Purpose**: Authenticate user and get JWT tokens

**Headers**:
```
Content-Type: application/json
User-Agent: PostmanTest
X-Forwarded-For: 127.0.0.1
```

**Body**:
```json
{
  "email": "{{test_user_email}}",
  "password": "{{test_user_password}}",
  "ipAddress": "127.0.0.1",
  "userAgent": "PostmanTest"
}
```

**Expected Response** (200 OK):
```json
{
  "userId": "uuid-here",
  "email": "{{test_user_email}}",
  "accessToken": "jwt-token-here",
  "refreshToken": "refresh-token-here",
  "tokenType": "Bearer",
  "expiresIn": 3600,
  "mfaEnabled": false,
  "roles": ["USER"]
}
```

### 4. Get Current User Profile
**GET** `{{base_url}}{{users_endpoint}}/me`

**Purpose**: Retrieve current authenticated user's profile

**Headers**:
```
Content-Type: application/json
Authorization: Bearer {{access_token}}
```

**Expected Response** (200 OK):
```json
{
  "id": "uuid-here",
  "email": "{{test_user_email}}",
  "firstName": "{{test_user_firstName}}",
  "lastName": "{{test_user_lastName}}",
  "phoneNumber": "+1234567890",
  "emailVerified": false,
  "mfaEnabled": false,
  "active": true,
  "createdAt": "2024-01-01T00:00:00Z",
  "updatedAt": "2024-01-01T00:00:00Z",
  "roles": ["USER"]
}
```

### 5. Get User by ID
**GET** `{{base_url}}{{users_endpoint}}/{{user_id}}`

**Purpose**: Retrieve user profile by ID

**Headers**:
```
Content-Type: application/json
Authorization: Bearer {{access_token}}
```

**Expected Response** (200 OK):
```json
{
  "id": "uuid-here",
  "email": "{{test_user_email}}",
  "firstName": "{{test_user_firstName}}",
  "lastName": "{{test_user_lastName}}",
  "phoneNumber": "+1234567890",
  "emailVerified": false,
  "mfaEnabled": false,
  "active": true,
  "createdAt": "2024-01-01T00:00:00Z",
  "updatedAt": "2024-01-01T00:00:00Z",
  "roles": ["USER"]
}
```

### 6. Setup TOTP MFA
**POST** `{{base_url}}{{mfa_endpoint}}/setup/totp`

**Purpose**: Generate TOTP secret and QR code for MFA setup

**Headers**:
```
Content-Type: application/json
Authorization: Bearer {{access_token}}
```

**Expected Response** (200 OK):
```json
{
  "secret": "base32-secret-here",
  "qrCodeUrl": "otpauth://totp/IAM:test@example.com?secret=base32-secret-here&issuer=IAM"
}
```

### 7. Enable MFA
**POST** `{{base_url}}{{mfa_endpoint}}/enable`

**Purpose**: Enable MFA for the user account

**Headers**:
```
Content-Type: application/json
Authorization: Bearer {{access_token}}
```

**Body**:
```json
{
  "verificationCode": "123456",
  "mfaType": "TOTP"
}
```

**Expected Response** (200 OK):
```
Empty response with status 200
```

### 8. Send SMS Verification Code
**POST** `{{base_url}}{{mfa_endpoint}}/send-sms`

**Purpose**: Send verification code via SMS

**Headers**:
```
Content-Type: application/json
Authorization: Bearer {{access_token}}
```

**Expected Response** (200 OK):
```
Empty response with status 200
```

### 9. Send Email Verification Code
**POST** `{{base_url}}{{mfa_endpoint}}/send-email`

**Purpose**: Send verification code via email

**Headers**:
```
Content-Type: application/json
Authorization: Bearer {{access_token}}
```

**Expected Response** (200 OK):
```
Empty response with status 200
```

### 10. Verify MFA Code
**POST** `{{base_url}}{{mfa_endpoint}}/verify`

**Purpose**: Verify MFA code during login

**Headers**:
```
Content-Type: application/json
Authorization: Bearer {{access_token}}
```

**Body**:
```json
{
  "code": "123456",
  "codeType": "TOTP"
}
```

**Expected Response** (200 OK):
```json
true
```

### 11. Initiate Password Reset
**POST** `{{base_url}}{{password_reset_endpoint}}/initiate`

**Purpose**: Start password reset process

**Headers**:
```
Content-Type: application/json
User-Agent: PostmanTest
X-Forwarded-For: 127.0.0.1
```

**Body**:
```json
{
  "email": "{{test_user_email}}"
}
```

**Expected Response** (200 OK):
```
Empty response with status 200
```

### 12. Validate Reset Token
**POST** `{{base_url}}{{password_reset_endpoint}}/validate-token`

**Purpose**: Validate password reset token

**Headers**:
```
Content-Type: application/json
```

**Body**:
```json
{
  "token": "reset-token-here"
}
```

**Expected Response** (200 OK):
```json
true
```

### 13. Reset Password
**POST** `{{base_url}}{{password_reset_endpoint}}/reset`

**Purpose**: Complete password reset

**Headers**:
```
Content-Type: application/json
User-Agent: PostmanTest
X-Forwarded-For: 127.0.0.1
```

**Body**:
```json
{
  "token": "reset-token-here",
  "newPassword": "NewSecurePass123!",
  "confirmPassword": "NewSecurePass123!"
}
```

**Expected Response** (200 OK):
```
Empty response with status 200
```

### 14. Get OAuth URL
**GET** `{{base_url}}{{oauth_endpoint}}/google/url?redirectUri={{oauth_redirect_uri}}`

**Purpose**: Get OAuth authorization URL

**Headers**:
```
Content-Type: application/json
```

**Expected Response** (200 OK):
```json
{
  "oauthUrl": "https://accounts.google.com/oauth2/auth?client_id=...&redirect_uri=..."
}
```

### 15. Handle OAuth Callback
**POST** `{{base_url}}{{oauth_endpoint}}/google/callback`

**Purpose**: Process OAuth callback and authenticate user

**Headers**:
```
Content-Type: application/json
User-Agent: PostmanTest
X-Forwarded-For: 127.0.0.1
```

**Body**:
```json
{
  "code": "{{oauth_code}}",
  "redirectUri": "{{oauth_redirect_uri}}"
}
```

**Expected Response** (200 OK):
```json
{
  "userId": "uuid-here",
  "email": "user@gmail.com",
  "accessToken": "jwt-token-here",
  "refreshToken": "refresh-token-here",
  "tokenType": "Bearer",
  "expiresIn": 3600,
  "mfaEnabled": false,
  "roles": ["USER"]
}
```

### 16. Get User OAuth Accounts
**GET** `{{base_url}}{{oauth_endpoint}}/accounts?userId={{user_id}}`

**Purpose**: Get all OAuth accounts linked to user

**Headers**:
```
Content-Type: application/json
Authorization: Bearer {{access_token}}
```

**Expected Response** (200 OK):
```json
[
  {
    "id": "uuid-here",
    "provider": "google",
    "providerAccountId": "google-account-id",
    "email": "user@gmail.com",
    "linked": true
  }
]
```

### 17. Unlink OAuth Account
**DELETE** `{{base_url}}{{oauth_endpoint}}/google/unlink?userId={{user_id}}`

**Purpose**: Unlink OAuth account

**Headers**:
```
Content-Type: application/json
Authorization: Bearer {{access_token}}
```

**Expected Response** (200 OK):
```
Empty response with status 200
```

### 18. Refresh Access Token
**POST** `{{base_url}}{{auth_endpoint}}/refresh`

**Purpose**: Refresh access token using refresh token

**Headers**:
```
Content-Type: application/json
```

**Body**:
```json
{
  "refreshToken": "{{refresh_token}}"
}
```

**Expected Response** (200 OK):
```json
{
  "accessToken": "new-jwt-token-here",
  "refreshToken": "new-refresh-token-here",
  "tokenType": "Bearer",
  "expiresIn": 3600
}
```

### 19. Logout User
**POST** `{{base_url}}{{auth_endpoint}}/logout`

**Purpose**: Invalidate refresh token and logout user

**Headers**:
```
Content-Type: application/json
```

**Body**:
```json
{
  "refreshToken": "{{refresh_token}}"
}
```

**Expected Response** (200 OK):
```
Empty response with status 200
```

### 20. Update User
**PUT** `{{base_url}}{{users_endpoint}}/{{user_id}}`

**Purpose**: Update user profile information

**Headers**:
```
Content-Type: application/json
Authorization: Bearer {{access_token}}
```

**Body**:
```json
{
  "firstName": "Updated",
  "lastName": "User",
  "phoneNumber": "+1987654321"
}
```

**Expected Response** (200 OK):
```json
{
  "id": "uuid-here",
  "email": "{{test_user_email}}",
  "firstName": "Updated",
  "lastName": "User",
  "phoneNumber": "+1987654321",
  "emailVerified": false,
  "mfaEnabled": false,
  "active": true,
  "createdAt": "2024-01-01T00:00:00Z",
  "updatedAt": "2024-01-01T00:00:00Z",
  "roles": ["USER"]
}
```

### 21. Delete User
**DELETE** `{{base_url}}{{users_endpoint}}/{{user_id}}`

**Purpose**: Delete user account (requires SUPER_ADMIN role)

**Headers**:
```
Content-Type: application/json
Authorization: Bearer {{access_token}}
```

**Expected Response** (200 OK):
```
Empty response with status 200
```

## Error Testing Scenarios

### 1. Invalid Credentials
**POST** `{{base_url}}{{auth_endpoint}}/login`

**Body**:
```json
{
  "email": "{{test_user_email}}",
  "password": "wrongpassword"
}
```

**Expected Response** (401 Unauthorized):
```json
{
  "timestamp": "2024-01-01T00:00:00Z",
  "status": 401,
  "error": "Unauthorized",
  "message": "Invalid credentials",
  "path": "/iam/api/v1/auth/login"
}
```

### 2. Duplicate Email Registration
**POST** `{{base_url}}{{auth_endpoint}}/register`

**Body**:
```json
{
  "email": "{{test_user_email}}",
  "password": "{{test_user_password}}",
  "firstName": "Another",
  "lastName": "User"
}
```

**Expected Response** (409 Conflict):
```json
{
  "timestamp": "2024-01-01T00:00:00Z",
  "status": 409,
  "error": "Conflict",
  "message": "Email already exists",
  "path": "/iam/api/v1/auth/register"
}
```

### 3. Invalid JWT Token
**GET** `{{base_url}}{{users_endpoint}}/me`

**Headers**:
```
Content-Type: application/json
Authorization: Bearer invalid-token
```

**Expected Response** (401 Unauthorized):
```json
{
  "timestamp": "2024-01-01T00:00:00Z",
  "status": 401,
  "error": "Unauthorized",
  "message": "Invalid JWT token",
  "path": "/iam/api/v1/users/me"
}
```

### 4. Rate Limiting Test
**POST** `{{base_url}}{{auth_endpoint}}/login`

**Body**:
```json
{
  "email": "{{test_user_email}}",
  "password": "wrongpassword"
}
```

**Expected Response** (429 Too Many Requests after multiple attempts):
```json
{
  "timestamp": "2024-01-01T00:00:00Z",
  "status": 429,
  "error": "Too Many Requests",
  "message": "Rate limit exceeded",
  "path": "/iam/api/v1/auth/login"
}
```

## Performance Testing

### Load Testing Scenarios

1. **Concurrent User Registration**: 100 concurrent registration requests
2. **Login Under Load**: 200 concurrent login requests
3. **Token Refresh**: 500 concurrent refresh requests
4. **MFA Verification**: 150 concurrent MFA verification requests

### Performance Metrics to Monitor

- **Response Time**: < 500ms for 95% of requests
- **Throughput**: > 1000 requests per second
- **Error Rate**: < 1% under normal load
- **Memory Usage**: Monitor JVM heap usage
- **Database Connections**: Monitor connection pool usage
- **Redis Performance**: Monitor cache hit/miss ratios

## Security Testing

### Input Validation Tests

1. **SQL Injection**: Attempt SQL injection in all input fields
2. **XSS Attacks**: Test for cross-site scripting vulnerabilities
3. **JWT Token Manipulation**: Test token tampering and expiration
4. **Rate Limiting Bypass**: Attempt to bypass rate limiting mechanisms

### Authentication Security Tests

1. **Password Strength**: Test weak password rejection
2. **Session Management**: Test session timeout and invalidation
3. **MFA Bypass**: Attempt to bypass MFA protection
4. **OAuth Security**: Test OAuth flow security

## Test Data Management

### Test User Accounts

Create multiple test accounts for different scenarios:

1. **Standard User**: Basic permissions
2. **Admin User**: Administrative privileges
3. **MFA Enabled User**: With TOTP/SMS/Email MFA
4. **OAuth Linked User**: Connected to external providers

### Cleanup Scripts

```javascript
// Postman test script for cleanup
if (pm.environment.get("cleanup_after_test") === "true") {
    // Delete test user after testing
    pm.sendRequest({
        url: "{{base_url}}{{users_endpoint}}/{{user_id}}",
        method: "DELETE",
        header: {
            "Content-Type": "application/json",
            "Authorization": "Bearer {{access_token}}"
        }
    }, function (err, response) {
        console.log("Cleanup completed");
    });
}
```

## Monitoring and Observability

### Log Analysis

Monitor application logs for:
- Authentication failures
- Security events
- Performance bottlenecks
- Error patterns

### Metrics Collection

Track the following metrics:
- API response times
- Authentication success/failure rates
- Database query performance
- Redis cache performance
- JVM memory usage

### Alerting Setup

Set up alerts for:
- High error rates (> 5%)
- Slow response times (> 1s)
- Database connection issues
- Redis connectivity problems
- Security events (brute force attempts)

## Integration Testing

### Database Integration

1. **Connection Testing**: Verify database connectivity
2. **Transaction Testing**: Test database transaction integrity
3. **Migration Testing**: Verify Liquibase migrations work correctly

### External Service Integration

1. **Email Service**: Test email delivery for password reset
2. **SMS Service**: Test SMS delivery for MFA codes
3. **OAuth Providers**: Test integration with Google, Microsoft, LinkedIn

### Redis Integration

1. **Cache Testing**: Verify Redis caching functionality
2. **Rate Limiting**: Test Redis-based rate limiting
3. **Session Storage**: Test session storage in Redis

## Test Automation

### CI/CD Pipeline Integration

1. **Automated Postman Tests**: Run tests as part of CI/CD pipeline
2. **Performance Tests**: Integrate load testing into deployment process
3. **Security Scans**: Include security testing in pipeline

### Test Reporting

Generate comprehensive test reports including:
- Test execution results
- Performance metrics
- Security scan results
- Error analysis and recommendations

This documentation provides a complete API testing strategy for the IAM service using Postman, covering all endpoints from authentication to user management, with proper error handling, security testing, and performance monitoring.