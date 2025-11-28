package com.iscm.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.iscm.entity.Organization;
import com.iscm.entity.enums.OrgStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrganizationRepository extends JpaRepository<Organization, UUID> {
    
    Optional<Organization> findByIdAndTenantId(UUID id, UUID tenantId);
    
    Optional<Organization> findByDomainAndTenantId(String domain, UUID tenantId);
    
    Page<Organization> findByTenantId(UUID tenantId, Pageable pageable);
    
    List<Organization> findByParentOrgIdAndTenantId(UUID parentOrgId, UUID tenantId);
    
    Page<Organization> findByTenantIdAndStatus(UUID tenantId, OrgStatus status, Pageable pageable);
}
