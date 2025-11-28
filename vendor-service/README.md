# ISCM Vendor Service

## Overview
The Vendor Service is a Spring Boot microservice responsible for managing vendors, organizations, and warehouses in the Intelligent Supply Chain Management (ISCM) platform.

## Features
- ✅ Vendor onboarding and management
- ✅ Organization hierarchy management
- ✅ Warehouse tracking and geolocation
- ✅ KYC workflow for vendor verification
- ✅ Document management with S3 storage
- ✅ Event-driven architecture with Kafka
- ✅ JWT-based authentication & RBAC
- ✅ Multi-tenancy support
- ✅ RESTful APIs with OpenAPI documentation
- ✅ Database migrations with Flyway
- ✅ Metrics and monitoring with Prometheus

## Tech Stack
- **Framework**: Spring Boot 3.2
- **Language**: Java 21
- **Database**: PostgreSQL 16
- **Messaging**: Apache Kafka
- **Storage**: AWS S3 / MinIO
- **Security**: Spring Security + OAuth2 JWT
- **Documentation**: SpringDoc OpenAPI 3
- **Monitoring**: Actuator + Prometheus + Grafana

## Prerequisites
- Java 21+
- Maven 3.9+
- Docker & Docker Compose
- PostgreSQL 16+
- Kafka 3.5+

## Quick Start

### 1. Clone the Repository
```bash
git clone <repository-url>
cd vendor-service
```

### 2. Start Infrastructure with Docker Compose
```bash
docker-compose up -d postgres kafka zookeeper minio
```

### 3. Build the Application
```bash
mvn clean package
```

### 4. Run the Application
```bash
mvn spring-boot:run
```

Or run with Docker:
```bash
docker-compose up --build
```

### 5. Access the Application
- **API Base URL**: http://localhost:8081/api/v1
- **Swagger UI**: http://localhost:8081/swagger-ui.html
- **Health Check**: http://localhost:8081/actuator/health
- **Metrics**: http://localhost:8081/actuator/prometheus
- **MinIO Console**: http://localhost:9001 (minioadmin/minioadmin)

## API Endpoints

### Organizations
```
POST   /api/v1/organizations          - Create organization
GET    /api/v1/organizations          - List organizations
GET    /api/v1/organizations/{id}     - Get organization by ID
```

### Vendors
```
POST   /api/v1/vendors                - Create vendor
GET    /api/v1/vendors                - List vendors (paginated)
GET    /api/v1/vendors/{id}           - Get vendor by ID
GET    /api/v1/vendors/code/{code}    - Get vendor by code
GET    /api/v1/vendors/search         - Search vendors
PATCH  /api/v1/vendors/{id}           - Update vendor
PATCH  /api/v1/vendors/{id}/kyc-status - Update KYC status
DELETE /api/v1/vendors/{id}           - Delete vendor
```

### Warehouses
```
POST   /api/v1/warehouses             - Create warehouse
GET    /api/v1/warehouses             - List warehouses (paginated)
GET    /api/v1/warehouses/{id}        - Get warehouse by ID
GET    /api/v1/warehouses/vendor/{id} - Get warehouses by vendor
GET    /api/v1/warehouses/country/{code} - Get warehouses by country
PATCH  /api/v1/warehouses/{id}/status - Update warehouse status
DELETE /api/v1/warehouses/{id}        - Delete warehouse
```

## Authentication
All endpoints require JWT authentication. Include the JWT token in the Authorization header:
```
Authorization: Bearer <your-jwt-token>
```

### Required Roles
- **ADMIN**: Full access to all operations
- **OPS**: Manage vendors and warehouses
- **VENDOR**: View own vendor information
- **BUYER**: View vendor information

## Configuration

### Environment Variables
```bash
# Database
DATABASE_URL=jdbc:postgresql://localhost:5432/vendor_db
DATABASE_USER=postgres
DATABASE_PASSWORD=postgres

# Kafka
KAFKA_BOOTSTRAP_SERVERS=localhost:9092

# S3/MinIO
S3_ENDPOINT=http://localhost:9000
S3_ACCESS_KEY=minioadmin
S3_SECRET_KEY=minioadmin
S3_BUCKET=vendor-documents

# Security
JWT_JWK_SET_URI=http://localhost:8080/auth/.well-known/jwks.json

# Server
SERVER_PORT=8081
SPRING_PROFILES_ACTIVE=dev
```

