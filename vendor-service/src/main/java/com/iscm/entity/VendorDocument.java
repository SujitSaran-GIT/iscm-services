package com.iscm.entity;

import java.time.LocalDate;
import java.util.UUID;

import com.iscm.entity.enums.DocumentStatus;
import com.iscm.entity.enums.DocumentType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "vendor_documents", indexes = {
    @Index(name = "idx_doc_vendor", columnList = "vendor_id"),
    @Index(name = "idx_doc_status", columnList = "status")
})

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VendorDocument extends BaseEntity {
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vendor_id", nullable = false)
    private Vendor vendor;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private DocumentType type;
    
    @Column(name = "document_name", nullable = false, length = 200)
    private String documentName;
    
    @Column(name = "s3_key", nullable = false, length = 500)
    private String s3Key;
    
    @Column(name = "s3_bucket", nullable = false, length = 100)
    private String s3Bucket;
    
    @Column(name = "file_size")
    private Long fileSize; // in bytes
    
    @Column(name = "mime_type", length = 100)
    private String mimeType;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private DocumentStatus status = DocumentStatus.PENDING_REVIEW;
    
    @Column(name = "expiry_date")
    private LocalDate expiryDate;
    
    @Column(name = "uploaded_by")
    private UUID uploadedBy;
    
    @Column(name = "verified_by")
    private UUID verifiedBy;
    
    @Column(name = "verified_at")
    private LocalDate verifiedAt;
    
    @Column(length = 500)
    private String notes;
    
}