# ISCM Identity & Access Management Service

A comprehensive Identity and Access Management (IAM) service for the ISCM platform, providing secure user authentication, role-based access control, and management capabilities.

## 🚀 Features

- **JWT-based Authentication**: Secure token-based authentication
- **Role-Based Access Control (RBAC)**: Fine-grained permission management
- **Rate Limiting**: Protection against brute force attacks
- **Account Lockout**: Automatic lock after failed login attempts
- **User Management**: Complete CRUD operations for users
- **Organization Management**: Multi-tenant support
- **Email Notifications**: Integrated email service
- **Monitoring**: Prometheus metrics and Grafana dashboards
- **Health Checks**: Comprehensive service monitoring
- **API Documentation**: Swagger/OpenAPI integration

## 📋 Prerequisites

- **Docker** and **Docker Compose**
- **Java 17** (for local development)
- **Maven 3.8+** (for local development)
- **PostgreSQL** (included in Docker setup)
- **Redis** (included in Docker setup)

## 🛠️ Setup Instructions

### Option 1: Docker Compose (Recommended)

#### Development Environment

```bash
# Start all services in development mode
docker-compose -f docker-compose.dev.yml up -d

# View logs
docker-compose -f docker-compose.dev.yml logs -f

# Stop all services
docker-compose -f docker-compose.dev.yml down
```

#### Production Environment

```bash
# Create environment file
cat > .env << EOF
DB_PASSWORD=your_secure_database_password
REDIS_PASSWORD=your_secure_redis_password
JWT_SECRET=your_super_secure_jwt_secret_key_at_least_32_characters
DOMAIN=your-domain.com
EOF

# Start all services in production mode
docker-compose -f docker-compose.prod.yml --env-file .env up -d

# View logs
docker-compose -f docker-compose.prod.yml logs -f

# Stop all services
docker-compose -f docker-compose.prod.yml down
```

### Option 2: Local Development Setup

#### 1. Start Dependencies

```bash
# Start PostgreSQL
docker run -d \
  --name iscm-postgres \
  -p 5432:5432 \
  -e POSTGRES_DB=iscm_iam \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=password \
  postgres:15-alpine

# Start Redis
docker run -d \
  --name iscm-redis \
  -p 6379:6379 \
  redis:7-alpine

# Start MailHog (for email testing)
docker run -d \
  --name iscm-mailhog \
  -p 1025:1025 \
  -p 8025:8025 \
  mailhog/mailhog:latest
```

#### 2. Build and Run the Application

```bash
# Navigate to service directory
cd iam-service

# Build the application
mvn clean package

# Run the application
mvn spring-boot:run

# Or run the JAR directly
java -jar target/iam-service-0.0.1-SNAPSHOT.jar
```

## 🔌 Service URLs

After starting the services, you can access:

- **IAM API**: http://localhost:8081/iam
- **Swagger UI**: http://localhost:8081/iam/swagger-ui.html
- **MailHog** (Email Testing): http://localhost:8025
- **Prometheus** (Monitoring): http://localhost:9090
- **Grafana** (Dashboards): http://localhost:3000 (admin/admin)
- **Health Check**: http://localhost:8081/iam/actuator/health

## 🧪 Postman Testing Guide

### 1. Import Postman Collection

Download and import the Postman collection [here](#) or create a new collection with the following settings:

**Collection Variables:**
```
baseUrl = http://localhost:8081/iam
accessToken = {{accessToken}}
refreshToken = {{refreshToken}}
userId = {{userId}}
```

### 2. Test Authentication Endpoints

#### A. User Registration

**Request:**
```http
POST {{baseUrl}}/api/v1/auth/register
Content-Type: application/json

{
  "email": "test.user@example.com",
  "password": "SecurePass123!",
  "firstName": "Test",
  "lastName": "User",
  "phone": "+1234567890",
  "organizationId": "123e4567-e89b-12d3-a456-426614174000"
}
```

**Expected Response (200 OK):**
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "tokenType": "Bearer",
  "expiresIn": 900,
  "user": {
    "id": "123e4567-e89b-12d3-a456-426614174000",
    "email": "test.user@example.com",
    "firstName": "Test",
    "lastName": "User",
    "fullName": "Test User",
    "roles": ["USER"],
    "tenantId": "123e4567-e89b-12d3-a456-426614174000"
  }
}
```

**Postman Test Script:**
```javascript
pm.test("Status code is 200", function () {
    pm.response.to.have.status(200);
});

pm.test("Response has access token", function () {
    const jsonData = pm.response.json();
    pm.expect(jsonData.accessToken).to.exist;
    pm.globals.set("accessToken", jsonData.accessToken);
    pm.globals.set("refreshToken", jsonData.refreshToken);
    pm.globals.set("userId", jsonData.user.id);
});

pm.test("User has USER role", function () {
    const jsonData = pm.response.json();
    pm.expect(jsonData.user.roles).to.include("USER");
});
```

#### B. User Login

**Request:**
```http
POST {{baseUrl}}/api/v1/auth/login
Content-Type: application/json