## Events Published

### Kafka Topics
- **vendor.events**: VendorOnboarded, WarehouseCreated

### Event Schema
```json
{
  "eventId": "uuid",
  "eventType": "VendorOnboarded",
  "timestamp": "2025-01-01T00:00:00Z",
  "vendorId": "uuid",
  "vendorCode": "V001",
  "legalName": "Acme Corp",
  "organizationId": "uuid",
  "status": "ACTIVE",
  "tenantId": "uuid"
}
```

## Database Schema

### Tables
1. **organizations** - Company/tenant information
2. **vendors** - Supplier/vendor master data
3. **warehouses** - Storage facility locations
4. **vendor_documents** - KYC and compliance documents

### Migrations
Flyway migrations are located in `src/main/resources/db/migration/`

## Testing

### Run Unit Tests
```bash
mvn test
```

### Run Integration Tests
```bash
mvn verify
```

### Test with Testcontainers
Integration tests use Testcontainers for PostgreSQL and Kafka.

## Monitoring

### Actuator Endpoints
- `/actuator/health` - Health status
- `/actuator/info` - Application info
- `/actuator/metrics` - Application metrics
- `/actuator/prometheus` - Prometheus metrics

### Grafana Dashboard
Import the provided Grafana dashboard for monitoring:
- JVM metrics
- HTTP request metrics
- Database connection pool
- Kafka producer metrics

## Development

### Code Structure
```
src/main/java/com/iscm/vendor/
├── config/          # Configuration classes
├── controller/      # REST controllers
├── service/         # Business logic
├── repository/      # Data access layer
├── entity/          # JPA entities
├── dto/             # Request/Response DTOs
├── events/          # Kafka event publishers
├── exception/       # Exception handlers
└── mapper/          # MapStruct mappers
```

### Best Practices
- Use DTOs for API contracts
- Apply RBAC with `@PreAuthorize`
- Log all important operations
- Validate all inputs with Jakarta Validation
- Use transactional boundaries appropriately
- Publish events asynchronously
- Implement idempotency where needed

## Deployment

### Kubernetes
```bash
# Build and push image
docker build -t iscm/vendor-service:1.0.0 .
docker push iscm/vendor-service:1.0.0

# Deploy to k8s
kubectl apply -f k8s/
```

### Environment-specific Configs
- **dev**: application-dev.yml
- **prod**: application-prod.yml

## Troubleshooting

### Common Issues

1. **Connection refused to Kafka**
   - Ensure Kafka is running: `docker-compose ps`
   - Check KAFKA_BOOTSTRAP_SERVERS configuration

2. **Database migration failed**
   - Verify PostgreSQL is running
   - Check Flyway migration scripts
   - Run: `mvn flyway:repair`

3. **JWT validation error**
   - Verify JWT_JWK_SET_URI is correct
   - Check auth service is running
   - Validate token expiry

## Contributing
1. Create a feature branch
2. Write tests for new features
3. Follow code style guidelines
4. Submit pull request with description

## License
Proprietary - ISCM Platform

## Support
For issues and questions:
- Email: support@iscm.com
- Slack: #iscm-vendor-service

# Dockerfile
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline
COPY src ./src
RUN mvn clean package -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

EXPOSE 8081

HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8081/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]

---
# docker-compose.yml

version: '3.8'

