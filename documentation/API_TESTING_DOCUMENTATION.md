# IAM Service API Documentation

## Overview

The IAM (Identity and Access Management) Service provides comprehensive authentication, authorization, and user management capabilities. This document details all available API endpoints including both user-facing and administrative functionality.

## Base Configuration

### Service Details
- **Base URL**: `http://localhost:8081/iam`
- **API Version**: v1
- **Authentication**: JWT Bearer Token
- **Content-Type**: application/json

### Environment Variables (Postman)

Create a Postman environment with the following variables:

```json
{
  "base_url": "http://localhost:8081",
  "context_path": "/iam",
  "api_version": "v1",
  "test_user_email": "test@example.com",
  "test_user_password": "SecurePass123!",
  "test_user_firstName": "Test",
  "test_user_lastName": "User",
  "admin_user_email": "admin@example.com",
  "admin_user_password": "AdminPass123!",
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
  "auth_endpoint": "/iam/api/v1/auth",
  "users_endpoint": "/iam/api/v1/users",
  "admin_endpoint": "/iam/api/v1/admin",
  "mfa_endpoint": "/iam/api/v1/mfa",
  "password_reset_endpoint": "/iam/api/v1/password-reset",
  "oauth_endpoint": "/iam/api/v1/oauth"
}
```

## API Endpoints Overview

### Authentication & User Management Endpoints
- `POST /api/v1/auth/register` - User Registration
- `POST /api/v1/auth/login` - User Login
- `POST /api/v1/auth/refresh` - Refresh Access Token
- `POST /api/v1/auth/logout` - User Logout
- `GET /api/v1/users/me` - Get Current User Profile
- `GET /api/v1/users/{id}` - Get User by ID
- `PUT /api/v1/users/{id}` - Update User
- `DELETE /api/v1/users/{id}` - Delete User

### Admin Management Endpoints (SUPER_ADMIN required)
- `GET /api/v1/admin/users` - List All Users (Paginated)
- `GET /api/v1/admin/users/{id}` - Get User by ID
- `POST /api/v1/admin/users` - Create New User
- `PUT /api/v1/admin/users/{id}` - Update User
- `DELETE /api/v1/admin/users/{id}` - Delete User
- `POST /api/v1/admin/users/{id}/lock` - Lock User Account
- `POST /api/v1/admin/users/{id}/unlock` - Unlock User Account
- `POST /api/v1/admin/users/{id}/reset-password` - Reset User Password
- `GET /api/v1/admin/users/search` - Search Users
- `GET /api/v1/admin/users/statistics` - Get User Statistics

### Security Features Endpoints
- `POST /api/v1/mfa/setup` - Setup MFA
- `POST /api/v1/mfa/verify` - Verify MFA Code
- `POST /api/v1/password/reset/request` - Request Password Reset
- `POST /api/v1/password/reset/confirm` - Confirm Password Reset

### OAuth Integration Endpoints
- `GET /api/v1/oauth/{provider}/url` - Get OAuth URL
- `GET /api/v1/oauth/{provider}/callback` - OAuth Callback

