CREATE TABLE IF NOT EXISTS vendors (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    vendor_code VARCHAR(50) NOT NULL UNIQUE,
    legal_name VARCHAR(300) NOT NULL,
    display_name VARCHAR(200) NOT NULL,
    organization_id UUID NOT NULL,
    email VARCHAR(100),
    phone VARCHAR(20),
    website VARCHAR(100),
    tax_id VARCHAR(50),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    kyc_status VARCHAR(20) NOT NULL DEFAULT 'NOT_STARTED',
    sla_tier VARCHAR(20) DEFAULT 'STANDARD',
    contract_start_date DATE,
    contract_end_date DATE,
    payment_terms VARCHAR(50),
    currency CHAR(3),
    notes TEXT,
    address_line1 VARCHAR(200),
    address_line2 VARCHAR(200),
    city VARCHAR(100),
    state VARCHAR(100),
    country VARCHAR(50),
    postal_code VARCHAR(20),
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_vendor_organization FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE CASCADE
);

CREATE INDEX idx_vendor_tenant ON vendors(tenant_id);
CREATE INDEX idx_vendor_code ON vendors(vendor_code);
CREATE INDEX idx_vendor_status ON vendors(status);
CREATE INDEX idx_vendor_kyc_status ON vendors(kyc_status);
CREATE INDEX idx_vendor_organization ON vendors(organization_id);