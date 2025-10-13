# IAM Service API Documentation

## API Testing Sequence

This document provides comprehensive API testing documentation for the IAM service, organized by role-based access control. The testing should proceed in the following order:

### Testing Priority Order:
1. **Public Endpoints** (No authentication required)
2. **User Endpoints** (Basic authenticated users)
3. **Admin Endpoints** (USER_READ/USER_WRITE permissions)
4. **Super Admin Endpoints** (SUPER_ADMIN role)

---

## 1. Authentication Endpoints

### 1.1 User Registration
**Endpoint:** `POST /api/v1/auth/register`
**Access Level:** Public
**Description:** Creates a new user account

**Request Body:**
```json
{
  "firstName": "John",
  "lastName": "Doe",
  "email": "john.doe@example.com",
  "password": "SecurePass123!",
  "confirmPassword": "SecurePass123!",
  "phone": "+1234567890"
}
```

**Expected Response (200 OK):**
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "tokenType": "Bearer",
  "expiresIn": 3600,
  "user": {
    "id": "uuid-here",
    "email": "john.doe@example.com",
    "firstName": "John",
    "lastName": "Doe",
    "roles": ["USER"],
    "mfaEnabled": false,
    "active": true
  }
}
```

**Test Cases:**
- ✅ Valid registration with strong password
- ❌ Weak password (should fail validation)
- ❌ Duplicate email (should return 409)
- ❌ Password mismatch (should fail validation)

---

### 1.2 User Login
**Endpoint:** `POST /api/v1/auth/login`
**Access Level:** Public
**Description:** Authenticates user credentials

**Request Body:**
```json
{
  "email": "john.doe@example.com",
  "password": "SecurePass123!"
}
```

**Headers:**
```
User-Agent: Mozilla/5.0 (Windows NT 10.0; Win64; x64)...
X-Forwarded-For: 192.168.1.100
```

**Expected Response (200 OK):**
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "tokenType": "Bearer",
  "expiresIn": 3600,
  "user": {
    "id": "uuid-here",
    "email": "john.doe@example.com",
    "firstName": "John",
    "lastName": "Doe",
    "roles": ["USER"],
    "mfaEnabled": false,
    "active": true
  },
  "requiresMfa": false
}
```

**Test Cases:**
- ✅ Valid credentials
- ❌ Invalid password (should return 401)
- ❌ Non-existent email (should return 401)
- ❌ Locked account (should return 423)

---

### 1.3 Token Refresh
**Endpoint:** `POST /api/v1/auth/refresh`
**Access Level:** Public
**Description:** Refreshes access token using refresh token