### Health & Monitoring Endpoints
- `GET /actuator/health` - Service Health Check
- `GET /actuator/info` - Application Information
- `GET /actuator/metrics` - Performance Metrics

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
**GET** `{{base_url}}{{context_path}}/actuator/health`

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
    "db": {"status": "UP", "details": {"database": "PostgreSQL", "validationQuery": "isValid()"}},
    "redis": {"status": "UP", "details": {"version": "7.4.5"}},
    "diskSpace": {"status": "UP"},
    "ping": {"status": "UP"}
  }
}
```

### 2. User Registration
**POST** `{{base_url}}{{context_path}}{{auth_endpoint}}/register`

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
**POST** `{{base_url}}{{context_path}}{{auth_endpoint}}/login`

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
**GET** `{{base_url}}{{context_path}}{{users_endpoint}}/me`

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

## Admin API Endpoints (SUPER_ADMIN Required)

### 22. Get All Users (Admin)
**GET** `{{base_url}}{{context_path}}{{admin_endpoint}}/users?page=0&size=20&sort=createdAt&direction=desc`

**Purpose**: Retrieve paginated list of all users in the system

**Headers**:
```
Content-Type: application/json
Authorization: Bearer {{access_token}}
```

**Query Parameters**:
- `page` (optional, default: 0) - Page number (0-based)
- `size` (optional, default: 20) - Page size
- `sort` (optional, default: "createdAt") - Sort field
- `direction` (optional, default: "desc") - Sort direction ("asc" or "desc")

**Expected Response** (200 OK):
```json
{
  "users": [
    {
      "id": "uuid-here",
      "email": "user@example.com",
      "firstName": "John",
      "lastName": "Doe",
      "phoneNumber": "+1234567890",
      "emailVerified": false,
      "mfaEnabled": false,
      "active": true,
      "createdAt": "2024-01-01T00:00:00Z",
      "updatedAt": "2024-01-01T00:00:00Z",
      "roles": ["USER"],
      "tenantId": null
    }
  ],
  "page": 0,
  "size": 20,
  "total": 150,
  "totalPages": 8,
  "last": false
}
```

**Access**: Requires SUPER_ADMIN role

### 23. Get User by ID (Admin)
**GET** `{{base_url}}{{context_path}}{{admin_endpoint}}/users/{userId}`

**Purpose**: Retrieve specific user details by ID

**Headers**:
```
Content-Type: application/json
Authorization: Bearer {{access_token}}
```

**Expected Response** (200 OK):
```json
{
  "id": "uuid-here",
  "email": "user@example.com",
  "firstName": "John",
  "lastName": "Doe",
  "phoneNumber": "+1234567890",
  "emailVerified": false,
  "mfaEnabled": false,
  "active": true,
  "createdAt": "2024-01-01T00:00:00Z",
  "updatedAt": "2024-01-01T00:00:00Z",
  "roles": ["USER"],
  "tenantId": null
}
```

**Access**: Requires SUPER_ADMIN or ADMIN role

### 24. Create User (Admin)
**POST** `{{base_url}}{{context_path}}{{admin_endpoint}}/users`

**Purpose**: Create a new user account with specified roles

**Headers**:
```
Content-Type: application/json
Authorization: Bearer {{access_token}}
```

**Body**:
```json
{
  "email": "newuser@example.com",
  "password": "SecurePass123!",
  "firstName": "New",
  "lastName": "User",
  "phone": "+1234567890",
  "active": true,
  "mfaEnabled": false,
  "roles": ["USER"],
  "tenantId": null
}
```

**Expected Response** (200 OK):
```json
{
  "id": "uuid-here",
  "email": "newuser@example.com",
  "firstName": "New",
  "lastName": "User",
  "phoneNumber": "+1234567890",
  "emailVerified": false,
  "mfaEnabled": false,
  "active": true,
  "createdAt": "2024-01-01T00:00:00Z",
  "updatedAt": "2024-01-01T00:00:00Z",
  "roles": ["USER"],
  "tenantId": null
}
```

**Access**: Requires SUPER_ADMIN role

### 25. Update User (Admin)
**PUT** `{{base_url}}{{context_path}}{{admin_endpoint}}/users/{userId}`

**Purpose**: Update existing user information and roles

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
  "phone": "+1987654321",
  "active": true,
  "mfaEnabled": false,
  "roles": ["USER", "ADMIN"],
  "tenantId": null
}
```

**Expected Response** (200 OK):
```json
{
  "id": "uuid-here",
  "email": "user@example.com",
  "firstName": "Updated",
  "lastName": "User",
  "phoneNumber": "+1987654321",
  "emailVerified": false,
  "mfaEnabled": false,
  "active": true,
  "createdAt": "2024-01-01T00:00:00Z",
  "updatedAt": "2024-01-01T00:00:00Z",
  "roles": ["USER", "ADMIN"],
  "tenantId": null
}
```

**Access**: Requires SUPER_ADMIN or ADMIN role

### 26. Delete User (Admin)
**DELETE** `{{base_url}}{{context_path}}{{admin_endpoint}}/users/{userId}`

**Purpose**: Permanently delete a user account

**Headers**:
```
Content-Type: application/json
Authorization: Bearer {{access_token}}
```

**Expected Response** (200 OK):
```
Empty response with status 200
```