{
  "email": "test.user@example.com",
  "password": "SecurePass123!"
}
```

**Expected Response (200 OK):**
Same as registration response

**Postman Test Script:**
```javascript
pm.test("Status code is 200", function () {
    pm.response.to.have.status(200);
});

pm.test("Login successful", function () {
    const jsonData = pm.response.json();
    pm.expect(jsonData.accessToken).to.exist;
    pm.globals.set("accessToken", jsonData.accessToken);
});
```

#### C. Refresh Access Token

**Request:**
```http
POST {{baseUrl}}/api/v1/auth/refresh
Content-Type: application/json

{
  "refreshToken": "{{refreshToken}}"
}
```

**Expected Response (200 OK):**
Same as login response

#### D. User Logout

**Request:**
```http
POST {{baseUrl}}/api/v1/auth/logout
Content-Type: application/json
Authorization: Bearer {{accessToken}}

{
  "refreshToken": "{{refreshToken}}"
}
```

**Expected Response (200 OK):**
Empty response

### 3. Test Protected Endpoints

#### A. Get Current User Profile

**Request:**
```http
GET {{baseUrl}}/api/v1/users/me
Authorization: Bearer {{accessToken}}
```

**Expected Response (200 OK):**
```json
{
  "id": "123e4567-e89b-12d3-a456-426614174000",
  "email": "test.user@example.com",
  "firstName": "Test",
  "lastName": "User",
  "fullName": "Test User",
  "phone": "+1234567890",
  "isActive": true,
  "authProvider": "local",
  "lastLoginAt": "2023-12-07T10:30:00Z",
  "failedLoginAttempts": 0,
  "accountLockedUntil": null,
  "roles": [
    {
      "id": "123e4567-e89b-12d3-a456-426614174000",
      "name": "USER",
      "description": "Standard user role"
    }
  ],
  "organization": {
    "id": "123e4567-e89b-12d3-a456-426614174000",
    "name": "Example Corp",
    "tenantId": "123e4567-e89b-12d3-a456-426614174000"
  },
  "createdAt": "2023-12-07T10:30:00Z",
  "updatedAt": "2023-12-07T10:30:00Z"
}
```

**Postman Test Script:**
```javascript
pm.test("Status code is 200", function () {
    pm.response.to.have.status(200);
});

pm.test("User profile retrieved", function () {
    const jsonData = pm.response.json();
    pm.expect(jsonData.email).to.eql("test.user@example.com");
    pm.expect(jsonData.firstName).to.eql("Test");
});
```

#### B. Get User by ID

**Request:**
```http
GET {{baseUrl}}/api/v1/users/{{userId}}
Authorization: Bearer {{accessToken}}
```

**Expected Response (200 OK):**
Same as current user profile response

#### C. Update User

**Request:**
```http
PUT {{baseUrl}}/api/v1/users/{{userId}}
Content-Type: application/json
Authorization: Bearer {{accessToken}}

{
  "firstName": "Updated",
  "lastName": "User",
  "phone": "+19876543210",
  "isActive": true
}
```

**Expected Response (200 OK):**
Updated user object

**Postman Test Script:**
```javascript
pm.test("Status code is 200", function () {
    pm.response.to.have.status(200);
});

pm.test("User updated successfully", function () {
    const jsonData = pm.response.json();
    pm.expect(jsonData.firstName).to.eql("Updated");
});
```

#### D. Delete User (Admin Only)

**Request:**
```http
DELETE {{baseUrl}}/api/v1/users/{{userId}}
Authorization: Bearer {{accessToken}}
```

**Expected Response (200 OK):**
Empty response

### 4. Test Error Scenarios

#### A. Invalid Login

**Request:**
```http
POST {{baseUrl}}/api/v1/auth/login
Content-Type: application/json

{
  "email": "test.user@example.com",
  "password": "wrongpassword"
}
```

**Expected Response (401 Unauthorized):**
```json
{
  "status": 401,
  "error": "Unauthorized",
  "message": "Invalid credentials",
  "path": "/api/v1/auth/login",
  "timestamp": "2023-12-07T10:30:00Z"
}
```

#### B. Duplicate Registration

**Request:**
```http
POST {{baseUrl}}/api/v1/auth/register
Content-Type: application/json

{
  "email": "test.user@example.com",
  "password": "SecurePass123!",
  "firstName": "Test",
  "lastName": "User",
  "phone": "+1234567890",
  "organizationId": "123e4567-e89b-12d3-a456-426614174000"
}
```

**Expected Response (409 Conflict):**
```json
{
  "status": 409,
  "error": "Conflict",
  "message": "Email already exists",
  "path": "/api/v1/auth/register",
  "timestamp": "2023-12-07T10:30:00Z"
}
```

#### C. Access Without Token

**Request:**
```http
GET {{baseUrl}}/api/v1/users/me
```

**Expected Response (401 Unauthorized):**
```json
{
  "status": 401,
  "error": "Unauthorized",
  "message": "Full authentication is required to access this resource",
  "path": "/api/v1/users/me",
  "timestamp": "2023-12-07T10:30:00Z"
}
```

### 5. Test Health Endpoints

#### A. Health Check

**Request:**
```http
GET {{baseUrl}}/actuator/health
```

**Expected Response (200 OK):**
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
    "diskSpace": {
      "status": "UP",
      "details": {
        "total": 100000000000,
        "free": 50000000000,
        "threshold": 10485760
      }
    }
  }
}
```

