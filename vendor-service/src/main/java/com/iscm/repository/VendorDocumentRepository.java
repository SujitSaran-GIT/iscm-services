package com.iscm.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.iscm.entity.VendorDocument;
import com.iscm.entity.enums.DocumentStatus;
import com.iscm.entity.enums.DocumentType;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface VendorDocumentRepository extends JpaRepository<VendorDocument, UUID> {
    
    List<VendorDocument> findByVendorIdAndTenantId(UUID vendorId, UUID tenantId);
    
    List<VendorDocument> findByVendorIdAndTypeAndTenantId(UUID vendorId, DocumentType type, UUID tenantId);
    
    @Query("SELECT d FROM VendorDocument d WHERE d.tenantId = :tenantId AND " +
           "d.expiryDate IS NOT NULL AND d.expiryDate <= :date AND d.status = 'APPROVED'")
    List<VendorDocument> findExpiringDocuments(@Param("tenantId") UUID tenantId, @Param("date") LocalDate date);
    
    List<VendorDocument> findByTenantIdAndStatus(UUID tenantId, DocumentStatus status);
}