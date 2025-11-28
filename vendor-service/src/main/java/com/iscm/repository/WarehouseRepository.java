package com.iscm.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.iscm.entity.Warehouse;
import com.iscm.entity.enums.WarehouseStatus;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface WarehouseRepository extends JpaRepository<Warehouse, UUID> {
    
    Optional<Warehouse> findByWarehouseCodeAndTenantId(String warehouseCode, UUID tenantId);
    
    boolean existsByWarehouseCodeAndTenantId(String warehouseCode, UUID tenantId);
    
    Page<Warehouse> findByTenantId(UUID tenantId, Pageable pageable);
    
    List<Warehouse> findByVendorIdAndTenantId(UUID vendorId, UUID tenantId);
    
    List<Warehouse> findByOrganizationIdAndTenantId(UUID organizationId, UUID tenantId);
    
    Page<Warehouse> findByTenantIdAndStatus(UUID tenantId, WarehouseStatus status, Pageable pageable);
    
    @Query("SELECT w FROM Warehouse w WHERE w.tenantId = :tenantId AND w.country = :country")
    List<Warehouse> findByCountry(@Param("tenantId") UUID tenantId, @Param("country") String country);
    
    // Find warehouses within a radius (using simple bounding box approximation)
    @Query("SELECT w FROM Warehouse w WHERE w.tenantId = :tenantId AND " +
           "w.latitude BETWEEN :minLat AND :maxLat AND " +
           "w.longitude BETWEEN :minLon AND :maxLon")
    List<Warehouse> findNearbyWarehouses(@Param("tenantId") UUID tenantId,@Param("minLat") BigDecimal minLat,@Param("maxLat") BigDecimal maxLat, @Param("minLon") BigDecimal minLon, @Param("maxLon") BigDecimal maxLon);
}