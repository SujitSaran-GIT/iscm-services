# ISCM IAM Service - API Endpoint Testing Guide

## Overview

This comprehensive guide provides detailed testing instructions for all endpoints in the ISCM Identity and Access Management (IAM) service. It includes proper HTTP headers, request bodies, parameters, and expected responses for each endpoint.

## Base Configuration

### Base URL
```
http://localhost:8081/iam/api/v1
```

### Common Headers

#### Content-Type
```http
Content-Type: application/json
```

#### Authorization (for protected endpoints)
```http
Authorization: Bearer <access_token>
```

#### Accept
```http
Accept: application/json
```

### Common Response Codes
- `200 OK` - Successful request
- `201 Created` - Resource created successfully
- `400 Bad Request` - Invalid request parameters
- `401 Unauthorized` - Authentication required/invalid
- `403 Forbidden` - Insufficient permissions
- `404 Not Found` - Resource not found
- `409 Conflict` - Resource already exists
- `422 Unprocessable Entity` - Validation errors
- `429 Too Many Requests` - Rate limit exceeded
- `500 Internal Server Error` - Server error

---

## Authentication Endpoints

### 1. User Registration

#### POST `/auth/register`

**Description**: Register a new user account

**Headers**:
```http
POST /iam/api/v1/auth/register HTTP/1.1
Host: localhost:8081
Content-Type: application/json
Accept: application/json
```

**Request Body**:
```json
{
    "email": "user@example.com",
    "password": "SecurePassword123!",
    "firstName": "John",
    "lastName": "Doe",
    "phone": "+1234567890"
}
```

**Request Body Parameters**:
- `email` (string, required) - User's email address (must be unique)
- `password` (string, required) - User's password (min 8 characters, uppercase, lowercase, number, special char)
- `firstName` (string, required) - User's first name
- `lastName` (string, required) - User's last name
- `phone` (string, optional) - User's phone number

**cURL Example**:
```bash
curl -X POST http://localhost:8081/iam/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -H "Accept: application/json" \
  -d '{
    "email": "user@example.com",
    "password": "SecurePassword123!",
    "firstName": "John",
    "lastName": "Doe"
  }'
```

**Success Response (201)**:
```json
{
    "status": "success",
    "message": "User registered successfully",
    "data": {
        "userId": "550e8400-e29b-41d4-a716-446655440000",
        "email": "user@example.com",
        "firstName": "John",
        "lastName": "Doe",
        "roles": ["SUPER_ADMIN"],
        "createdAt": "2025-11-23T15:30:00.000Z"
    }
}
```

**Error Response (400)**:
```json
{
    "status": "error",
    "message": "Validation failed",
    "errors": [
        {
            "field": "email",
            "message": "Email is not valid"
        },
        {
            "field": "password",
            "message": "Password must contain at least 8 characters"
        }
    ]
}
```

**Rate Limiting**: 3 requests per hour per IP

---

### 2. User Login

#### POST `/auth/login`

**Description**: Authenticate user and receive JWT tokens

**Headers**:
```http
POST /iam/api/v1/auth/login HTTP/1.1
Host: localhost:8081
Content-Type: application/json
Accept: application/json
```

**Request Body**:
```json
{
    "email": "user@example.com",
    "password": "SecurePassword123!"
}
```

**Request Body Parameters**:
- `email` (string, required) - User's email address
- `password` (string, required) - User's password

**cURL Example**:
```bash
curl -X POST http://localhost:8081/iam/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -H "Accept: application/json" \
  -d '{
    "email": "user@example.com",
    "password": "SecurePassword123!"
  }'
```

**Success Response (200)**:
```json
{
    "status": "success",
    "message": "Login successful",
    "data": {
        "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
        "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
        "tokenType": "Bearer",
        "expiresIn": 900,
        "refreshExpiresIn": 604800,
        "user": {
            "id": "550e8400-e29b-41d4-a716-446655440000",
            "email": "user@example.com",
            "firstName": "John",
            "lastName": "Doe",
            "roles": ["USER"]
        }
    }
}
```