services:
  postgres:
    image: postgres:16-alpine
    container_name: vendor-postgres
    environment:
      POSTGRES_DB: vendor_db
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: postgres
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U postgres"]
      interval: 10s
      timeout: 5s
      retries: 5

  kafka:
    image: confluentinc/cp-kafka:7.5.0
    container_name: vendor-kafka
    depends_on:
      - zookeeper
    ports:
      - "9092:9092"
    environment:
      KAFKA_BROKER_ID: 1
      KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://localhost:9092
      KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1
      KAFKA_AUTO_CREATE_TOPICS_ENABLE: "true"
    healthcheck:
      test: ["CMD", "kafka-broker-api-versions", "--bootstrap-server", "localhost:9092"]
      interval: 10s
      timeout: 5s
      retries: 5

  zookeeper:
    image: confluentinc/cp-zookeeper:7.5.0
    container_name: vendor-zookeeper
    ports:
      - "2181:2181"
    environment:
      ZOOKEEPER_CLIENT_PORT: 2181
      ZOOKEEPER_TICK_TIME: 2000

  minio:
    image: minio/minio:latest
    container_name: vendor-minio
    command: server /data --console-address ":9001"
    ports:
      - "9000:9000"
      - "9001:9001"
    environment:
      MINIO_ROOT_USER: minioadmin
      MINIO_ROOT_PASSWORD: minioadmin
    volumes:
      - minio_data:/data
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:9000/minio/health/live"]
      interval: 10s
      timeout: 5s
      retries: 5

  vendor-service:
    build: .
    container_name: vendor-service
    depends_on:
      postgres:
        condition: service_healthy
      kafka:
        condition: service_healthy
      minio:
        condition: service_healthy
    ports:
      - "8081:8081"
    environment:
      DATABASE_URL: jdbc:postgresql://postgres:5432/vendor_db
      DATABASE_USER: postgres
      DATABASE_PASSWORD: postgres
      KAFKA_BOOTSTRAP_SERVERS: kafka:9092
      MINIO_ENDPOINT: http://minio:9000
      MINIO_ACCESS_KEY: minioadmin
      MINIO_SECRET_KEY: minioadmin
      MINIO_BUCKET: vendor-documents
      JWT_JWK_SET_URI: http://gateway:8080/auth/.well-known/jwks.json
      SPRING_PROFILES_ACTIVE: dev
    healthcheck:
      test: ["CMD", "wget", "--no-verbose", "--tries=1", "--spider", "http://localhost:8081/actuator/health"]
      interval: 30s
      timeout: 10s
      retries: 3
      start_period: 40s

volumes:
  postgres_data:
  minio_data:

---
# README.md

# ISCM Vendor Service

## Overview
The Vendor Service is a Spring Boot microservice responsible for managing vendors, organizations, and warehouses in the Intelligent Supply Chain Management (ISCM) platform.

## Features
- ✅ Vendor onboarding and management
- ✅ Organization hierarchy management
- ✅ Warehouse tracking and geolocation
- ✅ KYC workflow for vendor verification
- ✅ Document management with S3 storage
- ✅ Event-driven architecture with Kafka
- ✅ JWT-based authentication & RBAC
- ✅ Multi-tenancy support
- ✅ RESTful APIs with OpenAPI documentation
- ✅ Database migrations with Flyway
- ✅ Metrics and monitoring with Prometheus

## Tech Stack
- **Framework**: Spring Boot 3.2
- **Language**: Java 21
- **Database**: PostgreSQL 16
- **Messaging**: Apache Kafka
- **Storage**: AWS S3 / MinIO
- **Security**: Spring Security + OAuth2 JWT
- **Documentation**: SpringDoc OpenAPI 3
- **Monitoring**: Actuator + Prometheus + Grafana

## Prerequisites
- Java 21+
- Maven 3.9+
- Docker & Docker Compose
- PostgreSQL 16+
- Kafka 3.5+

## Quick Start

### 1. Clone the Repository
```bash
git clone <repository-url>
cd vendor-service
```

### 2. Start Infrastructure with Docker Compose
```bash
docker-compose up -d postgres kafka zookeeper minio
```

### 3. Build the Application
```bash
mvn clean package
```

### 4. Run the Application
```bash
mvn spring-boot:run
```

Or run with Docker:
```bash
docker-compose up --build
```

### 5. Access the Application
- **API Base URL**: http://localhost:8081/api/v1
- **Swagger UI**: http://localhost:8081/swagger-ui.html
- **Health Check**: http://localhost:8081/actuator/health
- **Metrics**: http://localhost:8081/actuator/prometheus
- **MinIO Console**: http://localhost:9001 (minioadmin/minioadmin)

## API Endpoints

### Organizations
```
POST   /api/v1/organizations          - Create organization
GET    /api/v1/organizations          - List organizations
GET    /api/v1/organizations/{id}     - Get organization by ID
```