### 6. Rate Limiting Tests

To test rate limiting, make multiple requests to authentication endpoints quickly:

```javascript
// In Postman pre-request script
for (let i = 0; i < 15; i++) {
    pm.sendRequest({
        url: pm.environment.get("baseUrl") + "/api/v1/auth/login",
        method: "POST",
        header: "Content-Type: application/json",
        body: {
            mode: "raw",
            raw: JSON.stringify({
                email: "test.user@example.com",
                password: "SecurePass123!"
            })
        }
    }, function (err, response) {
        console.log(`Request ${i + 1}: ${response.code}`);
    });
}
```

**Expected Response (429 Too Many Requests):**
```json
{
  "status": 429,
  "error": "Too Many Requests",
  "message": "Rate limit exceeded",
  "path": "/api/v1/auth/login",
  "timestamp": "2023-12-07T10:30:00Z"
}
```

## 📊 API Documentation

### Base URL
- Development: `http://localhost:8081/iam`
- Production: `https://iam.iscm.com`

### Authentication
Include the access token in the `Authorization` header:
```
Authorization: Bearer <your-access-token>
```

### Endpoints

| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| POST | `/api/v1/auth/register` | User registration | No |
| POST | `/api/v1/auth/login` | User login | No |
| POST | `/api/v1/auth/refresh` | Refresh access token | No |
| POST | `/api/v1/auth/logout` | User logout | Yes |
| GET | `/api/v1/users/me` | Get current user | Yes |
| GET | `/api/v1/users/{id}` | Get user by ID | Yes |
| PUT | `/api/v1/users/{id}` | Update user | Yes |
| DELETE | `/api/v1/users/{id}` | Delete user | Yes |
| GET | `/actuator/health` | Health check | No |
| GET | `/actuator/info` | Application info | No |

### Rate Limiting
- **Authentication endpoints**: 10 requests per minute
- **User management endpoints**: 100 requests per minute
- **Admin endpoints**: 50 requests per minute

## 🔧 Configuration

### Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `DB_USERNAME` | Database username | `postgres` |
| `DB_PASSWORD` | Database password | `Saran@2002` |
| `REDIS_HOST` | Redis host | `localhost` |
| `REDIS_PORT` | Redis port | `6379` |
| `JWT_SECRET` | JWT secret key | `mySuperSecretKey...` |
| `MAIL_HOST` | SMTP host | `localhost` |
| `MAIL_PORT` | SMTP port | `1025` |

### Application Properties

The main configuration is in `iam-service/src/main/resources/application.properties`.

## 📈 Monitoring

### Prometheus Metrics
- URL: http://localhost:9090
- Metrics available at: `/actuator/prometheus`

### Grafana Dashboards
- URL: http://localhost:3000
- Username: `admin`
- Password: `admin`

### Key Metrics
- `http_requests_total` - HTTP request count
- `http_request_duration_seconds` - Request duration
- `system_cpu_usage` - CPU usage
- `system_memory_usage` - Memory usage
- `jvm_memory_bytes_used` - JVM memory usage

## 🚨 Troubleshooting

### Common Issues

#### 1. Database Connection Issues
```bash
# Check PostgreSQL container
docker ps | grep postgres
docker logs iscm-postgres

# Test database connection
docker exec -it iscm-postgres psql -U postgres -d iscm_iam -c "SELECT 1"
```

#### 2. Redis Connection Issues
```bash
# Check Redis container
docker ps | grep redis
docker logs iscm-redis

# Test Redis connection
docker exec -it iscm-redis redis-cli ping
```

#### 3. Port Conflicts
```bash
# Check port usage
netstat -tulpn | grep :5432
netstat -tulpn | grep :6379
netstat -tulpn | grep :8081
```

#### 4. JWT Token Issues
- Verify JWT secret is at least 32 characters
- Check token expiration time
- Ensure proper Authorization header format

#### 5. Rate Limiting
- Check Redis connection for rate limiting
- Verify rate limit configuration
- Monitor rate limit headers in responses

### Log Files

Application logs are stored in:
- Docker: `./logs/` directory
- Local: Console output and application logs

### Health Checks

Monitor service health:
```bash
# Health check
curl http://localhost:8081/iam/actuator/health

# Application info
curl http://localhost:8081/iam/actuator/info

# Metrics
curl http://localhost:8081/iam/actuator/metrics
```

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests for new functionality
5. Run the test suite
6. Submit a pull request

## 📄 License

This project is licensed under the MIT License - see the LICENSE file for details.

## 🆘 Support

For support and questions:
- Create an issue in the GitHub repository
- Check the troubleshooting section above
- Review the API documentation
- Monitor service health and metrics

---

**Note**: Always ensure proper security measures are in place when deploying to production environments, including strong passwords, secure JWT secrets, and proper network security configurations.