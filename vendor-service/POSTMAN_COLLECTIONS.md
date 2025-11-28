{
  "info": {
    "name": "ISCM Vendor Service API",
    "description": "Complete API collection for Vendor Service",
    "schema": "https://schema.getpostman.com/json/collection/v2.1.0/collection.json"
  },
  "variable": [
    {
      "key": "baseUrl",
      "value": "http://localhost:8081/api/v1",
      "type": "string"
    },
    {
      "key": "token",
      "value": "",
      "type": "string"
    }
  ],
  "auth": {
    "type": "bearer",
    "bearer": [
      {
        "key": "token",
        "value": "{{token}}",
        "type": "string"
      }
    ]
  },
  "item": [
    {
      "name": "Organizations",
      "item": [
        {
          "name": "Create Organization",
          "request": {
            "method": "POST",
            "header": [
              {
                "key": "Content-Type",
                "value": "application/json"
              }
            ],
            "body": {
              "mode": "raw",
              "raw": "{\n  \"name\": \"Acme Corporation\",\n  \"domain\": \"acme.com\",\n  \"email\": \"info@acme.com\",\n  \"phone\": \"+1-555-0100\",\n  \"addressLine1\": \"123 Business Street\",\n  \"city\": \"New York\",\n  \"state\": \"NY\",\n  \"country\": \"USA\",\n  \"postalCode\": \"10001\"\n}"
            },
            "url": {
              "raw": "{{baseUrl}}/organizations",
              "host": ["{{baseUrl}}"],
              "path": ["organizations"]
            }
          }
        },
        {
          "name": "Get All Organizations",
          "request": {
            "method": "GET",
            "url": {
              "raw": "{{baseUrl}}/organizations?page=0&size=20",
              "host": ["{{baseUrl}}"],
              "path": ["organizations"],
              "query": [
                {
                  "key": "page",
                  "value": "0"
                },
                {
                  "key": "size",
                  "value": "20"
                }
              ]
            }
          }
        },
        {
          "name": "Get Organization by ID",
          "request": {
            "method": "GET",
            "url": {
              "raw": "{{baseUrl}}/organizations/:id",
              "host": ["{{baseUrl}}"],
              "path": ["organizations", ":id"],
              "variable": [
                {
                  "key": "id",
                  "value": "{{organizationId}}"
                }
              ]
            }
          }
        }
      ]
    },
    {
      "name": "Vendors",
      "item": [
        {
          "name": "Create Vendor",
          "request": {
            "method": "POST",
            "header": [
              {
                "key": "Content-Type",
                "value": "application/json"
              }
            ],
            "body": {
              "mode": "raw",
              "raw": "{\n  \"vendorCode\": \"V001\",\n  \"legalName\": \"Tech Supplies Ltd\",\n  \"displayName\": \"Tech Supplies\",\n  \"organizationId\": \"{{organizationId}}\",\n  \"email\": \"vendor@techsupplies.com\",\n  \"phone\": \"+1-555-0200\",\n  \"website\": \"https://techsupplies.com\",\n  \"taxId\": \"12-3456789\",\n  \"slaTier\": \"PREMIUM\",\n  \"paymentTerms\": \"NET30\",\n  \"currency\": \"USD\",\n  \"addressLine1\": \"456 Vendor Avenue\",\n  \"city\": \"San Francisco\",\n  \"state\": \"CA\",\n  \"country\": \"USA\",\n  \"postalCode\": \"94102\"\n}"
            },
            "url": {
              "raw": "{{baseUrl}}/vendors",
              "host": ["{{baseUrl}}"],
              "path": ["vendors"]
            }
          }
        },
        {
          "name": "Get All Vendors",
          "request": {
            "method": "GET",
            "url": {
              "raw": "{{baseUrl}}/vendors?page=0&size=20",
              "host": ["{{baseUrl}}"],
              "path": ["vendors"],
              "query": [
                {
                  "key": "page",
                  "value": "0"
                },
                {
                  "key": "size",
                  "value": "20"
                }
              ]
            }
          }
        },
        {
          "name": "Get Vendor by ID",
          "request": {
            "method": "GET",
            "url": {
              "raw": "{{baseUrl}}/vendors/:id",
              "host": ["{{baseUrl}}"],
              "path": ["vendors", ":id"],
              "variable": [
                {
                  "key": "id",
                  "value": "{{vendorId}}"
                }
              ]
            }
          }
        },
        {
          "name": "Search Vendors",
          "request": {
            "method": "GET",
            "url": {
              "raw": "{{baseUrl}}/vendors/search?query=tech&page=0&size=20",
              "host": ["{{baseUrl}}"],
              "path": ["vendors", "search"],
              "query": [
                {
                  "key": "query",
                  "value": "tech"
                },
                {
                  "key": "page",
                  "value": "0"
                },
                {
                  "key": "size",
                  "value": "20"
                }
              ]
            }
          }
        },
        {
          "name": "Update Vendor",
          "request": {
            "method": "PATCH",
            "header": [
              {
                "key": "Content-Type",
                "value": "application/json"
              }
            ],
            "body": {
              "mode": "raw",
              "raw": "{\n  \"displayName\": \"Tech Supplies Pro\",\n  \"email\": \"contact@techsupplies.com\",\n  \"status\": \"ACTIVE\"\n}"
            },
            "url": {
              "raw": "{{baseUrl}}/vendors/:id",
              "host": ["{{baseUrl}}"],
              "path": ["vendors", ":id"],
              "variable": [
                {
                  "key": "id",
                  "value": "{{vendorId}}"
                }
              ]
            }
          }
        },
        {
          "name": "Update KYC Status",
          "request": {
            "method": "PATCH",
            "url": {
              "raw": "{{baseUrl}}/vendors/:id/kyc-status?kycStatus=VERIFIED",
              "host": ["{{baseUrl}}"],
              "path": ["vendors", ":id", "kyc-status"],
              "query": [
                {
                  "key": "kycStatus",
                  "value": "VERIFIED"
                }
              ],
              "variable": [
                {
                  "key": "id",
                  "value": "{{vendorId}}"
                }
              ]
            }
          }
        }
      ]
    },
    {
      "name": "Warehouses",
      "item": [
        {
          "name": "Create Warehouse",
          "request": {
            "method": "POST",
            "header": [
              {
                "key": "Content-Type",
                "value": "application/json"
              }
            ],
            "body": {
              "mode": "raw",
              "raw": "{\n  \"warehouseCode\": \"WH001\",\n  \"name\": \"Main Distribution Center\",\n  \"organizationId\": \"{{organizationId}}\",\n  \"vendorId\": \"{{vendorId}}\",\n  \"type\": \"DISTRIBUTION\",\n  \"addressLine1\": \"789 Warehouse Road\",\n  \"city\": \"Dallas\",\n  \"state\": \"TX\",\n  \"country\": \"USA\",\n  \"postalCode\": \"75201\",\n  \"latitude\": 32.7767,\n  \"longitude\": -96.7970,\n  \"timezone\": \"America/Chicago\",\n  \"contactPhone\": \"+1-555-0300\",\n  \"contactEmail\": \"warehouse@acme.com\",\n  \"managerName\": \"John Warehouse\",\n  \"capacitySqft\": 50000,\n  \"isTemperatureControlled\": false,\n  \"operatingHours\": \"Mon-Fri 8AM-6PM\"\n}"
            },
            "url": {
              "raw": "{{baseUrl}}/warehouses",
              "host": ["{{baseUrl}}"],
              "path": ["warehouses"]
            }
          }
        },
        {
          "name": "Get All Warehouses",
          "request": {
            "method": "GET",
            "url": {
              "raw": "{{baseUrl}}/warehouses?page=0&size=20",
              "host": ["{{baseUrl}}"],
              "path": ["warehouses"],
              "query": [
                {
                  "key": "page",
                  "value": "0"
                },
                {
                  "key": "size",
                  "value": "20"
                }
              ]
            }
          }
        },
        {
          "name": "Get Warehouses by Vendor",
          "request": {
            "method": "GET",
            "url": {
              "raw": "{{baseUrl}}/warehouses/vendor/:vendorId",
              "host": ["{{baseUrl}}"],
              "path": ["warehouses", "vendor", ":vendorId"],
              "variable": [
                {
                  "key": "vendorId",
                  "value": "{{vendorId}}"
                }
              ]
            }
          }
        },
        {
          "name": "Get Warehouses by Country",
          "request": {
            "method": "GET",
            "url": {
              "raw": "{{baseUrl}}/warehouses/country/USA",
              "host": ["{{baseUrl}}"],
              "path": ["warehouses", "country", "USA"]
            }
          }
        }
      ]
    },
    {
      "name": "Documents",
      "item": [
        {
          "name": "Upload Document",
          "request": {
            "method": "POST",
            "header": [],
            "body": {
              "mode": "formdata",
              "formdata": [
                {
                  "key": "file",
                  "type": "file",
                  "src": "/path/to/document.pdf"
                },
                {
                  "key": "vendorId",
                  "value": "{{vendorId}}",
                  "type": "text"
                },
                {
                  "key": "type",
                  "value": "TAX_CERTIFICATE",
                  "type": "text"
                },
                {
                  "key": "expiryDate",
                  "value": "2025-12-31",
                  "type": "text"
                },
                {
                  "key": "notes",
                  "value": "Annual tax certificate",
                  "type": "text"
                }
              ]
            },
            "url": {
              "raw": "{{baseUrl}}/documents/upload",
              "host": ["{{baseUrl}}"],
              "path": ["documents", "upload"]
            }
          }
        },
        {
          "name": "Get Vendor Documents",
          "request": {
            "method": "GET",
            "url": {
              "raw": "{{baseUrl}}/documents/vendor/:vendorId",
              "host": ["{{baseUrl}}"],
              "path": ["documents", "vendor", ":vendorId"],
              "variable": [
                {
                  "key": "vendorId",
                  "value": "{{vendorId}}"
                }
              ]
            }
          }
        },
        {
          "name": "Get Document Download URL",
          "request": {
            "method": "GET",
            "url": {
              "raw": "{{baseUrl}}/documents/:documentId/download-url",
              "host": ["{{baseUrl}}"],
              "path": ["documents", ":documentId", "download-url"],
              "variable": [
                {
                  "key": "documentId",
                  "value": "{{documentId}}"
                }
              ]
            }
          }
        },
        {
          "name": "Download Document",
          "request": {
            "method": "GET",
            "url": {
              "raw": "{{baseUrl}}/documents/:documentId/download",
              "host": ["{{baseUrl}}"],
              "path": ["documents", ":documentId", "download"],
              "variable": [
                {
                  "key": "documentId",
                  "value": "{{documentId}}"
                }
              ]
            }
          }
        },
        {
          "name": "Update Document Status",
          "request": {
            "method": "PATCH",
            "url": {
              "raw": "{{baseUrl}}/documents/:documentId/status?status=APPROVED",
              "host": ["{{baseUrl}}"],
              "path": ["documents", ":documentId", "status"],
              "query": [
                {
                  "key": "status",
                  "value": "APPROVED"
                }
              ],
              "variable": [
                {
                  "key": "documentId",
                  "value": "{{documentId}}"
                }
              ]
            }
          }
        },
        {
          "name": "Get Expiring Documents",
          "request": {
            "method": "GET",
            "url": {
              "raw": "{{baseUrl}}/documents/expiring?beforeDate=2025-12-31",
              "host": ["{{baseUrl}}"],
              "path": ["documents", "expiring"],
              "query": [
                {
                  "key": "beforeDate",
                  "value": "2025-12-31"
                }
              ]
            }
          }
        },
        {
          "name": "Get Documents by Status",
          "request": {
            "method": "GET",
            "url": {
              "raw": "{{baseUrl}}/documents/status/PENDING_REVIEW",
              "host": ["{{baseUrl}}"],
              "path": ["documents", "status", "PENDING_REVIEW"]
            }
          }
        }
      ]
    },
    {
      "name": "Health & Monitoring",
      "item": [
        {
          "name": "Health Check",
          "request": {
            "method": "GET",
            "url": {
              "raw": "http://localhost:8081/actuator/health",
              "protocol": "http",
              "host": ["localhost"],
              "port": "8081",
              "path": ["actuator", "health"]
            }
          }
        },
        {
          "name": "Prometheus Metrics",
          "request": {
            "method": "GET",
            "url": {
              "raw": "http://localhost:8081/actuator/prometheus",
              "protocol": "http",
              "host": ["localhost"],
              "port": "8081",
              "path": ["actuator", "prometheus"]
            }
          }
        }
      ]
    }
  ]
}

