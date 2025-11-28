# ISCM Vendor Service - Integration Guide

## Table of Contents
1. [Document Upload Integration](#document-upload-integration)
2. [MinIO Configuration](#minio-configuration)
3. [Kafka Event Handling](#kafka-event-handling)
4. [Security Best Practices](#security-best-practices)
5. [Performance Optimization](#performance-optimization)
6. [Troubleshooting](#troubleshooting)

---

## Document Upload Integration

### Uploading Documents via API

**Example using cURL:**
```bash
curl -X POST http://localhost:8081/api/v1/documents/upload \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -F "file=@/path/to/document.pdf" \
  -F "vendorId=33333333-3333-3333-3333-333333333333" \
  -F "type=TAX_CERTIFICATE" \
  -F "expiryDate=2025-12-31" \
  -F "notes=Annual tax certificate"
```

**Example using JavaScript (React):**
```javascript
const uploadDocument = async (vendorId, file, metadata) => {
  const formData = new FormData();
  formData.append('file', file);
  formData.append('vendorId', vendorId);
  formData.append('type', metadata.type);
  formData.append('expiryDate', metadata.expiryDate);
  formData.append('notes', metadata.notes);

  const response = await fetch('http://localhost:8081/api/v1/documents/upload', {
    method: 'POST',
    headers: {
      'Authorization': `Bearer ${token}`
    },
    body: formData
  });

  return await response.json();
};
```

**Example using Java Spring RestTemplate:**
```java
MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
body.add("file", new FileSystemResource(file));
body.add("vendorId", vendorId);
body.add("type", "TAX_CERTIFICATE");
body.add("expiryDate", "2025-12-31");

HttpHeaders headers = new HttpHeaders();
headers.setContentType(MediaType.MULTIPART_FORM_DATA);
headers.setBearerAuth(token);

HttpEntity<MultiValueMap<String, Object>> requestEntity = 
    new HttpEntity<>(body, headers);

ResponseEntity<DocumentResponse> response = restTemplate.postForEntity(
    "http://localhost:8081/api/v1/documents/upload",
    requestEntity,
    DocumentResponse.class
);
```

### Supported Document Types
- `TAX_CERTIFICATE` - Tax registration certificates
- `BUSINESS_LICENSE` - Business operation licenses
- `BANK_STATEMENT` - Bank account statements
- `PAN_CARD` - PAN card (India)
- `GST_CERTIFICATE` - GST registration (India)
- `INCORPORATION_CERTIFICATE` - Company incorporation docs
- `TRADE_LICENSE` - Import/export licenses
- `ISO_CERTIFICATE` - Quality certifications
- `INSURANCE_CERTIFICATE` - Insurance documents
- `CONTRACT` - Business contracts
- `OTHER` - Other documents

### Document Status Workflow
```
PENDING_REVIEW → (Admin reviews) → APPROVED/REJECTED
              ↓
           EXPIRED (based on expiryDate)
```

---

## MinIO Configuration

### Local Development Setup
```yaml
# docker-compose.yml snippet
minio:
  image: minio/minio:latest
  command: server /data --console-address ":9001"
  ports:
    - "9000:9000"  # API
    - "9001:9001"  # Console
  environment:
    MINIO_ROOT_USER: minioadmin
    MINIO_ROOT_PASSWORD: minioadmin
  volumes:
    - minio_data:/data
```

### Creating Buckets via MinIO Client
```bash
# Download mc (MinIO Client)
wget https://dl.min.io/client/mc/release/linux-amd64/mc
chmod +x mc

# Configure alias
./mc alias set local http://localhost:9000 minioadmin minioadmin

# Create bucket
./mc mb local/vendor-documents

# Set bucket policy (public read for presigned URLs to work)
./mc anonymous set download local/vendor-documents
```

### Production Configuration (AWS S3 Compatible)
```yaml
# application-prod.yml
minio:
  endpoint: https://s3.amazonaws.com
  access-key: ${AWS_ACCESS_KEY_ID}
  secret-key: ${AWS_SECRET_ACCESS_KEY}
  bucket-name: prod-vendor-documents
```

### Storage Structure
```
vendor-documents/
├── vendors/
│   ├── {vendor-id}/
│   │   ├── documents/
│   │   │   ├── tax_certificate/
│   │   │   │   └── {uuid}.pdf
│   │   │   ├── business_license/
│   │   │   │   └── {uuid}.jpg
│   │   │   └── ...
```

---

## Kafka Event Handling

### Events Published by Vendor Service

**1. VendorOnboarded Event**
```json
{
  "eventId": "550e8400-e29b-41d4-a716-446655440000",
  "eventType": "VendorOnboarded",
  "timestamp": "2025-01-15T10:30:00Z",
  "vendorId": "33333333-3333-3333-3333-333333333333",
  "vendorCode": "V001",
  "legalName": "Tech Supplies Ltd",
  "organizationId": "11111111-1111-1111-1111-111111111111",
  "status": "PENDING",
  "tenantId": "99999999-9999-9999-9999-999999999999"
}
```

**2. WarehouseCreated Event**
```json
{
  "eventId": "660e8400-e29b-41d4-a716-446655440001",
  "eventType": "WarehouseCreated",
  "timestamp": "2025-01-15T11:00:00Z",
  "warehouseId": "55555555-5555-5555-5555-555555555555",
  "warehouseCode": "WH001",
  "name": "Main Distribution Center",
  "organizationId": "11111111-1111-1111-1111-111111111111",
  "vendorId": "33333333-3333-3333-3333-333333333333",
  "country": "USA",
  "city": "Dallas",
  "tenantId": "99999999-9999-9999-9999-999999999999"
}
```

### Consuming Events (Example - Spring Kafka)
```java
@KafkaListener(topics = "vendor.events", groupId = "inventory-service")
public void handleVendorEvent(String message) {
    JsonNode event = objectMapper.readTree(message);
    String eventType = event.get("eventType").asText();
    
    switch (eventType) {
        case "VendorOnboarded":
            handleVendorOnboarded(event);
            break;
        case "WarehouseCreated":
            handleWarehouseCreated(event);
            break;
    }
}
```

### Event Idempotency
All events include an `eventId` (UUID). Consumers should track processed event IDs to ensure idempotent processing:

```java
@Transactional
public void handleEvent(Event event) {
    if (processedEventRepository.existsById(event.getEventId())) {
        log.info("Event already processed: {}", event.getEventId());
        return; // Skip duplicate
    }
    
    // Process event
    processEvent(event);
    
    // Mark as processed
    processedEventRepository.save(new ProcessedEvent(event.getEventId()));
}
```

---

## Security Best Practices

### JWT Token Validation
The service validates JWT tokens against a JWK Set URI. Ensure your tokens include:
- `sub` (subject) - User ID
- `tenant_id` - Tenant/Organization ID
- `roles` - User roles (ADMIN, OPS, VENDOR, etc.)

**Example JWT Claims:**
```json
{
  "sub": "user-uuid",
  "tenant_id": "tenant-uuid",
  "roles": ["ADMIN", "OPS"],
  "exp": 1735689600
}
```

### Role-Based Access Control

| Endpoint | ADMIN | OPS | VENDOR | BUYER |
|----------|-------|-----|--------|-------|
| Create Organization | ✅ | ❌ | ❌ | ❌ |
| Create Vendor | ✅ | ✅ | ❌ | ❌ |
| View Vendors | ✅ | ✅ | ✅ | ✅ |
| Update Vendor | ✅ | ✅ | ❌ | ❌ |
| Upload Documents | ✅ | ✅ | ✅ | ❌ |
| Approve Documents | ✅ | ✅ | ❌ | ❌ |
| Create Warehouse | ✅ | ✅ | ❌ | ❌ |

### Multi-Tenancy Isolation
All queries automatically filter by `tenantId` from the JWT token. **Never** expose raw database IDs across tenants.

### Sensitive Data Handling
- PII fields (email, phone) are logged at DEBUG level only
- Document content is never logged
- S3 keys use UUIDs, not predictable names
- Presigned URLs expire after configured days (default: 7)

---

## Performance Optimization

### Database Indexing
The service creates indexes on:
- `tenant_id` (all tables) - for multi-tenancy queries
- `vendor_code` - for lookups
- `warehouse_code` - for lookups
- `(latitude, longitude)` - for geospatial queries
- `status`, `kyc_status` - for filtering

### Connection Pool Configuration
```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      connection-timeout: 30000
      idle-timeout: 600000
      max-lifetime: 1800000
```

### Caching Strategy (Optional)
For frequently accessed vendors, consider adding Redis:

```java
@Cacheable(value = "vendors", key = "#vendorCode")
public VendorResponse getVendorByCode(String vendorCode, UUID tenantId) {
    // ...
}

@CacheEvict(value = "vendors", key = "#result.vendorCode")
public VendorResponse updateVendor(UUID id, UpdateVendorRequest request) {
    // ...
}
```

### Large File Upload Optimization
```yaml
spring:
  servlet:
    multipart:
      max-file-size: 50MB
      max-request-size: 50MB
```

For files > 50MB, consider implementing multipart uploads with resumable uploads.

---

## Troubleshooting

### Common Issues

#### 1. "MinIO connection refused"
**Symptoms:** `ConnectException: Connection refused` when uploading documents

**Solution:**
```bash
# Check MinIO is running
docker ps | grep minio

# Check MinIO health
curl http://localhost:9000/minio/health/live

# Restart MinIO
docker-compose restart minio

# Verify endpoint in application.yml
minio:
  endpoint: http://localhost:9000  # Not https for local
```

#### 2. "Bucket does not exist"
**Symptoms:** `ErrorResponse: NoSuchBucket`

**Solution:**
The service auto-creates buckets on first upload, but you can manually create:
```bash
# Using MinIO Client
mc mb local/vendor-documents

# Or via MinIO Console
# Open http://localhost:9001, login, create bucket
```

#### 3. "Document upload returns 413 Payload Too Large"
**Solution:**
Increase max file size:
```yaml
spring:
  servlet:
    multipart:
      max-file-size: 100MB
      max-request-size: 100MB
```

Also check gateway/proxy limits (NGINX, Kong, etc.)

#### 4. "Kafka producer timeout"
**Symptoms:** `TimeoutException` when creating vendors

**Solution:**
```bash
# Check Kafka is running
docker-compose ps kafka

# Check Kafka logs
docker-compose logs kafka

# Test Kafka connectivity
docker exec -it vendor-kafka kafka-broker-api-versions \
  --bootstrap-server localhost:9092

# Increase producer timeout
spring:
  kafka:
    producer:
      properties:
        request.timeout.ms: 60000
```

#### 5. "Flyway migration failed"
**Symptoms:** `FlywayException: Validate failed`

**Solution:**
```bash
# Check migration history
docker exec -it vendor-postgres psql -U postgres -d vendor_db \
  -c "SELECT * FROM flyway_schema_history;"

# Repair Flyway (if corrupted)
mvn flyway:repair

# Or clean and re-migrate (DEV ONLY!)
mvn flyway:clean flyway:migrate
```

#### 6. "JWT validation error: Invalid signature"
**Symptoms:** `401 Unauthorized` despite valid-looking token

**Solution:**
- Verify JWK Set URI is accessible:
  ```bash
  curl http://localhost:8080/auth/.well-known/jwks.json
  ```
- Check token algorithm matches (RS256 vs HS256)
- Ensure clock sync between services (JWT exp validation)
- Verify token hasn't expired

#### 7. "Document download returns 403 Forbidden"
**Symptoms:** Presigned URL returns AccessDenied

**Solution:**
- Check MinIO bucket policy allows downloads
- Verify presigned URL hasn't expired (default 7 days)
- Check MinIO access key/secret key are correct
- For AWS S3, verify IAM permissions

### Debug Logging

Enable detailed logging for troubleshooting:
```yaml
logging:
  level:
    com.iscm.vendor: DEBUG
    io.minio: DEBUG
    org.springframework.kafka: DEBUG
    org.springframework.security: DEBUG
    org.hibernate.SQL: DEBUG
```

### Health Checks

**Liveness Probe:**
```bash
curl http://localhost:8081/actuator/health/liveness
```

**Readiness Probe:**
```bash
curl http://localhost:8081/actuator/health/readiness
```

**Custom Health Indicators:**
The service includes health checks for:
- Database connectivity
- Kafka broker availability
- MinIO storage availability

### Performance Monitoring

**View JVM metrics:**
```bash
curl http://localhost:8081/actuator/metrics/jvm.memory.used | jq
```

**View HTTP request metrics:**
```bash
curl http://localhost:8081/actuator/metrics/http.server.requests | jq
```

**View database connection pool:**
```bash
curl http://localhost:8081/actuator/metrics/hikaricp.connections.active | jq
```

---

## Testing Checklist

Before deploying to production:

- [ ] All unit tests pass (`mvn test`)
- [ ] All integration tests pass (`mvn verify`)
- [ ] Document upload/download works end-to-end
- [ ] JWT authentication properly configured
- [ ] MinIO/S3 bucket exists and accessible
- [ ] Kafka topics created and accessible
- [ ] Database migrations applied successfully
- [ ] Health checks return healthy status
- [ ] Metrics are being exported to Prometheus
- [ ] Logs are structured and contain no sensitive data
- [ ] API documentation accessible at /swagger-ui.html
- [ ] Rate limiting configured (if using API Gateway)
- [ ] Backup strategy in place for PostgreSQL
- [ ] MinIO/S3 versioning enabled for documents

---

## Next Steps

1. **Integrate with API Gateway** for centralized routing and auth
2. **Set up Grafana dashboards** for monitoring
3. **Configure alerts** for critical metrics (document expiry, failed uploads)
4. **Implement audit logging** for compliance
5. **Set up backup automation** for database and MinIO
6. **Configure CI/CD pipeline** for automated deployments

For further assistance, refer to:
- [Spring Boot Documentation](https://docs.spring.io/spring-boot/)
- [MinIO Documentation](https://min.io/docs/)
- [Apache Kafka Documentation](https://kafka.apache.org/documentation/)