### Vendors
```
POST   /api/v1/vendors                - Create vendor
GET    /api/v1/vendors                - List vendors (paginated)
GET    /api/v1/vendors/{id}           - Get vendor by ID
GET    /api/v1/vendors/code/{code}    - Get vendor by code
GET    /api/v1/vendors/search         - Search vendors
PATCH  /api/v1/vendors/{id}           - Update vendor
PATCH  /api/v1/vendors/{id}/kyc-status - Update KYC status
DELETE /api/v1/vendors/{id}           - Delete vendor
```

### Warehouses
```
POST   /api/v1/warehouses             - Create warehouse
GET    /api/v1/warehouses             - List warehouses (paginated)
GET    /api/v1/warehouses/{id}        - Get warehouse by ID
GET    /api/v1/warehouses/vendor/{id} - Get warehouses by vendor
GET    /api/v1/warehouses/country/{code} - Get warehouses by country
PATCH  /api/v1/warehouses/{id}/status - Update warehouse status
DELETE /api/v1/warehouses/{id}        - Delete warehouse
```

### Documents
```
POST   /api/v1/documents/upload       - Upload vendor document (multipart/form-data)
GET    /api/v1/documents/vendor/{id}  - Get all documents for a vendor
GET    /api/v1/documents/{id}         - Get document by ID
GET    /api/v1/documents/{id}/download-url - Get presigned download URL
GET    /api/v1/documents/{id}/download - Download document directly
PATCH  /api/v1/documents/{id}/status  - Update document status (APPROVED/REJECTED)
DELETE /api/v1/documents/{id}         - Delete document
GET    /api/v1/documents/expiring     - Get expiring documents
GET    /api/v1/documents/status/{status} - Get documents by status
```

## Authentication
All endpoints require JWT authentication. Include the JWT token in the Authorization header:
```
Authorization: Bearer <your-jwt-token>
```

### Required Roles
- **ADMIN**: Full access to all operations
- **OPS**: Manage vendors and warehouses
- **VENDOR**: View own vendor information
- **BUYER**: View vendor information

## Configuration

### Environment Variables
```bash
# Database
DATABASE_URL=jdbc:postgresql://localhost:5432/vendor_db
DATABASE_USER=postgres
DATABASE_PASSWORD=postgres

# Kafka
KAFKA_BOOTSTRAP_SERVERS=localhost:9092

# S3/MinIO
S3_ENDPOINT=http://localhost:9000
S3_ACCESS_KEY=minioadmin
S3_SECRET_KEY=minioadmin
S3_BUCKET=vendor-documents

# Security
JWT_JWK_SET_URI=http://localhost:8080/auth/.well-known/jwks.json

# Server
SERVER_PORT=8081
SPRING_PROFILES_ACTIVE=dev
```

## Events Published

### Kafka Topics
- **vendor.events**: VendorOnboarded, WarehouseCreated

### Event Schema
```json
{
  "eventId": "uuid",
  "eventType": "VendorOnboarded",
  "timestamp": "2025-01-01T00:00:00Z",
  "vendorId": "uuid",
  "vendorCode": "V001",
  "legalName": "Acme Corp",
  "organizationId": "uuid",
  "status": "ACTIVE",
  "tenantId": "uuid"
}
```

## Database Schema

### Tables
1. **organizations** - Company/tenant information
2. **vendors** - Supplier/vendor master data
3. **warehouses** - Storage facility locations
4. **vendor_documents** - KYC and compliance documents

### Migrations
Flyway migrations are located in `src/main/resources/db/migration/`

## Testing

### Run Unit Tests
```bash
mvn test
```

### Run Integration Tests
```bash
mvn verify
```

### Test with Testcontainers
Integration tests use Testcontainers for PostgreSQL and Kafka.

## Monitoring

### Actuator Endpoints
- `/actuator/health` - Health status
- `/actuator/info` - Application info
- `/actuator/metrics` - Application metrics
- `/actuator/prometheus` - Prometheus metrics

### Grafana Dashboard
Import the provided Grafana dashboard for monitoring:
- JVM metrics
- HTTP request metrics
- Database connection pool
- Kafka producer metrics

## Development