---
# seed-data.sql - Sample data for testing

-- Insert Organizations
INSERT INTO organizations (id, tenant_id, name, domain, email, phone, city, state, country, postal_code, status, version, created_at, updated_at)
VALUES 
  ('11111111-1111-1111-1111-111111111111'::uuid, '99999999-9999-9999-9999-999999999999'::uuid, 'Acme Corporation', 'acme.com', 'info@acme.com', '+1-555-0100', 'New York', 'NY', 'USA', '10001', 'ACTIVE', 0, NOW(), NOW()),
  ('22222222-2222-2222-2222-222222222222'::uuid, '99999999-9999-9999-9999-999999999999'::uuid, 'Global Logistics Inc', 'globallogistics.com', 'contact@globallogistics.com', '+1-555-0101', 'Los Angeles', 'CA', 'USA', '90001', 'ACTIVE', 0, NOW(), NOW());

-- Insert Vendors
INSERT INTO vendors (id, tenant_id, vendor_code, legal_name, display_name, organization_id, email, phone, website, tax_id, status, kyc_status, sla_tier, payment_terms, currency, city, state, country, postal_code, version, created_at, updated_at)
VALUES
  ('33333333-3333-3333-3333-333333333333'::uuid, '99999999-9999-9999-9999-999999999999'::uuid, 'V001', 'Tech Supplies Ltd', 'Tech Supplies', '11111111-1111-1111-1111-111111111111'::uuid, 'vendor@techsupplies.com', '+1-555-0200', 'https://techsupplies.com', '12-3456789', 'ACTIVE', 'VERIFIED', 'PREMIUM', 'NET30', 'USD', 'San Francisco', 'CA', 'USA', '94102', 0, NOW(), NOW()),
  ('44444444-4444-4444-4444-444444444444'::uuid, '99999999-9999-9999-9999-999999999999'::uuid, 'V002', 'Office Goods International', 'Office Goods', '11111111-1111-1111-1111-111111111111'::uuid, 'info@officegoods.com', '+1-555-0201', 'https://officegoods.com', '98-7654321', 'ACTIVE', 'VERIFIED', 'STANDARD', 'NET60', 'USD', 'Chicago', 'IL', 'USA', '60601', 0, NOW(), NOW());

