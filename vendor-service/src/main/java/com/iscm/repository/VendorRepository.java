package com.iscm.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.iscm.entity.Vendor;
import com.iscm.entity.enums.KycStatus;
import com.iscm.entity.enums.VendorStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface VendorRepository extends JpaRepository<Vendor, UUID> {
    
    Optional<Vendor> findByVendorCodeAndTenantId(String vendorCode, UUID tenantId);
    
    boolean existsByVendorCodeAndTenantId(String vendorCode, UUID tenantId);
    
    Page<Vendor> findByTenantId(UUID tenantId, Pageable pageable);
    
    Page<Vendor> findByTenantIdAndStatus(UUID tenantId, VendorStatus status, Pageable pageable);
    
    @Query("SELECT v FROM Vendor v WHERE v.tenantId = :tenantId AND " +
           "(LOWER(v.legalName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(v.displayName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(v.vendorCode) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Vendor> searchVendors(@Param("tenantId") UUID tenantId, @Param("search") String search, Pageable pageable);
    
    List<Vendor> findByOrganizationIdAndTenantId(UUID organizationId, UUID tenantId);
    
    @Query("SELECT v FROM Vendor v WHERE v.tenantId = :tenantId AND v.kycStatus = :kycStatus")
    List<Vendor> findByKycStatus(@Param("tenantId") UUID tenantId, @Param("kycStatus") KycStatus kycStatus);
}
