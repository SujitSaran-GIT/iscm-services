package com.iscm.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.iscm.dtos.request.CreateOrganizationRequest;
import com.iscm.dtos.response.OrganizationResponse;
import com.iscm.entity.Organization;
import com.iscm.entity.enums.OrgStatus;
import com.iscm.repository.OrganizationRepository;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrganizationService {
    
    private final OrganizationRepository organizationRepository;
    
    @Transactional
    public OrganizationResponse createOrganization(CreateOrganizationRequest request, UUID tenantId) {
        log.info("Creating organization: {} for tenant: {}", request.getName(), tenantId);
        
        // Check if domain already exists
        if (request.getDomain() != null) {
            organizationRepository.findByDomainAndTenantId(request.getDomain(), tenantId)
                .ifPresent(org -> {
                    throw new IllegalArgumentException("Organization with domain " + request.getDomain() + " already exists");
                });
        }
        
        Organization organization = Organization.builder()
            .name(request.getName())
            .domain(request.getDomain())
            .parentOrgId(request.getParentOrgId())
            .description(request.getDescription())
            .phone(request.getPhone())
            .email(request.getEmail())
            .addressLine1(request.getAddressLine1())
            .addressLine2(request.getAddressLine2())
            .city(request.getCity())
            .state(request.getState())
            .country(request.getCountry())
            .postalCode(request.getPostalCode())
            .status(OrgStatus.ACTIVE)
            .build();
        
        organization.setTenantId(tenantId);
        
        Organization savedOrg = organizationRepository.save(organization);
        log.info("Organization created successfully with ID: {}", savedOrg.getId());
        
        return toResponse(savedOrg);
    }
    
    @Transactional(readOnly = true)
    public OrganizationResponse getOrganizationById(UUID id, UUID tenantId) {
        log.debug("Fetching organization with ID: {} for tenant: {}", id, tenantId);
        
        Organization organization = organizationRepository.findByIdAndTenantId(id, tenantId)
            .orElseThrow(() -> new IllegalArgumentException("Organization not found with ID: " + id));
        
        return toResponse(organization);
    }
    
    @Transactional(readOnly = true)
    public Page<OrganizationResponse> getAllOrganizations(UUID tenantId, Pageable pageable) {
        log.debug("Fetching all organizations for tenant: {}", tenantId);
        
        return organizationRepository.findByTenantId(tenantId, pageable)
            .map(this::toResponse);
    }
    
    private OrganizationResponse toResponse(Organization org) {
        return OrganizationResponse.builder()
            .id(org.getId())
            .name(org.getName())
            .domain(org.getDomain())
            .parentOrgId(org.getParentOrgId())
            .description(org.getDescription())
            .phone(org.getPhone())
            .email(org.getEmail())
            .addressLine1(org.getAddressLine1())
            .addressLine2(org.getAddressLine2())
            .city(org.getCity())
            .state(org.getState())
            .country(org.getCountry())
            .postalCode(org.getPostalCode())
            .status(org.getStatus().name())
            .createdAt(org.getCreatedAt())
            .updatedAt(org.getUpdatedAt())
            .build();
    }
}