# ISCM Services - Development Environment

A simplified Identity and Access Management (IAM) service and Gateway for development purposes.

## Services

### IAM Service (Port 8081)
- **Context Path**: `/iam`
- **Main Features**: User authentication, JWT tokens, role-based access control
- **API Documentation**: http://localhost:8081/iam/swagger-ui.html
- **Health Check**: http://localhost:8081/iam/actuator/health

### Gateway Service (Port 7070)
- **Features**: Request routing, circuit breaker, rate limiting
- **Health Check**: http://localhost:7070/actuator/health

## Prerequisites

1. **Java 21** - Required for Spring Boot 3.x
2. **PostgreSQL** - Database for IAM service
3. **Redis** - Caching and session storage

## Database Setup

### PostgreSQL
```sql
CREATE DATABASE iscm_iam;
CREATE USER postgres WITH PASSWORD 'Saran@2002';
GRANT ALL PRIVILEGES ON DATABASE iscm_iam TO postgres;
```

### Enable Liquibase for database migration:
1. Run the IAM service once with Liquibase enabled
2. Update `application.properties` to set `spring.liquibase.enabled=true`
3. Start the service to create all tables
4. Set `spring.liquibase.enabled=false` for normal operation

## Redis Setup

### Docker Command
```bash
docker run -d --name iscm-redis -p 6379:6379 redis:7-alpine
```

### Or Install Locally
- Install Redis server
- Start Redis on default port 6379

## Running the Services

### Option 1: Using Maven
```bash
# Navigate to IAM service
cd iam-service
mvn spring-boot:run

# Navigate to Gateway (in another terminal)
cd ../gateway
mvn spring-boot:run
```

### Option 2: Using Docker Compose
```bash
docker-compose -f docker-compose.dev.yml up -d
```

## Configuration

### IAM Service Configuration (`iam-service/src/main/resources/application.properties`)
- **Database**: PostgreSQL connection settings
- **Redis**: Cache and session storage
- **JWT**: Token configuration
- **Security**: Rate limiting and authentication settings

### Gateway Configuration
- Routes requests to IAM service
- Implements circuit breaker pattern
- Provides rate limiting

## API Endpoints

### Authentication
- `POST /iam/api/v1/auth/register` - Register new user
- `POST /iam/api/v1/auth/login` - User login
- `POST /iam/api/v1/auth/refresh` - Refresh token
- `POST /iam/api/v1/auth/logout` - User logout

### User Management
- `GET /iam/api/v1/users/me` - Get current user profile
- `PUT /iam/api/v1/users/me` - Update user profile
- `GET /iam/api/v1/users/{id}` - Get user by ID

### Admin Operations
- `GET /iam/api/v1/admin/users` - List all users (admin only)
- `POST /iam/api/v1/admin/users` - Create user (admin only)
- `GET /iam/api/v1/admin/users/statistics` - User statistics

### Security
- `POST /iam/api/v1/mfa/setup/totp` - Setup MFA
- `POST /iam/api/v1/mfa/enable` - Enable MFA
- `POST /iam/api/v1/mfa/disable` - Disable MFA

## Testing

### Health Checks
```bash
# IAM Service Health
curl http://localhost:8081/iam/actuator/health

# Gateway Health
curl http://localhost:7070/actuator/health
```

### Sample API Calls
```bash
# Register User
curl -X POST http://localhost:8081/iam/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "password": "TestPass123!",
    "firstName": "Test",
    "lastName": "User"
  }'

# Login
curl -X POST http://localhost:8081/iam/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "password": "TestPass123!"
  }'
```

## Features

### Security
- JWT-based authentication
- Role-based access control (RBAC)
- Rate limiting
- Account lockout protection
- Multi-factor authentication (MFA)

### Caching
- Redis-based caching for user data
- Session management
- JWT token blacklisting

### Monitoring
- Actuator health endpoints
- Application metrics
- Security event logging

## Development Notes

### Database Schema
- Uses Liquibase for database migrations
- Located in `src/main/resources/db/changelog/`
- Master changelog: `db.changelog.master.yaml`

### Security Configuration
- JWT secrets are configured in `application.properties`
- For production, use environment variables
- Default JWT expiration: 15 minutes (access), 7 days (refresh)

### Caching Strategy
- User sessions cached in Redis
- JWT validation results cached
- Cache TTL: 30 minutes default

## Troubleshooting

### Common Issues

1. **Database Connection Error**
   - Ensure PostgreSQL is running
   - Check database credentials in `application.properties`
   - Verify database exists

2. **Redis Connection Error**
   - Ensure Redis is running on port 6379
   - Check Redis configuration
   - Verify Redis is accessible

3. **JWT Token Issues**
   - Check JWT secrets are configured
   - Verify token expiration settings
   - Ensure proper token format in requests

4. **Port Conflicts**
   - IAM service uses port 8081
   - Gateway uses port 7070
   - Ensure ports are available

### Logs
- Application logs: Console output
- Database queries: Set `logging.level.org.hibernate.SQL=DEBUG`
- Security events: Set `logging.level.com.iscm=DEBUG`

## File Structure

```
iscm-services/
├── iam-service/                 # IAM Service
│   ├── src/main/java/
│   │   └── com/iscm/iam/
│   │       ├── config/         # Configuration classes
│   │       ├── controller/     # REST controllers
│   │       ├── dto/           # Data transfer objects
│   │       ├── model/         # JPA entities
│   │       ├── repository/    # Data repositories
│   │       ├── security/      # Security components
│   │       └── service/       # Business logic
│   └── src/main/resources/
│       ├── application.properties
│       └── db/changelog/       # Database migrations
├── gateway/                     # Gateway Service
│   └── src/main/java/
│       └── com/gateway/
│           ├── config/
│           ├── controller/
│           ├── filter/
│           └── service/
├── docker-compose.dev.yml       # Docker development setup
└── README-DEV.md               # This file
```

## Next Steps

1. Set up PostgreSQL and Redis
2. Run database migrations using Liquibase
3. Start both services
4. Test API endpoints using Swagger UI or curl
5. Customize configuration as needed

For detailed API documentation, see the Swagger UI at http://localhost:8081/iam/swagger-ui.html