**Access**: Requires SUPER_ADMIN role
**Note**: Cannot delete the last super admin user

### 27. Lock User Account (Admin)
**POST** `{{base_url}}{{context_path}}{{admin_endpoint}}/users/{userId}/lock?durationMinutes=30`

**Purpose**: Lock user account for specified duration

**Headers**:
```
Content-Type: application/json
Authorization: Bearer {{access_token}}
```

**Query Parameters**:
- `durationMinutes` (optional, default: 30) - Lock duration in minutes

**Expected Response** (200 OK):
```
Empty response with status 200
```

**Access**: Requires SUPER_ADMIN role

### 28. Unlock User Account (Admin)
**POST** `{{base_url}}{{context_path}}{{admin_endpoint}}/users/{userId}/unlock`

**Purpose**: Unlock a previously locked user account

**Headers**:
```
Content-Type: application/json
Authorization: Bearer {{access_token}}
```

**Expected Response** (200 OK):
```
Empty response with status 200
```

**Access**: Requires SUPER_ADMIN role

### 29. Reset User Password (Admin)
**POST** `{{base_url}}{{context_path}}{{admin_endpoint}}/users/{userId}/reset-password?newPassword=NewSecurePass123!`

**Purpose**: Reset user's password to a new value

**Headers**:
```
Content-Type: application/json
Authorization: Bearer {{access_token}}
```

**Query Parameters**:
- `newPassword` (required) - New password for the user

**Expected Response** (200 OK):
```
Empty response with status 200
```

**Access**: Requires SUPER_ADMIN role
**Note**: New password must meet security requirements

### 30. Search Users (Admin)
**GET** `{{base_url}}{{context_path}}{{admin_endpoint}}/users/search?query=john&page=0&size=20`

**Purpose**: Search users by email, first name, or last name

**Headers**:
```
Content-Type: application/json
Authorization: Bearer {{access_token}}
```

**Query Parameters**:
- `query` (required) - Search term for email, first name, or last name
- `page` (optional, default: 0) - Page number
- `size` (optional, default: 20) - Page size

**Expected Response** (200 OK):
```json
{
  "users": [
    {
      "id": "uuid-here",
      "email": "john.doe@example.com",
      "firstName": "John",
      "lastName": "Doe",
      "phoneNumber": "+1234567890",
      "emailVerified": false,
      "mfaEnabled": false,
      "active": true,
      "createdAt": "2024-01-01T00:00:00Z",
      "updatedAt": "2024-01-01T00:00:00Z",
      "roles": ["USER"],
      "tenantId": null
    }
  ],
  "page": 0,
  "size": 20,
  "total": 5,
  "totalPages": 1,
  "last": true
}
```

**Access**: Requires SUPER_ADMIN or ADMIN role

### 31. Get User Statistics (Admin)
**GET** `{{base_url}}{{context_path}}{{admin_endpoint}}/users/statistics`

**Purpose**: Retrieve comprehensive user statistics for admin dashboard

**Headers**:
```
Content-Type: application/json
Authorization: Bearer {{access_token}}
```

**Expected Response** (200 OK):
```json
{
  "totalUsers": 150,
  "activeUsers": 145,
  "inactiveUsers": 5,
  "lockedUsers": 2,
  "mfaEnabledUsers": 120,
  "registeredToday": 3,
  "registeredThisWeek": 15,
  "registeredThisMonth": 45,
  "superAdminCount": 2,
  "adminCount": 5,
  "regularUserCount": 143
}
```

**Access**: Requires SUPER_ADMIN role

## Error Testing Scenarios

### 1. Invalid Credentials
**POST** `{{base_url}}{{context_path}}{{auth_endpoint}}/login`

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

### 5. Admin Access Without SUPER_ADMIN Role
**GET** `{{base_url}}{{context_path}}{{admin_endpoint}}/users`

**Headers**:
```
Content-Type: application/json
Authorization: Bearer {{access_token}}
```

**Expected Response** (403 Forbidden):
```json
{
  "status": 403,
  "error": "Access Denied",
  "message": "You don't have permission to access this resource",
  "path": "/iam/api/v1/admin/users"
}
```