**Request Body:**
```json
{
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

**Expected Response (200 OK):**
```json
{
  "accessToken": "new-access-token-here",
  "refreshToken": "new-refresh-token-here",
  "tokenType": "Bearer",
  "expiresIn": 3600
}
```

**Test Cases:**
- ✅ Valid refresh token
- ❌ Expired refresh token (should return 401)
- ❌ Invalid refresh token (should return 401)

---

### 1.4 User Logout
**Endpoint:** `POST /api/v1/auth/logout`
**Access Level:** Public
**Description:** Invalidates refresh token

**Request Body:**
```json
{
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

**Expected Response (200 OK):**
```json
{}
```

---

## 2. User Management Endpoints

### 2.1 Get Current User Profile
**Endpoint:** `GET /api/v1/users/me`
**Access Level:** Any authenticated user
**Description:** Returns current user's profile information

**Headers:**
```
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

**Expected Response (200 OK):**
```json
{
  "id": "uuid-here",
  "email": "john.doe@example.com",
  "firstName": "John",
  "lastName": "Doe",
  "phone": "+1234567890",
  "roles": ["USER"],
  "mfaEnabled": false,
  "active": true,
  "createdAt": "2024-01-01T00:00:00Z",
  "updatedAt": "2024-01-01T00:00:00Z"
}
```

**Test Cases:**
- ✅ Valid access token
- ❌ Invalid access token (should return 401)
- ❌ Expired access token (should return 401)

---

### 2.2 Get User by ID
**Endpoint:** `GET /api/v1/users/{userId}`
**Access Level:** Users with USER_READ authority
**Description:** Returns user information by ID

**Headers:**
```
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

**Path Parameters:**
- `userId`: UUID of the user to retrieve

**Expected Response (200 OK):**
```json
{
  "id": "uuid-here",
  "email": "john.doe@example.com",
  "firstName": "John",
  "lastName": "Doe",
  "phone": "+1234567890",
  "roles": ["USER"],
  "mfaEnabled": false,
  "active": true,
  "createdAt": "2024-01-01T00:00:00Z",
  "updatedAt": "2024-01-01T00:00:00Z"
}
```

**Test Cases:**
- ✅ Admin user accessing another user's profile
- ❌ Regular user without USER_READ permission (should return 403)
- ❌ Non-existent user ID (should return 404)

---

### 2.3 Update User
**Endpoint:** `PUT /api/v1/users/{userId}`
**Access Level:** Users with USER_WRITE authority
**Description:** Updates user information

**Headers:**
```
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

**Request Body:**
```json
{
  "firstName": "John",
  "lastName": "Smith",
  "phone": "+1234567890"
}
```

**Expected Response (200 OK):**
```json
{
  "id": "uuid-here",
  "email": "john.doe@example.com",
  "firstName": "John",
  "lastName": "Smith",
  "phone": "+1234567890",
  "roles": ["USER"],
  "mfaEnabled": false,
  "active": true,
  "createdAt": "2024-01-01T00:00:00Z",
  "updatedAt": "2024-01-02T00:00:00Z"
}
```

**Test Cases:**
- ✅ Admin user updating another user
- ❌ Regular user without USER_WRITE permission (should return 403)
- ❌ Invalid user data (should return 400)

---

### 2.4 Delete User
**Endpoint:** `DELETE /api/v1/users/{userId}`
**Access Level:** Users with SUPER_ADMIN role
**Description:** Deletes a user account

**Headers:**
```
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

**Path Parameters:**
- `userId`: UUID of the user to delete

**Expected Response (200 OK):**
```json
{}
```

**Test Cases:**
- ✅ Super admin deleting another user
- ❌ Admin without SUPER_ADMIN role (should return 403)
- ❌ Regular user (should return 403)

---

## 3. Multi-Factor Authentication (MFA) Endpoints

### 3.1 Setup TOTP MFA
**Endpoint:** `POST /api/v1/mfa/setup/totp`
**Access Level:** Authenticated user
**Description:** Generates TOTP secret and QR code for MFA setup

**Headers:**
```
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

**Expected Response (200 OK):**
```json
{
  "secret": "JBSWY3DPEHPK3PXP",
  "qrCodeUrl": "otpauth://totp/ISCAM:john.doe@example.com?secret=JBSWY3DPEHPK3PXP&issuer=ISCAM"
}
```

**Test Cases:**
- ✅ Authenticated user setting up MFA
- ❌ Unauthenticated request (should return 401)

---

### 3.2 Enable MFA
**Endpoint:** `POST /api/v1/mfa/enable`
**Access Level:** Authenticated user
**Description:** Enables MFA for the user account

**Headers:**
```
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

**Request Body:**
```json
{
  "verificationCode": "123456",
  "mfaType": "TOTP"
}
```

**Expected Response (200 OK):**
```json
{}
```

**Test Cases:**
- ✅ Valid TOTP code
- ❌ Invalid TOTP code (should return 400)
- ❌ Unauthenticated request (should return 401)

---

### 3.3 Verify MFA Code
**Endpoint:** `POST /api/v1/mfa/verify`
**Access Level:** Authenticated user
**Description:** Verifies MFA code during login

**Headers:**
```
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

**Request Body:**
```json
{
  "code": "123456",
  "codeType": "TOTP"
}
```

**Expected Response (200 OK):**
```json
true
```

**Test Cases:**
- ✅ Valid TOTP code
- ✅ Valid backup code
- ❌ Invalid code (should return 400 with false)

---

### 3.4 Disable MFA
**Endpoint:** `POST /api/v1/mfa/disable`
**Access Level:** Authenticated user
**Description:** Disables MFA for the user account

**Headers:**
```
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

**Expected Response (200 OK):**
```json
{}
```

---

### 3.5 Send SMS Verification Code
**Endpoint:** `POST /api/v1/mfa/send-sms`
**Access Level:** Authenticated user
**Description:** Sends verification code via SMS

**Headers:**
```
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

**Expected Response (200 OK):**
```json
{}
```

---

### 3.6 Send Email Verification Code
**Endpoint:** `POST /api/v1/mfa/send-email`
**Access Level:** Authenticated user
**Description:** Sends verification code via email

**Headers:**
```
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

**Expected Response (200 OK):**
```json
{}
```

---

## 4. Password Reset Endpoints

### 4.1 Initiate Password Reset
**Endpoint:** `POST /api/v1/password-reset/initiate`
**Access Level:** Public
**Description:** Sends password reset link to user's email

**Request Body:**
```json
{
  "email": "john.doe@example.com"
}
```

**Expected Response (200 OK):**
```json
{}
```

**Note:** Always returns 200 to prevent email enumeration

---

### 4.2 Reset Password
**Endpoint:** `POST /api/v1/password-reset/reset`
**Access Level:** Public
**Description:** Resets user's password using valid token

**Request Body:**
```json
{
  "token": "reset-token-here",
  "newPassword": "NewSecurePass123!",
  "confirmPassword": "NewSecurePass123!"
}
```

**Expected Response (200 OK):**
```json
{}
```

---

### 4.3 Validate Reset Token
**Endpoint:** `POST /api/v1/password-reset/validate-token`
**Access Level:** Public
**Description:** Validates password reset token

**Request Body:**
```json
{
  "token": "reset-token-here"
}
```

**Expected Response (200 OK):**
```json
true
```

---

## 5. OAuth Authentication Endpoints

### 5.1 Get OAuth Authorization URL
**Endpoint:** `GET /api/v1/oauth/{provider}/url`
**Access Level:** Public
**Description:** Returns OAuth authorization URL

**Query Parameters:**
- `redirectUri`: Redirect URI after authentication

**Path Parameters:**
- `provider`: OAuth provider (google, microsoft, linkedin)

**Expected Response (200 OK):**
```json
{
  "oauthUrl": "https://accounts.google.com/oauth/authorize?client_id=...&redirect_uri=..."
}
```

---

### 5.2 Handle OAuth Callback
**Endpoint:** `POST /api/v1/oauth/{provider}/callback`
**Access Level:** Public
**Description:** Processes OAuth callback and authenticates user

**Request Body:**
```json
{
  "code": "authorization-code-here",
  "redirectUri": "https://your-app.com/callback"
}
```

**Expected Response (200 OK):**
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "tokenType": "Bearer",
  "expiresIn": 3600,
  "user": {
    "id": "uuid-here",
    "email": "john.doe@example.com",
    "firstName": "John",
    "lastName": "Doe",
    "roles": ["USER"],
    "mfaEnabled": false,
    "active": true
  }
}
```

---

### 5.3 Unlink OAuth Account
**Endpoint:** `DELETE /api/v1/oauth/{provider}/unlink`
**Access Level:** Authenticated user
**Description:** Removes OAuth account linkage

**Headers:**
```
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

