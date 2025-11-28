CREATE TABLE IF NOT EXISTS warehouses (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    warehouse_code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(200) NOT NULL,
    organization_id UUID NOT NULL,
    vendor_id UUID,
    type VARCHAR(20) NOT NULL DEFAULT 'DISTRIBUTION',
    address_line1 VARCHAR(200),
    address_line2 VARCHAR(200),
    city VARCHAR(100),
    state VARCHAR(100),
    country VARCHAR(50) NOT NULL,
    postal_code VARCHAR(20),
    latitude DECIMAL(10, 7),
    longitude DECIMAL(10, 7),
    timezone VARCHAR(50),
    contact_phone VARCHAR(20),
    contact_email VARCHAR(100),
    manager_name VARCHAR(100),
    capacity_sqft INTEGER,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    is_temperature_controlled BOOLEAN DEFAULT FALSE,
    operating_hours VARCHAR(100),
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_warehouse_organization FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE CASCADE,
    CONSTRAINT fk_warehouse_vendor FOREIGN KEY (vendor_id) REFERENCES vendors(id) ON DELETE SET NULL
);

CREATE INDEX idx_warehouse_tenant ON warehouses(tenant_id);
CREATE INDEX idx_warehouse_code ON warehouses(warehouse_code);
CREATE INDEX idx_warehouse_vendor ON warehouses(vendor_id);
CREATE INDEX idx_warehouse_organization ON warehouses(organization_id);
CREATE INDEX idx_warehouse_geo ON warehouses(latitude, longitude);
CREATE INDEX idx_warehouse_country ON warehouses(country);
CREATE INDEX idx_warehouse_status ON warehouses(status);