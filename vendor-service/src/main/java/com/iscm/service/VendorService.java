package com.iscm.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.iscm.dtos.request.CreateVendorRequest;
import com.iscm.dtos.request.UpdateVendorRequest;
import com.iscm.dtos.response.VendorResponse;
import com.iscm.entity.Organization;
import com.iscm.entity.Vendor;
import com.iscm.entity.enums.KycStatus;
import com.iscm.entity.enums.SlaTier;
import com.iscm.entity.enums.VendorStatus;
// import com.iscm.events.EventPublisher;
// import com.iscm.events.VendorOnboardedEvent;
import com.iscm.exception.VendorNotFoundException;
import com.iscm.mapper.VendorMapper;
import com.iscm.repository.OrganizationRepository;
import com.iscm.repository.VendorRepository;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class VendorService {
    
    private final VendorRepository vendorRepository;
    private final OrganizationRepository organizationRepository;
    private final VendorMapper vendorMapper;
    // private final EventPublisher eventPublisher;
    
    @Transactional
    public VendorResponse createVendor(CreateVendorRequest request, UUID tenantId) {
        log.info("Creating vendor with code: {} for tenant: {}", request.getVendorCode(), tenantId);
        
        // Check if vendor code already exists
        if (vendorRepository.existsByVendorCodeAndTenantId(request.getVendorCode(), tenantId)) {
            throw new IllegalArgumentException("Vendor with code " + request.getVendorCode() + " already exists");
        }
        
        // Validate organization exists
        Organization organization = organizationRepository.findByIdAndTenantId(request.getOrganizationId(), tenantId)
            .orElseThrow(() -> new IllegalArgumentException("Organization not found"));
        
        // Build vendor entity
        Vendor vendor = Vendor.builder()
            .vendorCode(request.getVendorCode())
            .legalName(request.getLegalName())
            .displayName(request.getDisplayName())
            .organization(organization)
            .email(request.getEmail())
            .phone(request.getPhone())
            .website(request.getWebsite())
            .taxId(request.getTaxId())
            .status(VendorStatus.PENDING)
            .kycStatus(KycStatus.NOT_STARTED)
            .slaTier(parseSlaTier(request.getSlaTier()))
            .contractStartDate(request.getContractStartDate())
            .contractEndDate(request.getContractEndDate())
            .paymentTerms(request.getPaymentTerms())
            .currency(request.getCurrency())
            .addressLine1(request.getAddressLine1())
            .addressLine2(request.getAddressLine2())
            .city(request.getCity())
            .state(request.getState())
            .country(request.getCountry())
            .postalCode(request.getPostalCode())
            .notes(request.getNotes())
            .build();
        
        vendor.setTenantId(tenantId);
        
        Vendor savedVendor = vendorRepository.save(vendor);
        log.info("Vendor created successfully with ID: {}", savedVendor.getId());
        
        // Publish event
        // eventPublisher.publishVendorOnboarded(
        //     VendorOnboardedEvent.builder()
        //         .vendorId(savedVendor.getId())
        //         .vendorCode(savedVendor.getVendorCode())
        //         .legalName(savedVendor.getLegalName())
        //         .organizationId(savedVendor.getOrganization().getId())
        //         .status(savedVendor.getStatus().name())
        //         .tenantId(tenantId)
        //         .build()
        // );
        
        return vendorMapper.toResponse(savedVendor);
    }
    
    @Transactional(readOnly = true)
    public VendorResponse getVendorById(UUID id, UUID tenantId) {
        log.debug("Fetching vendor with ID: {} for tenant: {}", id, tenantId);
        
        Vendor vendor = vendorRepository.findById(id)
            .filter(v -> v.getTenantId().equals(tenantId))
            .orElseThrow(() -> new VendorNotFoundException("Vendor not found with ID: " + id));
        
        return vendorMapper.toResponse(vendor);
    }
    
    @Transactional(readOnly = true)
    public VendorResponse getVendorByCode(String vendorCode, UUID tenantId) {
        log.debug("Fetching vendor with code: {} for tenant: {}", vendorCode, tenantId);
        
        Vendor vendor = vendorRepository.findByVendorCodeAndTenantId(vendorCode, tenantId)
            .orElseThrow(() -> new VendorNotFoundException("Vendor not found with code: " + vendorCode));
        
        return vendorMapper.toResponse(vendor);
    }
    
    @Transactional(readOnly = true)
    public Page<VendorResponse> getAllVendors(UUID tenantId, Pageable pageable) {
        log.debug("Fetching all vendors for tenant: {}", tenantId);
        
        return vendorRepository.findByTenantId(tenantId, pageable)
            .map(vendorMapper::toResponse);
    }
    
    @Transactional(readOnly = true)
    public Page<VendorResponse> searchVendors(String search, UUID tenantId, Pageable pageable) {
        log.debug("Searching vendors with query: {} for tenant: {}", search, tenantId);
        
        return vendorRepository.searchVendors(tenantId, search, pageable)
            .map(vendorMapper::toResponse);
    }
    
    @Transactional(readOnly = true)
    public Page<VendorResponse> getVendorsByStatus(VendorStatus status, UUID tenantId, Pageable pageable) {
        log.debug("Fetching vendors with status: {} for tenant: {}", status, tenantId);
        
        return vendorRepository.findByTenantIdAndStatus(tenantId, status, pageable)
            .map(vendorMapper::toResponse);
    }
    
    @Transactional
    public VendorResponse updateVendor(UUID id, UpdateVendorRequest request, UUID tenantId) {
        log.info("Updating vendor with ID: {} for tenant: {}", id, tenantId);
        
        Vendor vendor = vendorRepository.findById(id)
            .filter(v -> v.getTenantId().equals(tenantId))
            .orElseThrow(() -> new VendorNotFoundException("Vendor not found with ID: " + id));
        
        // Update fields if provided
        if (request.getDisplayName() != null) {
            vendor.setDisplayName(request.getDisplayName());
        }
        if (request.getEmail() != null) {
            vendor.setEmail(request.getEmail());
        }
        if (request.getPhone() != null) {
            vendor.setPhone(request.getPhone());
        }
        if (request.getWebsite() != null) {
            vendor.setWebsite(request.getWebsite());
        }
        if (request.getStatus() != null) {
            vendor.setStatus(VendorStatus.valueOf(request.getStatus()));
        }
        if (request.getKycStatus() != null) {
            vendor.setKycStatus(KycStatus.valueOf(request.getKycStatus()));
        }
        if (request.getSlaTier() != null) {
            vendor.setSlaTier(parseSlaTier(request.getSlaTier()));
        }
        if (request.getContractStartDate() != null) {
            vendor.setContractStartDate(request.getContractStartDate());
        }
        if (request.getContractEndDate() != null) {
            vendor.setContractEndDate(request.getContractEndDate());
        }
        if (request.getPaymentTerms() != null) {
            vendor.setPaymentTerms(request.getPaymentTerms());
        }
        if (request.getAddressLine1() != null) {
            vendor.setAddressLine1(request.getAddressLine1());
        }
        if (request.getAddressLine2() != null) {
            vendor.setAddressLine2(request.getAddressLine2());
        }
        if (request.getCity() != null) {
            vendor.setCity(request.getCity());
        }
        if (request.getState() != null) {
            vendor.setState(request.getState());
        }
        if (request.getCountry() != null) {
            vendor.setCountry(request.getCountry());
        }
        if (request.getPostalCode() != null) {
            vendor.setPostalCode(request.getPostalCode());
        }
        if (request.getNotes() != null) {
            vendor.setNotes(request.getNotes());
        }
        
        Vendor updatedVendor = vendorRepository.save(vendor);
        log.info("Vendor updated successfully with ID: {}", updatedVendor.getId());
        
        return vendorMapper.toResponse(updatedVendor);
    }
    
    @Transactional
    public void deleteVendor(UUID id, UUID tenantId) {
        log.info("Deleting vendor with ID: {} for tenant: {}", id, tenantId);
        
        Vendor vendor = vendorRepository.findById(id)
            .filter(v -> v.getTenantId().equals(tenantId))
            .orElseThrow(() -> new VendorNotFoundException("Vendor not found with ID: " + id));
        
        vendorRepository.delete(vendor);
        log.info("Vendor deleted successfully with ID: {}", id);
    }
    
    @Transactional
    public VendorResponse updateKycStatus(UUID id, KycStatus kycStatus, UUID tenantId) {
        log.info("Updating KYC status for vendor ID: {} to: {}", id, kycStatus);
        
        Vendor vendor = vendorRepository.findById(id)
            .filter(v -> v.getTenantId().equals(tenantId))
            .orElseThrow(() -> new VendorNotFoundException("Vendor not found with ID: " + id));
        
        vendor.setKycStatus(kycStatus);
        
        // Auto-activate vendor if KYC is verified
        if (kycStatus == KycStatus.VERIFIED && vendor.getStatus() == VendorStatus.PENDING) {
            vendor.setStatus(VendorStatus.ACTIVE);
        }
        
        Vendor updatedVendor = vendorRepository.save(vendor);
        return vendorMapper.toResponse(updatedVendor);
    }
    
    private SlaTier parseSlaTier(String slaTier) {
        if (slaTier == null) {
            return SlaTier.STANDARD;
        }
        try {
            return SlaTier.valueOf(slaTier.toUpperCase());
        } catch (IllegalArgumentException e) {
            return SlaTier.STANDARD;
        }
    }
}