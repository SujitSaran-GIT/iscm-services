package com.iscm.entity;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.iscm.entity.enums.OrgStatus;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "organizations", indexes = {
    @Index(name = "idx_org_tenant", columnList = "tenant_id"),
    @Index(name = "idx_org_domain", columnList = "domain")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder(toBuilder = true)
public class Organization extends BaseEntity {
    
    @Column(nullable = false, length = 200)
    private String name;
    
    @Column(unique = true, length = 100)
    private String domain;
    
    @Column(name = "parent_org_id")
    private UUID parentOrgId;
    
    @Column(length = 500)
    private String description;
    
    @Column(length = 20)
    private String phone;
    
    @Column(length = 100)
    private String email;
    
    @Column(length = 200)
    private String addressLine1;
    
    @Column(length = 200)
    private String addressLine2;
    
    @Column(length = 100)
    private String city;
    
    @Column(length = 100)
    private String state;
    
    @Column(length = 50)
    private String country;
    
    @Column(length = 20)
    private String postalCode;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private OrgStatus status = OrgStatus.ACTIVE;
    
    @OneToMany(mappedBy = "organization", cascade = CascadeType.ALL)
    @Builder.Default
    private List<Vendor> vendors = new ArrayList<>();
    
    @OneToMany(mappedBy = "organization", cascade = CascadeType.ALL)
    @Builder.Default
    private List<Warehouse> warehouses = new ArrayList<>();
    
    
}