-- Insert Warehouses
INSERT INTO warehouses (id, tenant_id, warehouse_code, name, organization_id, vendor_id, type, address_line1, city, state, country, postal_code, latitude, longitude, timezone, contact_phone, contact_email, manager_name, capacity_sqft, status, is_temperature_controlled, operating_hours, version, created_at, updated_at)
VALUES
  ('55555555-5555-5555-5555-555555555555'::uuid, '99999999-9999-9999-9999-999999999999'::uuid, 'WH001', 'Main Distribution Center', '11111111-1111-1111-1111-111111111111'::uuid, '33333333-3333-3333-3333-333333333333'::uuid, 'DISTRIBUTION', '789 Warehouse Road', 'Dallas', 'TX', 'USA', '75201', 32.7767, -96.7970, 'America/Chicago', '+1-555-0300', 'warehouse@acme.com', 'John Warehouse', 50000, 'ACTIVE', false, 'Mon-Fri 8AM-6PM', 0, NOW(), NOW()),
  ('66666666-6666-6666-6666-666666666666'::uuid, '99999999-9999-9999-9999-999999999999'::uuid, 'WH002', 'West Coast Fulfillment', '11111111-1111-1111-1111-111111111111'::uuid, '33333333-3333-3333-3333-333333333333'::uuid, 'FULFILLMENT', '456 Logistics Lane', 'Seattle', 'WA', 'USA', '98101', 47.6062, -122.3321, 'America/Los_Angeles', '+1-555-0301', 'seattle@acme.com', 'Jane Manager', 35000, 'ACTIVE', true, 'Mon-Sat 7AM-7PM', 0, NOW(), NOW());

---
# application-test.yml - Test configuration

spring:
  datasource:
    url: jdbc:tc:postgresql:16-alpine:///testdb
    driver-class-name: org.testcontainers.jdbc.ContainerDatabaseDriver
  
  jpa:
    hibernate:
      ddl-auto: create-drop
    show-sql: true
  
  flyway:
    enabled: false

minio:
  endpoint: http://localhost:9000
  access-key: minioadmin
  secret-key: minioadmin
  bucket-name: test-bucket
  document-expiry-days: 1

logging:
  level:
    com.iscm.vendor: DEBUG
    org.testcontainers: INFO