### 6. Attempt to Delete Last Super Admin
**DELETE** `{{base_url}}{{context_path}}{{admin_endpoint}}/users/{super_admin_id}`

**Headers**:
```
Content-Type: application/json
Authorization: Bearer {{access_token}}
```

**Expected Response** (400 Bad Request):
```json
{
  "timestamp": "2024-01-01T00:00:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Cannot delete the last super admin user",
  "path": "/iam/api/v1/admin/users/{super_admin_id}"
}
```

### 7. Weak Password in Admin User Creation
**POST** `{{base_url}}{{context_path}}{{admin_endpoint}}/users`

**Body**:
```json
{
  "email": "newuser@example.com",
  "password": "weak",
  "firstName": "New",
  "lastName": "User"
}
```

**Expected Response** (400 Bad Request):
```json
{
  "timestamp": "2024-01-01T00:00:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Password does not meet security requirements",
  "path": "/iam/api/v1/admin/users"
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

## Summary and Best Practices

### API Usage Guidelines

1. **Authentication**: All protected endpoints require a valid JWT Bearer token
2. **Authorization**: Admin endpoints require specific roles (SUPER_ADMIN/ADMIN)
3. **Rate Limiting**: Implement client-side rate limiting to avoid API abuse
4. **Error Handling**: Always check HTTP status codes and error responses
5. **Token Management**: Refresh tokens before expiration for seamless user experience

### Security Best Practices

1. **Password Requirements**:
   - Minimum 8 characters
   - At least 1 uppercase letter
   - At least 1 lowercase letter
   - At least 1 digit
   - At least 1 special character
   - No whitespace allowed

2. **JWT Token Security**:
   - Store tokens securely (HttpOnly cookies recommended)
   - Implement proper token expiration handling
   - Use HTTPS in production environments
   - Validate token signatures

3. **Admin Security**:
   - Use principle of least privilege for admin roles
   - Implement audit logging for admin actions
   - Never share super admin credentials
   - Regular security audits of admin accounts

### Common Response Codes

- **200 OK**: Successful operation
- **201 Created**: Resource successfully created
- **400 Bad Request**: Invalid request parameters or body
- **401 Unauthorized**: Authentication failed or token expired
- **403 Forbidden**: Insufficient permissions for the requested action
- **404 Not Found**: Requested resource does not exist
- **409 Conflict**: Resource already exists (e.g., duplicate email)
- **429 Too Many Requests**: Rate limit exceeded
- **500 Internal Server Error**: Server-side error

### Testing Checklist

#### Authentication Flow
- [ ] User registration with valid data
- [ ] User registration with duplicate email (expect 409)
- [ ] User login with valid credentials
- [ ] User login with invalid credentials (expect 401)
- [ ] Token refresh before expiration
- [ ] User logout (token invalidation)

#### User Management
- [ ] Get current user profile
- [ ] Update user profile
- [ ] Get user by ID
- [ ] Delete user account

#### Admin Operations
- [ ] Access admin endpoints without SUPER_ADMIN role (expect 403)
- [ ] List all users (paginated)
- [ ] Create new user with admin privileges
- [ ] Update user roles and status
- [ ] Lock/unlock user accounts
- [ ] Reset user passwords
- [ ] Search users by email/name
- [ ] Get user statistics dashboard

#### Error Scenarios
- [ ] Invalid JWT tokens
- [ ] Expired tokens
- [ ] Missing required fields
- [ ] Invalid email formats
- [ ] Weak password rejection
- [ ] Rate limiting triggers

### API Version Compatibility

This documentation covers API version v1. Future versions may introduce:
- Breaking changes with proper deprecation notices
- New endpoints and features
- Enhanced security measures
- Performance optimizations

### Support and Troubleshooting

For common issues:
1. **401 Unauthorized**: Check token validity and expiration
2. **403 Forbidden**: Verify user roles and permissions
3. **404 Not Found**: Confirm endpoint URLs and resource IDs
4. **429 Too Many Requests**: Implement request throttling
5. **500 Errors**: Check service logs and database connectivity

This comprehensive documentation provides a complete API testing strategy for the IAM service using Postman, covering all endpoints from authentication to user management, with proper error handling, security testing, performance monitoring, and extensive admin functionality coverage.