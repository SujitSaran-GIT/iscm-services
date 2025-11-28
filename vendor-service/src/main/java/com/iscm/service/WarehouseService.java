package com.iscm.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.iscm.dtos.request.CreateWarehouseRequest;
import com.iscm.dtos.response.WarehouseResponse;
import com.iscm.entity.Organization;
import com.iscm.entity.Vendor;
import com.iscm.entity.Warehouse;
import com.iscm.entity.enums.WarehouseStatus;
import com.iscm.entity.enums.WarehouseType;
// import com.iscm.events.EventPublisher;
// import com.iscm.events.WarehouseCreatedEvent;
import com.iscm.exception.VendorNotFoundException;
import com.iscm.mapper.WarehouseMapper;
import com.iscm.repository.OrganizationRepository;
import com.iscm.repository.VendorRepository;
import com.iscm.repository.WarehouseRepository;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class WarehouseService {
    
    private final WarehouseRepository warehouseRepository;
    private final OrganizationRepository organizationRepository;
    private final VendorRepository vendorRepository;
    private final WarehouseMapper warehouseMapper;
    // private final EventPublisher eventPublisher;
    
    @Transactional
    public WarehouseResponse createWarehouse(CreateWarehouseRequest request, UUID tenantId) {
        log.info("Creating warehouse with code: {} for tenant: {}", request.getWarehouseCode(), tenantId);
        
        // Check if warehouse code already exists
        if (warehouseRepository.existsByWarehouseCodeAndTenantId(request.getWarehouseCode(), tenantId)) {
            throw new IllegalArgumentException("Warehouse with code " + request.getWarehouseCode() + " already exists");
        }
        
        // Validate organization exists
        Organization organization = organizationRepository.findByIdAndTenantId(request.getOrganizationId(), tenantId)
            .orElseThrow(() -> new IllegalArgumentException("Organization not found"));
        
        // Validate vendor if provided
        Vendor vendor = null;
        if (request.getVendorId() != null) {
            vendor = vendorRepository.findById(request.getVendorId())
                .filter(v -> v.getTenantId().equals(tenantId))
                .orElseThrow(() -> new VendorNotFoundException("Vendor not found"));
        }
        
        // Build warehouse entity
        Warehouse warehouse = Warehouse.builder()
            .warehouseCode(request.getWarehouseCode())
            .name(request.getName())
            .organization(organization)
            .vendor(vendor)
            .type(parseWarehouseType(request.getType()))
            .addressLine1(request.getAddressLine1())
            .addressLine2(request.getAddressLine2())
            .city(request.getCity())
            .state(request.getState())
            .country(request.getCountry())
            .postalCode(request.getPostalCode())
            .latitude(request.getLatitude())
            .longitude(request.getLongitude())
            .timezone(request.getTimezone())
            .contactPhone(request.getContactPhone())
            .contactEmail(request.getContactEmail())
            .managerName(request.getManagerName())
            .capacitySqft(request.getCapacitySqft())
            .status(WarehouseStatus.ACTIVE)
            .isTemperatureControlled(request.getIsTemperatureControlled() != null ? request.getIsTemperatureControlled() : false)
            .operatingHours(request.getOperatingHours())
            .build();
        
        warehouse.setTenantId(tenantId);
        
        Warehouse savedWarehouse = warehouseRepository.save(warehouse);
        log.info("Warehouse created successfully with ID: {}", savedWarehouse.getId());
        
        // Publish event
        // eventPublisher.publishWarehouseCreated(
        //     WarehouseCreatedEvent.builder()
        //         .warehouseId(savedWarehouse.getId())
        //         .warehouseCode(savedWarehouse.getWarehouseCode())
        //         .name(savedWarehouse.getName())
        //         .organizationId(savedWarehouse.getOrganization().getId())
        //         .vendorId(savedWarehouse.getVendor() != null ? savedWarehouse.getVendor().getId() : null)
        //         .country(savedWarehouse.getCountry())
        //         .city(savedWarehouse.getCity())
        //         .tenantId(tenantId)
        //         .build()
        // );
        
        return warehouseMapper.toResponse(savedWarehouse);
    }
    
    @Transactional(readOnly = true)
    public WarehouseResponse getWarehouseById(UUID id, UUID tenantId) {
        log.debug("Fetching warehouse with ID: {} for tenant: {}", id, tenantId);
        
        Warehouse warehouse = warehouseRepository.findById(id)
            .filter(w -> w.getTenantId().equals(tenantId))
            .orElseThrow(() -> new IllegalArgumentException("Warehouse not found with ID: " + id));
        
        return warehouseMapper.toResponse(warehouse);
    }
    
    @Transactional(readOnly = true)
    public Page<WarehouseResponse> getAllWarehouses(UUID tenantId, Pageable pageable) {
        log.debug("Fetching all warehouses for tenant: {}", tenantId);
        
        return warehouseRepository.findByTenantId(tenantId, pageable)
            .map(warehouseMapper::toResponse);
    }
    
    @Transactional(readOnly = true)
    public List<WarehouseResponse> getWarehousesByVendor(UUID vendorId, UUID tenantId) {
        log.debug("Fetching warehouses for vendor: {} and tenant: {}", vendorId, tenantId);
        
        return warehouseRepository.findByVendorIdAndTenantId(vendorId, tenantId)
            .stream()
            .map(warehouseMapper::toResponse)
            .collect(Collectors.toList());
    }
    
    @Transactional(readOnly = true)
    public List<WarehouseResponse> getWarehousesByCountry(String country, UUID tenantId) {
        log.debug("Fetching warehouses in country: {} for tenant: {}", country, tenantId);
        
        return warehouseRepository.findByCountry(tenantId, country)
            .stream()
            .map(warehouseMapper::toResponse)
            .collect(Collectors.toList());
    }
    
    @Transactional
    public WarehouseResponse updateWarehouseStatus(UUID id, WarehouseStatus status, UUID tenantId) {
        log.info("Updating warehouse status for ID: {} to: {}", id, status);
        
        Warehouse warehouse = warehouseRepository.findById(id)
            .filter(w -> w.getTenantId().equals(tenantId))
            .orElseThrow(() -> new IllegalArgumentException("Warehouse not found with ID: " + id));
        
        warehouse.setStatus(status);
        
        Warehouse updatedWarehouse = warehouseRepository.save(warehouse);
        return warehouseMapper.toResponse(updatedWarehouse);
    }
    
    @Transactional
    public void deleteWarehouse(UUID id, UUID tenantId) {
        log.info("Deleting warehouse with ID: {} for tenant: {}", id, tenantId);
        
        Warehouse warehouse = warehouseRepository.findById(id)
            .filter(w -> w.getTenantId().equals(tenantId))
            .orElseThrow(() -> new IllegalArgumentException("Warehouse not found with ID: " + id));
        
        warehouseRepository.delete(warehouse);
        log.info("Warehouse deleted successfully with ID: {}", id);
    }
    
    private WarehouseType parseWarehouseType(String type) {
        if (type == null) {
            return WarehouseType.DISTRIBUTION;
        }
        try {
            return WarehouseType.valueOf(type.toUpperCase());
        } catch (IllegalArgumentException e) {
            return WarehouseType.DISTRIBUTION;
        }
    }
}