**Error Response (401)**:
```json
{
    "status": "error",
    "message": "Invalid email or password",
    "timestamp": "2025-11-23T15:30:00.000Z"
}
```

**Rate Limiting**: 5 requests per 5 minutes per IP

---

### 3. Token Refresh

#### POST `/auth/refresh`

**Description**: Refresh access token using refresh token

**Headers**:
```http
POST /iam/api/v1/auth/refresh HTTP/1.1
Host: localhost:8081
Content-Type: application/json
Accept: application/json
```

**Request Body**:
```json
{
    "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

**Request Body Parameters**:
- `refreshToken` (string, required) - Valid refresh token

**cURL Example**:
```bash
curl -X POST http://localhost:8081/iam/api/v1/auth/refresh \
  -H "Content-Type: application/json" \
  -H "Accept: application/json" \
  -d '{
    "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
  }'
```

**Success Response (200)**:
```json
{
    "status": "success",
    "message": "Token refreshed successfully",
    "data": {
        "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
        "expiresIn": 900
    }
}
```

**Error Response (401)**:
```json
{
    "status": "error",
    "message": "Invalid or expired refresh token",
    "timestamp": "2025-11-23T15:30:00.000Z"
}
```

---

### 4. User Logout

#### POST `/auth/logout`

**Description**: Logout user and invalidate tokens

**Headers**:
```http
POST /iam/api/v1/auth/logout HTTP/1.1
Host: localhost:8081
Content-Type: application/json
Authorization: Bearer <access_token>
Accept: application/json
```

**Request Body**:
```json
{
    "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

**cURL Example**:
```bash
curl -X POST http://localhost:8081/iam/api/v1/auth/logout \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <access_token>" \
  -H "Accept: application/json" \
  -d '{
    "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
  }'
```

**Success Response (200)**:
```json
{
    "status": "success",
    "message": "Logout successful"
}
```

---

## User Management Endpoints

### 5. Get Current User Profile

#### GET `/users/me`

**Description**: Get current user's profile information

**Headers**:
```http
GET /iam/api/v1/users/me HTTP/1.1
Host: localhost:8081
Authorization: Bearer <access_token>
Accept: application/json
```

**cURL Example**:
```bash
curl -X GET http://localhost:8081/iam/api/v1/users/me \
  -H "Authorization: Bearer <access_token>" \
  -H "Accept: application/json"
```

**Success Response (200)**:
```json
{
    "status": "success",
    "data": {
        "id": "550e8400-e29b-41d4-a716-446655440000",
        "email": "user@example.com",
        "firstName": "John",
        "lastName": "Doe",
        "phone": "+1234567890",
        "isActive": true,
        "roles": ["USER"],
        "organization": {
            "id": "00000000-0000-0000-0000-000000000001",
            "name": "ISCM Platform"
        },
        "lastLoginAt": "2025-11-23T15:30:00.000Z",
        "createdAt": "2025-11-23T10:00:00.000Z",
        "updatedAt": "2025-11-23T15:30:00.000Z"
    }
}
```

**Error Response (401)**:
```json
{
    "status": "error",
    "message": "Unauthorized - Invalid or expired token",
    "timestamp": "2025-11-23T15:30:00.000Z"
}
```

---

### 6. Get User by ID

#### GET `/users/{userId}`

**Description**: Get user information by user ID (Admin only)

**Headers**:
```http
GET /iam/api/v1/users/550e8400-e29b-41d4-a716-446655440000 HTTP/1.1
Host: localhost:8081
Authorization: Bearer <admin_access_token>
Accept: application/json
```

**Path Parameters**:
- `userId` (UUID, required) - User ID to retrieve

**cURL Example**:
```bash
curl -X GET http://localhost:8081/iam/api/v1/users/550e8400-e29b-41d4-a716-446655440000 \
  -H "Authorization: Bearer <admin_access_token>" \
  -H "Accept: application/json"
```

**Success Response (200)**:
```json
{
    "status": "success",
    "data": {
        "id": "550e8400-e29b-41d4-a716-446655440000",
        "email": "user@example.com",
        "firstName": "John",
        "lastName": "Doe",
        "isActive": true,
        "roles": ["USER"],
        "createdAt": "2025-11-23T10:00:00.000Z",
        "lastLoginAt": "2025-11-23T15:30:00.000Z"
    }
}
```

**Error Response (404)**:
```json
{
    "status": "error",
    "message": "User not found",
    "timestamp": "2025-11-23T15:30:00.000Z"
}
```

---

### 7. Update User Profile

#### PUT `/users/{userId}`

**Description**: Update user profile information

**Headers**:
```http
PUT /iam/api/v1/users/550e8400-e29b-41d4-a716-446655440000 HTTP/1.1
Host: localhost:8081
Content-Type: application/json
Authorization: Bearer <access_token>
Accept: application/json
```

**Path Parameters**:
- `userId` (UUID, required) - User ID to update

**Request Body**:
```json
{
    "firstName": "John",
    "lastName": "Smith",
    "phone": "+1234567890",
    "isActive": true
}
```

**Request Body Parameters**:
- `firstName` (string, optional) - Updated first name
- `lastName` (string, optional) - Updated last name
- `phone` (string, optional) - Updated phone number
- `isActive` (boolean, optional) - Account status (admin only)

**cURL Example**:
```bash
curl -X PUT http://localhost:8081/iam/api/v1/users/550e8400-e29b-41d4-a716-446655440000 \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <access_token>" \
  -H "Accept: application/json" \
  -d '{
    "firstName": "John",
    "lastName": "Smith",
    "phone": "+1234567890"
  }'
```

**Success Response (200)**:
```json
{
    "status": "success",
    "message": "User updated successfully",
    "data": {
        "id": "550e8400-e29b-41d4-a716-446655440000",
        "email": "user@example.com",
        "firstName": "John",
        "lastName": "Smith",
        "phone": "+1234567890",
        "updatedAt": "2025-11-23T15:30:00.000Z"
    }
}
```

---

### 8. Delete User

#### DELETE `/users/{userId}`

**Description**: Delete user account (Admin only)

**Headers**:
```http
DELETE /iam/api/v1/users/550e8400-e29b-41d4-a716-446655440000 HTTP/1.1
Host: localhost:8081
Authorization: Bearer <admin_access_token>
Accept: application/json
```

**Path Parameters**:
- `userId` (UUID, required) - User ID to delete

**cURL Example**:
```bash
curl -X DELETE http://localhost:8081/iam/api/v1/users/550e8400-e29b-41d4-a716-446655440000 \
  -H "Authorization: Bearer <admin_access_token>" \
  -H "Accept: application/json"
```

**Success Response (200)**:
```json
{
    "status": "success",
    "message": "User deleted successfully"
}
```

**Error Response (404)**:
```json
{
    "status": "error",
    "message": "User not found",
    "timestamp": "2025-11-23T15:30:00.000Z"
}
```

---

### 9. Search Users

#### GET `/users/search`

**Description**: Search for users with pagination and filters

**Headers**:
```http
GET /iam/api/v1/users/search?page=0&size=10&search=john&role=USER&active=true HTTP/1.1
Host: localhost:8081
Authorization: Bearer <admin_access_token>
Accept: application/json
```

**Query Parameters**:
- `page` (int, optional, default: 0) - Page number
- `size` (int, optional, default: 10) - Page size
- `search` (string, optional) - Search term (searches in name, email)
- `role` (string, optional) - Filter by role name
- `active` (boolean, optional) - Filter by active status
- `organization` (string, optional) - Filter by organization ID

**cURL Example**:
```bash
curl -X GET "http://localhost:8081/iam/api/v1/users/search?page=0&size=10&search=john&role=USER&active=true" \
  -H "Authorization: Bearer <admin_access_token>" \
  -H "Accept: application/json"
```

**Success Response (200)**:
```json
{
    "status": "success",
    "data": {
        "content": [
            {
                "id": "550e8400-e29b-41d4-a716-446655440000",
                "email": "john.doe@example.com",
                "firstName": "John",
                "lastName": "Doe",
                "isActive": true,
                "roles": ["USER"],
                "createdAt": "2025-11-23T10:00:00.000Z",
                "lastLoginAt": "2025-11-23T15:30:00.000Z"
            }
        ],
        "pageable": {
            "page": 0,
            "size": 10,
            "totalElements": 1,
            "totalPages": 1
        }
    }
}
```

---

## Multi-Factor Authentication (MFA) Endpoints

### 10. Setup MFA

#### POST `/mfa/setup`

**Description**: Setup Multi-Factor Authentication for user

**Headers**:
```http
POST /iam/api/v1/mfa/setup HTTP/1.1
Host: localhost:8081
Authorization: Bearer <access_token>
Content-Type: application/json
Accept: application/json
```

**Request Body**:
```json
{
    "password": "CurrentPassword123!"
}
```

**Request Body Parameters**:
- `password` (string, required) - User's current password for verification

**cURL Example**:
```bash
curl -X POST http://localhost:8081/iam/api/v1/mfa/setup \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <access_token>" \
  -H "Accept: application/json" \
  -d '{
    "password": "CurrentPassword123!"
  }'
```

**Success Response (200)**:
```json
{
    "status": "success",
    "message": "MFA setup initiated",
    "data": {
        "qrCodeUrl": "https://api.qrserver.com/v1/create-qr-code/?size=200x200&data=otpauth://totp/ISCM:user@example.com?secret=JBSWY3DPEHPK3PXP&issuer=ISCM",
        "secretKey": "JBSWY3DPEHPK3PXP",
        "backupCodes": [
            "12345678",
            "23456789",
            "34567890",
            "45678901",
            "56789012"
        ]
    }
}
```

---

### 11. Verify MFA

#### POST `/mfa/verify`

**Description**: Verify MFA code during login or setup

**Headers**:
```http
POST /iam/api/v1/mfa/verify HTTP/1.1
Host: localhost:8081
Content-Type: application/json
Accept: application/json
```

**Request Body**:
```json
{
    "email": "user@example.com",
    "code": "123456",
    "sessionToken": "session_token_here"
}
```

**Request Body Parameters**:
- `email` (string, required) - User's email
- `code` (string, required) - 6-digit TOTP code
- `sessionToken` (string, required) - Session token from login step

**cURL Example**:
```bash
curl -X POST http://localhost:8081/iam/api/v1/mfa/verify \
  -H "Content-Type: application/json" \
  -H "Accept: application/json" \
  -d '{
    "email": "user@example.com",
    "code": "123456",
    "sessionToken": "session_token_here"
  }'
```

**Success Response (200)**:
```json
{
    "status": "success",
    "message": "MFA verification successful",
    "data": {
        "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
        "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
        "user": {
            "id": "550e8400-e29b-41d4-a716-446655440000",
            "email": "user@example.com",
            "mfaEnabled": true
        }
    }
}
```

---

### 12. Disable MFA

#### POST `/mfa/disable`

**Description**: Disable Multi-Factor Authentication for user

**Headers**:
```http
POST /iam/api/v1/mfa/disable HTTP/1.1
Host: localhost:8081
Authorization: Bearer <access_token>
Content-Type: application/json
Accept: application/json
```

**Request Body**:
```json
{
    "password": "CurrentPassword123!",
    "totpCode": "123456"
}
```

**Request Body Parameters**:
- `password` (string, required) - User's current password
- `totpCode` (string, required) - Current TOTP code for verification

**cURL Example**:
```bash
curl -X POST http://localhost:8081/iam/api/v1/mfa/disable \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <access_token>" \
  -H "Accept: application/json" \
  -d '{
    "password": "CurrentPassword123!",
    "totpCode": "123456"
  }'
```

**Success Response (200)**:
```json
{
    "status": "success",
    "message": "MFA disabled successfully"
}
```

---

## Password Reset Endpoints

### 13. Request Password Reset

#### POST `/password/reset/request`

**Description**: Request password reset link via email

**Headers**:
```http
POST /iam/api/v1/password/reset/request HTTP/1.1
Host: localhost:8081
Content-Type: application/json
Accept: application/json
```

**Request Body**:
```json
{
    "email": "user@example.com"
}
```

**Request Body Parameters**:
- `email` (string, required) - User's email address

**cURL Example**:
```bash
curl -X POST http://localhost:8081/iam/api/v1/password/reset/request \
  -H "Content-Type: application/json" \
  -H "Accept: application/json" \
  -d '{
    "email": "user@example.com"
  }'
```

**Success Response (200)**:
```json
{
    "status": "success",
    "message": "Password reset link sent to your email"
}
```

**Rate Limiting**: 3 requests per 15 minutes per IP

---

### 14. Confirm Password Reset

#### POST `/password/reset/confirm`

**Description**: Reset password using reset token

**Headers**:
```http
POST /iam/api/v1/password/reset/confirm HTTP/1.1
Host: localhost:8081
Content-Type: application/json
Accept: application/json
```

**Request Body**:
```json
{
    "token": "reset_token_here",
    "newPassword": "NewSecurePassword123!"
}
```

**Request Body Parameters**:
- `token` (string, required) - Password reset token from email
- `newPassword` (string, required) - New password (min 8 characters, uppercase, lowercase, number, special char)

**cURL Example**:
```bash
curl -X POST http://localhost:8081/iam/api/v1/password/reset/confirm \
  -H "Content-Type: application/json" \
  -H "Accept: application/json" \
  -d '{
    "token": "reset_token_here",
    "newPassword": "NewSecurePassword123!"
  }'
```

**Success Response (200)**:
```json
{
    "status": "success",
    "message": "Password reset successful"
}
```

**Error Response (400)**:
```json
{
    "status": "error",
    "message": "Invalid or expired reset token",
    "timestamp": "2025-11-23T15:30:00.000Z"
}
```

---

## Role Management Endpoints

### 15. Assign Role to User

#### POST `/users/{userId}/roles`

**Description**: Assign a role to a user (Admin only)

**Headers**:
```http
POST /iam/api/v1/users/550e8400-e29b-41d4-a716-446655440000/roles HTTP/1.1
Host: localhost:8081
Authorization: Bearer <admin_access_token>
Content-Type: application/json
Accept: application/json
```

**Path Parameters**:
- `userId` (UUID, required) - User ID to assign role to

**Request Body**:
```json
{
    "roleName": "ADMIN",
    "scope": "ORGANIZATION"
}
```

**Request Body Parameters**:
- `roleName` (string, required) - Role name to assign
- `scope` (string, optional) - Role scope (PLATFORM, ORGANIZATION)

**cURL Example**:
```bash
curl -X POST http://localhost:8081/iam/api/v1/users/550e8400-e29b-41d4-a716-446655440000/roles \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <admin_access_token>" \
  -H "Accept: application/json" \
  -d '{
    "roleName": "ADMIN",
    "scope": "ORGANIZATION"
  }'
```

**Success Response (200)**:
```json
{
    "status": "success",
    "message": "Role assigned successfully"
}
```

---

### 16. Remove Role from User

#### DELETE `/users/{userId}/roles/{roleName}`

**Description**: Remove a role from a user (Admin only)

**Headers**:
```http
DELETE /iam/api/v1/users/550e8400-e29b-41d4-a716-446655440000/roles/ADMIN HTTP/1.1
Host: localhost:8081
Authorization: Bearer <admin_access_token>
Accept: application/json
```

**Path Parameters**:
- `userId` (UUID, required) - User ID to remove role from
- `roleName` (string, required) - Role name to remove

**cURL Example**:
```bash
curl -X DELETE http://localhost:8081/iam/api/v1/users/550e8400-e29b-41d4-a716-446655440000/roles/ADMIN \
  -H "Authorization: Bearer <admin_access_token>" \
  -H "Accept: application/json"
```

**Success Response (200)**:
```json
{
    "status": "success",
    "message": "Role removed successfully"
}
```

---

### 17. Get All Roles

#### GET `/roles`

**Description**: Get all available roles (Admin only)

**Headers**:
```http
GET /iam/api/v1/roles HTTP/1.1
Host: localhost:8081
Authorization: Bearer <admin_access_token>
Accept: application/json
```

**cURL Example**:
```bash
curl -X GET http://localhost:8081/iam/api/v1/roles \
  -H "Authorization: Bearer <admin_access_token>" \
  -H "Accept: application/json"
```

**Success Response (200)**:
```json
{
    "status": "success",
    "data": [
        {
            "id": "00000000-0000-0000-0000-000000000201",
            "name": "SUPER_ADMIN",
            "description": "Super Administrator with full access",
            "scope": "PLATFORM",
            "permissions": [
                "USER_READ",
                "USER_WRITE",
                "ROLE_READ",
                "ROLE_WRITE"
            ],
            "createdAt": "2025-11-23T10:00:00.000Z"
        },
        {
            "id": "00000000-0000-0000-0000-000000000202",
            "name": "USER",
            "description": "Regular user with basic access",
            "scope": "PLATFORM",
            "permissions": [
                "USER_READ"
            ],
            "createdAt": "2025-11-23T10:00:00.000Z"
        }
    ]
}
```

---

## Session Management Endpoints

### 18. Get User Sessions

#### GET `/users/{userId}/sessions`

**Description**: Get all active sessions for a user (Admin or owner)

**Headers**:
```http
GET /iam/api/v1/users/550e8400-e29b-41d4-a716-446655440000/sessions HTTP/1.1
Host: localhost:8081
Authorization: Bearer <access_token>
Accept: application/json
```

**Path Parameters**:
- `userId` (UUID, required) - User ID to get sessions for

**cURL Example**:
```bash
curl -X GET http://localhost:8081/iam/api/v1/users/550e8400-e29b-41d4-a716-446655440000/sessions \
  -H "Authorization: Bearer <access_token>" \
  -H "Accept: application/json"
```

**Success Response (200)**:
```json
{
    "status": "success",
    "data": [
        {
            "id": "session-id-here",
            "userAgent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36",
            "ipAddress": "192.168.1.100",
            "createdAt": "2025-11-23T15:00:00.000Z",
            "expiresAt": "2025-11-30T15:00:00.000Z",
            "isCurrent": true
        }
    ]
}
```

---

### 19. Revoke Session

#### DELETE `/users/{userId}/sessions/{sessionId}`

**Description**: Revoke a specific user session (Admin or owner)

**Headers**:
```http
DELETE /iam/api/v1/users/550e8400-e29b-41d4-a716-446655440000/sessions/session-id-here HTTP/1.1
Host: localhost:8081
Authorization: Bearer <access_token>
Accept: application/json
```

**Path Parameters**:
- `userId` (UUID, required) - User ID
- `sessionId` (string, required) - Session ID to revoke

**cURL Example**:
```bash
curl -X DELETE http://localhost:8081/iam/api/v1/users/550e8400-e29b-41d4-a716-446655440000/sessions/session-id-here \
  -H "Authorization: Bearer <access_token>" \
  -H "Accept: application/json"
```

**Success Response (200)**:
```json
{
    "status": "success",
    "message": "Session revoked successfully"
}
```

---

### 20. Revoke All Sessions

#### DELETE `/users/{userId}/sessions`

**Description**: Revoke all sessions for a user (Admin or owner)

**Headers**:
```http
DELETE /iam/api/v1/users/550e8400-e29b-41d4-a716-446655440000/sessions HTTP/1.1
Host: localhost:8081
Authorization: Bearer <access_token>
Accept: application/json
```

**Path Parameters**:
- `userId` (UUID, required) - User ID to revoke all sessions for

**cURL Example**:
```bash
curl -X DELETE http://localhost:8081/iam/api/v1/users/550e8400-e29b-41d4-a716-446655440000/sessions \
  -H "Authorization: Bearer <access_token>" \
  -H "Accept: application/json"
```

**Success Response (200)**:
```json
{
    "status": "success",
    "message": "All sessions revoked successfully"
}
```

---

## Health and Status Endpoints

### 21. Health Check

#### GET `/health`

**Description**: Get service health status

**Headers**:
```http
GET /iam/api/v1/health HTTP/1.1
Host: localhost:8081
Accept: application/json
```

**cURL Example**:
```bash
curl -X GET http://localhost:8081/iam/api/v1/health \
  -H "Accept: application/json"
```

**Success Response (200)**:
```json
{
    "status": "UP",
    "components": {
        "db": {
            "status": "UP",
            "details": {
                "database": "PostgreSQL",
                "validationQuery": "isValid()"
            }
        },
        "redis": {
            "status": "UP",
            "details": {
                "version": "7.0.0"
            }
        },
        "diskSpace": {
            "status": "UP",
            "details": {
                "total": 250685575168,
                "free": 125342787584,
                "threshold": 10485760
            }
        }
    }
}
```

---

### 22. Application Info

#### GET `/info`

**Description**: Get application information

**Headers**:
```http
GET /iam/api/v1/info HTTP/1.1
Host: localhost:8081
Accept: application/json
```

**cURL Example**:
```bash
curl -X GET http://localhost:8081/iam/api/v1/info \
  -H "Accept: application/json"
```

**Success Response (200)**:
```json
{
    "application": {
        "name": "ISCM IAM Service",
        "version": "0.0.1-SNAPSHOT",
        "description": "Identity and Access Management Service",
        "spring": {
            "boot": {
                "version": "3.5.6"
            }
        }
    },
    "build": {
        "time": "2025-11-23T10:00:00.000Z",
        "artifact": "iam-service",
        "group": "com.iscm"
    }
}
```

---

## OAuth Integration Endpoints

### 23. Get OAuth URL

#### GET `/oauth/{provider}/url`

**Description**: Get OAuth authorization URL for external provider

**Headers**:
```http
GET /iam/api/v1/oauth/google/url?redirectUri=http://localhost:3000/callback HTTP/1.1
Host: localhost:8081
Accept: application/json
```

**Path Parameters**:
- `provider` (string, required) - OAuth provider (google, microsoft, linkedin)

**Query Parameters**:
- `redirectUri` (string, required) - Callback URL after authentication

**cURL Example**:
```bash
curl -X GET "http://localhost:8081/iam/api/v1/oauth/google/url?redirectUri=http://localhost:3000/callback" \
  -H "Accept: application/json"
```

**Success Response (200)**:
```json
{
    "status": "success",
    "data": {
        "authorizationUrl": "https://accounts.google.com/oauth/authorize?client_id=...&redirect_uri=...",
        "state": "state_token_here"
    }
}
```

---

### 24. OAuth Callback

#### GET `/oauth/{provider}/callback`

**Description**: Handle OAuth callback from external provider

**Headers**:
```http
GET /iam/api/v1/oauth/google/callback?code=auth_code_here&state=state_token_here HTTP/1.1
Host: localhost:8081
Accept: application/json
```

**Path Parameters**:
- `provider` (string, required) - OAuth provider

**Query Parameters**:
- `code` (string, required) - Authorization code from provider
- `state` (string, required) - State parameter for CSRF protection

**cURL Example**:
```bash
curl -X GET "http://localhost:8081/iam/api/v1/oauth/google/callback?code=auth_code_here&state=state_token_here" \
  -H "Accept: application/json"
```

**Success Response (200)**:
```json
{
    "status": "success",
    "message": "OAuth authentication successful",
    "data": {
        "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
        "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
        "user": {
            "id": "550e8400-e29b-41d4-a716-446655440000",
            "email": "user@gmail.com",
            "firstName": "John",
            "lastName": "Doe",
            "provider": "google",
            "providerId": "123456789012345678901"
        }
    }
}
```

---

## Testing Scenarios

### Complete User Registration and Login Flow

```bash
# 1. Register a new user
curl -X POST http://localhost:8081/iam/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "testuser@example.com",
    "password": "SecurePassword123!",
    "firstName": "Test",
    "lastName": "User"
  }'

# 2. Login with the registered user
LOGIN_RESPONSE=$(curl -s -X POST http://localhost:8081/iam/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "testuser@example.com",
    "password": "SecurePassword123!"
  }')

# Extract access token
ACCESS_TOKEN=$(echo $LOGIN_RESPONSE | jq -r '.data.accessToken')

# 3. Get user profile
curl -X GET http://localhost:8081/iam/api/v1/users/me \
  -H "Authorization: Bearer $ACCESS_TOKEN"

# 4. Update user profile
curl -X PUT http://localhost:8081/iam/api/v1/users/me \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "firstName": "Updated",
    "lastName": "Name"
  }'

# 5. Setup MFA
curl -X POST http://localhost:8081/iam/api/v1/mfa/setup \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "password": "SecurePassword123!"
  }'

# 6. Logout
curl -X POST http://localhost:8081/iam/api/v1/auth/logout \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "refreshToken": "refresh_token_here"
  }'
```

### Error Handling Test Scenarios

```bash
# Test invalid credentials
curl -X POST http://localhost:8081/iam/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "password": "wrongpassword"
  }'

# Test missing required fields
curl -X POST http://localhost:8081/iam/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "firstName": "John"
  }'

# Test unauthorized access
curl -X GET http://localhost:8081/iam/api/v1/users/me \
  -H "Authorization: Bearer invalid_token"

# Test rate limiting (make multiple requests quickly)
for i in {1..4}; do
  curl -X POST http://localhost:8081/iam/api/v1/auth/register \
    -H "Content-Type: application/json" \
    -d '{
      "email": "test$i@example.com",
      "password": "Password123!",
      "firstName": "Test",
      "lastName": "User"
    }'
done
```

### Performance Testing with Siege

```bash
# Load test login endpoint
siege -c 10 -r 100 -t 60s \
  "http://localhost:8081/iam/api/v1/auth/login POST \
  Content-Type: application/json \
  {'email': 'test@example.com', 'password': 'Password123!'}"

# Load test user profile endpoint
siege -c 20 -r 50 -t 30s \
  "http://localhost:8081/iam/api/v1/users/me GET \
  Authorization: Bearer <access_token>"
```

## Testing Tools

### 1. cURL Examples
All endpoints above include cURL examples for testing.

### 2. Postman Collection
Import the following JSON into Postman for a complete collection:

```json
{
  "info": {
    "name": "ISCM IAM API",
    "description": "Complete API testing collection for ISCM IAM Service",
    "version": "1.0.0"
  },
  "auth": {
    "type": "bearer",
    "bearer": [
      {
        "key": "token",
        "value": "{{accessToken}}",
        "type": "string"
      }
    ]
  },
  "variable": [
    {
      "key": "baseUrl",
      "value": "http://localhost:8081/iam/api/v1"
    },
    {
      "key": "accessToken",
      "value": ""
    }
  ]
}
```

### 3. JMeter Test Plan
Create a JMeter test plan with the following structure:
- Thread Group: 100 users
- Ramp-up: 10 seconds
- Loop Count: 100
- HTTP Requests for all endpoints
- Response assertions for status codes

---

## Important Notes

### Security Considerations

1. **Always use HTTPS in production**
2. **Never log passwords or sensitive tokens**
3. **Implement proper input validation**
4. **Use secure password policies**
5. **Enable rate limiting on all authentication endpoints**

### Testing Best Practices

1. **Test both positive and negative scenarios**
2. **Verify rate limiting functionality**
3. **Test error handling and edge cases**
4. **Use proper authentication for protected endpoints**
5. **Clean up test data after testing**

### Rate Limiting Information

- **Registration**: 3 requests per hour per IP
- **Login**: 5 requests per 5 minutes per IP
- **Password Reset**: 3 requests per 15 minutes per IP
- **General API**: 100 requests per minute per IP

### Common Response Headers

All rate-limited responses include:
- `X-RateLimit-Limit`: Maximum requests allowed
- `X-RateLimit-Remaining`: Requests remaining in current window
- `X-RateLimit-Reset`: Seconds until reset
- `X-RateLimit-Retry-After`: Seconds to wait (when limited)

---

**Last Updated**: November 23, 2025
**Version**: 1.0
**Author**: ISCM Development Team