### Code Structure
```
src/main/java/com/iscm/vendor/
├── config/          # Configuration classes
├── controller/      # REST controllers
├── service/         # Business logic
├── repository/      # Data access layer
├── entity/          # JPA entities
├── dto/             # Request/Response DTOs
├── events/          # Kafka event publishers
├── exception/       # Exception handlers
└── mapper/          # MapStruct mappers
```

### Best Practices
- Use DTOs for API contracts
- Apply RBAC with `@PreAuthorize`
- Log all important operations
- Validate all inputs with Jakarta Validation
- Use transactional boundaries appropriately
- Publish events asynchronously
- Implement idempotency where needed

## Deployment

### Kubernetes
```bash
# Build and push image
docker build -t iscm/vendor-service:1.0.0 .
docker push iscm/vendor-service:1.0.0

# Deploy to k8s
kubectl apply -f k8s/
```

### Environment-specific Configs
- **dev**: application-dev.yml
- **prod**: application-prod.yml

## Troubleshooting

### Common Issues

1. **Connection refused to Kafka**
   - Ensure Kafka is running: `docker-compose ps`
   - Check KAFKA_BOOTSTRAP_SERVERS configuration

2. **Database migration failed**
   - Verify PostgreSQL is running
   - Check Flyway migration scripts
   - Run: `mvn flyway:repair`

3. **JWT validation error**
   - Verify JWT_JWK_SET_URI is correct
   - Check auth service is running
   - Validate token expiry

## Contributing
1. Create a feature branch
2. Write tests for new features
3. Follow code style guidelines
4. Submit pull request with description

## License
Proprietary - ISCM Platform

## Support
For issues and questions:
- Email: support@iscm.com
- Slack: #iscm-vendor-service

# Dockerfile
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline
COPY src ./src
RUN mvn clean package -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

EXPOSE 8081

HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8081/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]

---
# docker-compose.yml

version: '3.8'

services:
  postgres:
    image: postgres:16-alpine
    container_name: vendor-postgres
    environment:
      POSTGRES_DB: vendor_db
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: postgres
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U postgres"]
      interval: 10s
      timeout: 5s
      retries: 5

  kafka:
    image: confluentinc/cp-kafka:7.5.0
    container_name: vendor-kafka
    depends_on:
      - zookeeper
    ports:
      - "9092:9092"
    environment:
      KAFKA_BROKER_ID: 1
      KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://localhost:9092
      KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1
      KAFKA_AUTO_CREATE_TOPICS_ENABLE: "true"
    healthcheck:
      test: ["CMD", "kafka-broker-api-versions", "--bootstrap-server", "localhost:9092"]
      interval: 10s
      timeout: 5s
      retries: 5

  zookeeper:
    image: confluentinc/cp-zookeeper:7.5.0
    container_name: vendor-zookeeper
    ports:
      - "2181:2181"
    environment:
      ZOOKEEPER_CLIENT_PORT: 2181
      ZOOKEEPER_TICK_TIME: 2000

  minio:
    image: minio/minio:latest
    container_name: vendor-minio
    command: server /data --console-address ":9001"
    ports:
      - "9000:9000"
      - "9001:9001"
    environment:
      MINIO_ROOT_USER: minioadmin
      MINIO_ROOT_PASSWORD: minioadmin
    volumes:
      - minio_data:/data
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:9000/minio/health/live"]
      interval: 10s
      timeout: 5s
      retries: 5

  vendor-service:
    build: .
    container_name: vendor-service
    depends_on:
      postgres:
        condition: service_healthy
      kafka:
        condition: service_healthy
      minio:
        condition: service_healthy
    ports:
      - "8081:8081"
    environment:
      DATABASE_URL: jdbc:postgresql://postgres:5432/vendor_db
      DATABASE_USER: postgres
      DATABASE_PASSWORD: postgres
      KAFKA_BOOTSTRAP_SERVERS: kafka:9092
      MINIO_ENDPOINT: http://minio:9000
      MINIO_ACCESS_KEY: minioadmin
      MINIO_SECRET_KEY: minioadmin
      MINIO_BUCKET: vendor-documents
      JWT_JWK_SET_URI: http://gateway:8080/auth/.well-known/jwks.json
      SPRING_PROFILES_ACTIVE: dev
    healthcheck:
      test: ["CMD", "wget", "--no-verbose", "--tries=1", "--spider", "http://localhost:8081/actuator/health"]
      interval: 30s
      timeout: 10s
      retries: 3
      start_period: 40s