**Query Parameters:**
- `userId`: UUID of the user

**Path Parameters:**
- `provider`: OAuth provider (google, microsoft, linkedin)

**Expected Response (200 OK):**
```json
{}
```

---

### 5.4 Get User OAuth Accounts
**Endpoint:** `GET /api/v1/oauth/accounts`
**Access Level:** Authenticated user
**Description:** Returns all OAuth accounts linked to the user

**Headers:**
```
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

**Query Parameters:**
- `userId`: UUID of the user

**Expected Response (200 OK):**
```json
[
  {
    "id": "uuid-here",
    "provider": "google",
    "providerUserId": "google-user-id",
    "email": "john.doe@example.com",
    "linkedAt": "2024-01-01T00:00:00Z"
  }
]
```

---

## Testing Matrix by Role

### Public Access (No Authentication)
- ✅ POST /api/v1/auth/register
- ✅ POST /api/v1/auth/login
- ✅ POST /api/v1/auth/refresh
- ✅ POST /api/v1/auth/logout
- ✅ POST /api/v1/password-reset/initiate
- ✅ POST /api/v1/password-reset/reset
- ✅ POST /api/v1/password-reset/validate-token
- ✅ GET /api/v1/oauth/{provider}/url
- ✅ POST /api/v1/oauth/{provider}/callback

### Basic User (USER role)
- ✅ GET /api/v1/users/me
- ✅ POST /api/v1/mfa/setup/totp
- ✅ POST /api/v1/mfa/enable
- ✅ POST /api/v1/mfa/verify
- ✅ POST /api/v1/mfa/disable
- ✅ POST /api/v1/mfa/send-sms
- ✅ POST /api/v1/mfa/send-email
- ✅ DELETE /api/v1/oauth/{provider}/unlink
- ✅ GET /api/v1/oauth/accounts

### Admin (USER_READ authority)
- ✅ GET /api/v1/users/{userId}
- ❌ PUT /api/v1/users/{userId} (requires USER_WRITE)
- ❌ DELETE /api/v1/users/{userId} (requires SUPER_ADMIN)

### Admin (USER_WRITE authority)
- ✅ GET /api/v1/users/{userId}
- ✅ PUT /api/v1/users/{userId}
- ❌ DELETE /api/v1/users/{userId} (requires SUPER_ADMIN)

### Super Admin (SUPER_ADMIN role)
- ✅ GET /api/v1/users/{userId}
- ✅ PUT /api/v1/users/{userId}
- ✅ DELETE /api/v1/users/{userId}

---

## Security Headers Required

For all authenticated endpoints, include the following headers:

```
Authorization: Bearer <access-token>
Content-Type: application/json
User-Agent: Your-Application/1.0
X-Forwarded-For: client-ip-address
```

## Error Response Format

All endpoints return standardized error responses:

```json
{
  "timestamp": "2024-01-01T00:00:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "path": "/api/v1/auth/register",
  "errors": [
    {
      "field": "password",
      "message": "Password must be at least 8 characters long"
    }
  ]
}
```

## Rate Limiting

The following endpoints have rate limiting:
- `/api/v1/auth/login` - 5 attempts per minute
- `/api/v1/auth/register` - 3 attempts per minute
- `/api/v1/password-reset/initiate` - 2 attempts per 5 minutes

Rate limit exceeded returns HTTP 429 with retry-after header.