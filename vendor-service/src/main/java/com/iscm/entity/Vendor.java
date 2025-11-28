package com.iscm.entity;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import com.iscm.entity.enums.KycStatus;
import com.iscm.entity.enums.SlaTier;
import com.iscm.entity.enums.VendorStatus;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;


@Entity
@Table(name = "vendors", indexes = {
    @Index(name = "idx_vendor_tenant", columnList = "tenant_id"),
    @Index(name = "idx_vendor_code", columnList = "vendor_code"),
    @Index(name = "idx_vendor_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder(toBuilder = true)
public class Vendor extends BaseEntity {
    
    @Column(name = "vendor_code", unique = true, nullable = false, length = 50)
    private String vendorCode;
    
    @Column(name = "legal_name", nullable = false, length = 300)
    private String legalName;
    
    @Column(name = "display_name", nullable = false, length = 200)
    private String displayName;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;
    
    @Column(length = 100)
    private String email;
    
    @Column(length = 20)
    private String phone;
    
    @Column(length = 100)
    private String website;
    
    @Column(length = 50)
    private String taxId;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private VendorStatus status = VendorStatus.PENDING;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "kyc_status", nullable = false, length = 20)
    @Builder.Default
    private KycStatus kycStatus = KycStatus.NOT_STARTED;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "sla_tier", length = 20)
    @Builder.Default
    private SlaTier slaTier = SlaTier.STANDARD;
    
    @Column(name = "contract_start_date")
    private LocalDate contractStartDate;
    
    @Column(name = "contract_end_date")
    private LocalDate contractEndDate;
    
    @Column(name = "payment_terms", length = 50)
    private String paymentTerms; // e.g., "NET30", "NET60"
    
    @Column(name = "currency", columnDefinition = "bpchar")
    private String currency; // ISO currency code
    
    @Column(length = 1000)
    private String notes;
    
    // Address fields
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
    
    @OneToMany(mappedBy = "vendor", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<VendorDocument> documents = new ArrayList<>();
    
    @OneToMany(mappedBy = "vendor", cascade = CascadeType.ALL)
    @Builder.Default
    private List<Warehouse> warehouses = new ArrayList<>();
    
    
}