volumes:
  postgres_data:
  minio_data:

---
# README.md

# ISCM Vendor Service

## Overview
The Vendor Service is a Spring Boot microservice responsible for managing vendors, organizations, and warehouses in the Intelligent Supply Chain Management (ISCM) platform.

## Features
- ✅ Vendor onboarding and management
- ✅ Organization hierarchy management
- ✅ Warehouse tracking and geolocation
- ✅ KYC workflow for vendor verification
- ✅ Document management with MinIO storage
- ✅ File upload/download with presigned URLs
- ✅ Document expiry tracking and alerts
- ✅ Event-driven architecture with Kafka
- ✅ JWT-based authentication & RBAC
- ✅ Multi-tenancy support
- ✅ RESTful APIs with OpenAPI documentation
- ✅ Database migrations with Flyway
- ✅ Metrics and monitoring with Prometheus

## Tech Stack
- **Framework**: Spring Boot 3.2
- **Language**: Java 21
- **Database**: PostgreSQL 16
- **Messaging**: Apache Kafka
- **Storage**: AWS S3 / MinIO
- **Security**: Spring Security + OAuth2 JWT
- **Documentation**: SpringDoc OpenAPI 3
- **Monitoring**: Actuator + Prometheus + Grafana

## Prerequisites
- Java 21+
- Maven 3.9+
- Docker & Docker Compose
- PostgreSQL 16+
- Kafka 3.5+

## Quick Start

### 1. Clone the Repository
```bash
git clone <repository-url>
cd vendor-service
```

### 2. Start Infrastructure with Docker Compose
```bash
docker-compose up -d postgres kafka zookeeper minio
```

### 3. Build the Application
```bash
mvn clean package
```

### 4. Run the Application
```bash
mvn spring-boot:run
```

Or run with Docker:
```bash
docker-compose up --build
```

### 5. Access the Application
- **API Base URL**: http://localhost:8081/api/v1
- **Swagger UI**: http://localhost:8081/swagger-ui.html
- **Health Check**: http://localhost:8081/actuator/health
- **Metrics**: http://localhost:8081/actuator/prometheus
- **MinIO Console**: http://localhost:9001 (minioadmin/minioadmin)

## API Endpoints

### Organizations
```
POST   /api/v1/organizations          - Create organization
GET    /api/v1/organizations          - List organizations
GET    /api/v1/organizations/{id}     - Get organization by ID
```

### Vendors
```
POST   /api/v1/vendors                - Create vendor
GET    /api/v1/vendors                - List vendors (paginated)
GET    /api/v1/vendors/{id}           - Get vendor by ID
GET    /api/v1/vendors/code/{code}    - Get vendor by code
GET    /api/v1/vendors/search         - Search vendors
PATCH  /api/v1/vendors/{id}           - Update vendor
PATCH  /api/v1/vendors/{id}/kyc-status - Update KYC status
DELETE /api/v1/vendors/{id}           - Delete vendor
```

### Warehouses
```
POST   /api/v1/warehouses             - Create warehouse
GET    /api/v1/warehouses             - List warehouses (paginated)
GET    /api/v1/warehouses/{id}        - Get warehouse by ID
GET    /api/v1/warehouses/vendor/{id} - Get warehouses by vendor
GET    /api/v1/warehouses/country/{code} - Get warehouses by country
PATCH  /api/v1/warehouses/{id}/status - Update warehouse status
DELETE /api/v1/warehouses/{id}        - Delete warehouse
```

### Documents
```
POST   /api/v1/documents/upload       - Upload vendor document (multipart/form-data)
GET    /api/v1/documents/vendor/{id}  - Get all documents for a vendor
GET    /api/v1/documents/{id}         - Get document by ID
GET    /api/v1/documents/{id}/download-url - Get presigned download URL
GET    /api/v1/documents/{id}/download - Download document directly
PATCH  /api/v1/documents/{id}/status  - Update document status (APPROVED/REJECTED)
DELETE /api/v1/documents/{id}         - Delete document
GET    /api/v1/documents/expiring     - Get expiring documents
GET    /api/v1/documents/status/{status} - Get documents by status
```

