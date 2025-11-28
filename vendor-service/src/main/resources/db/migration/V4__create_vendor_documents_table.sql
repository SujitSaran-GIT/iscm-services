CREATE TABLE IF NOT EXISTS vendor_documents (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    vendor_id UUID NOT NULL,
    type VARCHAR(50) NOT NULL,
    document_name VARCHAR(200) NOT NULL,
    s3_key VARCHAR(500) NOT NULL,
    s3_bucket VARCHAR(100) NOT NULL,
    file_size BIGINT,
    mime_type VARCHAR(100),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING_REVIEW',
    expiry_date DATE,
    uploaded_by UUID,
    verified_by UUID,
    verified_at DATE,
    notes VARCHAR(500),
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_document_vendor FOREIGN KEY (vendor_id) REFERENCES vendors(id) ON DELETE CASCADE
);

CREATE INDEX idx_doc_vendor ON vendor_documents(vendor_id);
CREATE INDEX idx_doc_status ON vendor_documents(status);
CREATE INDEX idx_doc_type ON vendor_documents(type);
CREATE INDEX idx_doc_expiry ON vendor_documents(expiry_date);
CREATE INDEX idx_doc_tenant ON vendor_documents(tenant_id);