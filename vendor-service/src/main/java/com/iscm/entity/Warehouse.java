package com.iscm.entity;

import java.math.BigDecimal;

import com.iscm.entity.enums.WarehouseStatus;
import com.iscm.entity.enums.WarehouseType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "warehouses", indexes = {
    @Index(name = "idx_warehouse_tenant", columnList = "tenant_id"),
    @Index(name = "idx_warehouse_code", columnList = "warehouse_code"),
    @Index(name = "idx_warehouse_vendor", columnList = "vendor_id"),
    @Index(name = "idx_warehouse_geo", columnList = "latitude, longitude")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder(toBuilder = true)
public class Warehouse extends BaseEntity {
    
    @Column(name = "warehouse_code", unique = true, nullable = false, length = 50)
    private String warehouseCode;
    
    @Column(nullable = false, length = 200)
    private String name;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vendor_id")
    private Vendor vendor;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private WarehouseType type = WarehouseType.DISTRIBUTION;
    
    @Column(length = 200)
    private String addressLine1;
    
    @Column(length = 200)
    private String addressLine2;
    
    @Column(length = 100)
    private String city;
    
    @Column(length = 100)
    private String state;
    
    @Column(nullable = false, length = 50)
    private String country;
    
    @Column(length = 20)
    private String postalCode;
    
    @Column(precision = 10, scale = 7)
    private BigDecimal latitude;
    
    @Column(precision = 10, scale = 7)
    private BigDecimal longitude;
    
    @Column(length = 50)
    private String timezone; // e.g., "Asia/Kolkata"
    
    @Column(length = 20)
    private String contactPhone;
    
    @Column(length = 100)
    private String contactEmail;
    
    @Column(length = 100)
    private String managerName;
    
    @Column(name = "capacity_sqft")
    private Integer capacitySqft;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private WarehouseStatus status = WarehouseStatus.ACTIVE;
    
    @Column(name = "is_temperature_controlled")
    @Builder.Default
    private Boolean isTemperatureControlled = false;
    
    @Column(name = "operating_hours", length = 100)
    private String operatingHours; // e.g., "Mon-Fri 9AM-6PM"
    
}
