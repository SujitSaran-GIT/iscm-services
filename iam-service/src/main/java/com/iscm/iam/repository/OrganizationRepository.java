package com.iscm.iam.repository;

import com.iscm.iam.model.Organization;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrganizationRepository extends JpaRepository<Organization, UUID> {
    
    Optional<Organization> findByDomain(String domain);
    
    List<Organization> findByParentOrganizationIsNull();
    
    @Query("SELECT o FROM Organization o WHERE o.parentOrganization.id = :parentId")
    List<Organization> findByParentOrganizationId(@Param("parentId") UUID parentId);
    
    Boolean existsByDomain(String domain);
}