## Authentication
All endpoints require JWT authentication. Include the JWT token in the Authorization header:
```
Authorization: Bearer <your-jwt-token>
```

### Required Roles
- **ADMIN**: Full access to all operations
- **OPS**: Manage vendors and warehouses
- **VENDOR**: View own vendor information
- **BUYER**: View vendor information

## Configuration

### Environment Variables
```bash
# Database
DATABASE_URL=jdbc:postgresql://localhost:5432/vendor_db
DATABASE_USER=postgres
DATABASE_PASSWORD=postgres

# Kafka
KAFKA_BOOTSTRAP_SERVERS=localhost:9092

# MinIO
MINIO_ENDPOINT=http://localhost:9000
MINIO_ACCESS_KEY=minioadmin
MINIO_SECRET_KEY=minioadmin
MINIO_BUCKET=vendor-documents

# Security
JWT_JWK_SET_URI=http://localhost:8080/auth/.well-known/jwks.json

# Server
SERVER_PORT=8081
SPRING_PROFILES_ACTIVE=dev
```

## Events Published

### Kafka Topics
- **vendor.events**: VendorOnboarded, WarehouseCreated

### Event Schema
```json
{
  "eventId": "uuid",
  "eventType": "VendorOnboarded",
  "timestamp": "2025-01-01T00:00:00Z",
  "vendorId": "uuid",
  "vendorCode": "V001",
  "legalName": "Acme Corp",
  "organizationId": "uuid",
  "status": "ACTIVE",
  "tenantId": "uuid"
}
```

## Database Schema

### Tables
1. **organizations** - Company/tenant information
2. **vendors** - Supplier/vendor master data
3. **warehouses** - Storage facility locations
4. **vendor_documents** - KYC and compliance documents

### Migrations
Flyway migrations are located in `src/main/resources/db/migration/`

## Testing

### Run Unit Tests
```bash
mvn test
```

### Run Integration Tests
```bash
mvn verify
```

### Test with Testcontainers
Integration tests use Testcontainers for PostgreSQL and Kafka.

## Monitoring

### Actuator Endpoints
- `/actuator/health` - Health status
- `/actuator/info` - Application info
- `/actuator/metrics` - Application metrics
- `/actuator/prometheus` - Prometheus metrics

### Grafana Dashboard
Import the provided Grafana dashboard for monitoring:
- JVM metrics
- HTTP request metrics
- Database connection pool
- Kafka producer metrics

## Development

### Code Structure
```
src/main/java/com/iscm/vendor/
├── config/          # Configuration classes
├── controller/      # REST controllers
├── service/         # Business logic
├── repository/      # Data access layer
├── entity/          # JPA entities
├── dto/             # Request/Response DTOs
├── events/          # Kafka event publishers
├── exception/       # Exception handlers
└── mapper/          # MapStruct mappers
```

### Best Practices
- Use DTOs for API contracts
- Apply RBAC with `@PreAuthorize`
- Log all important operations
- Validate all inputs with Jakarta Validation
- Use transactional boundaries appropriately
- Publish events asynchronously
- Implement idempotency where needed

## Deployment

### Kubernetes
```bash
# Build and push image
docker build -t iscm/vendor-service:1.0.0 .
docker push iscm/vendor-service:1.0.0

# Deploy to k8s
kubectl apply -f k8s/
```

### Environment-specific Configs
- **dev**: application-dev.yml
- **prod**: application-prod.yml

## Troubleshooting

### Common Issues

1. **Connection refused to Kafka**
   - Ensure Kafka is running: `docker-compose ps`
   - Check KAFKA_BOOTSTRAP_SERVERS configuration

2. **Database migration failed**
   - Verify PostgreSQL is running
   - Check Flyway migration scripts
   - Run: `mvn flyway:repair`

3. **JWT validation error**
   - Verify JWT_JWK_SET_URI is correct
   - Check auth service is running
   - Validate token expiry

## Contributing
1. Create a feature branch
2. Write tests for new features
3. Follow code style guidelines
4. Submit pull request with description

## License
Proprietary - ISCM Platform

## Support
For issues and questions:
- Email: support@iscm.com
- Slack: #iscm-vendor-service