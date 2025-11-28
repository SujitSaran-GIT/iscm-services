CREATE TABLE IF NOT EXISTS organizations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    name VARCHAR(200) NOT NULL,
    domain VARCHAR(100) UNIQUE,
    parent_org_id UUID,
    description VARCHAR(500),
    phone VARCHAR(20),
    email VARCHAR(100),
    address_line1 VARCHAR(200),
    address_line2 VARCHAR(200),
    city VARCHAR(100),
    state VARCHAR(100),
    country VARCHAR(50),
    postal_code VARCHAR(20),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_parent_org FOREIGN KEY (parent_org_id) REFERENCES organizations(id) ON DELETE SET NULL
);

CREATE INDEX idx_org_tenant ON organizations(tenant_id);
CREATE INDEX idx_org_domain ON organizations(domain);
CREATE INDEX idx_org_status ON organizations(status);