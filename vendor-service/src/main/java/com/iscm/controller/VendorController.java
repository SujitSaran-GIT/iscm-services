package com.iscm.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.iscm.dtos.request.CreateVendorRequest;
import com.iscm.dtos.request.UpdateVendorRequest;
import com.iscm.dtos.response.VendorResponse;
import com.iscm.entity.enums.KycStatus;
import com.iscm.entity.enums.VendorStatus;
import com.iscm.service.VendorService;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/vendors")
@RequiredArgsConstructor
@Tag(name = "Vendors", description = "Vendor management APIs")
@SecurityRequirement(name = "bearer-jwt")
public class VendorController {

    private final VendorService vendorService;

    @PostMapping
    @Operation(summary = "Create a new vendor")
    public ResponseEntity<VendorResponse> createVendor(
            @Valid @RequestBody CreateVendorRequest request) {

        UUID tenantId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        VendorResponse response = vendorService.createVendor(request, tenantId);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get vendor by ID")
    public ResponseEntity<VendorResponse> getVendorById(@PathVariable UUID id) {

        UUID tenantId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        VendorResponse response = vendorService.getVendorById(id, tenantId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/code/{vendorCode}")
    @Operation(summary = "Get vendor by code")
    public ResponseEntity<VendorResponse> getVendorByCode(@PathVariable String vendorCode) {

        UUID tenantId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        VendorResponse response = vendorService.getVendorByCode(vendorCode, tenantId);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @Operation(summary = "Get all vendors")
    public ResponseEntity<Page<VendorResponse>> getAllVendors(
            @PageableDefault(size = 20) Pageable pageable) {

        UUID tenantId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        Page<VendorResponse> response = vendorService.getAllVendors(tenantId, pageable);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/search")
    @Operation(summary = "Search vendors")
    public ResponseEntity<Page<VendorResponse>> searchVendors(
            @RequestParam String query,
            @PageableDefault(size = 20) Pageable pageable) {

        UUID tenantId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        Page<VendorResponse> response = vendorService.searchVendors(query, tenantId, pageable);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/status/{status}")
    @Operation(summary = "Get vendors by status")
    public ResponseEntity<Page<VendorResponse>> getVendorsByStatus(
            @PathVariable String status,
            @PageableDefault(size = 20) Pageable pageable) {

        UUID tenantId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        VendorStatus vendorStatus = VendorStatus.valueOf(status.toUpperCase());
        Page<VendorResponse> response = vendorService.getVendorsByStatus(vendorStatus, tenantId, pageable);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Update vendor")
    public ResponseEntity<VendorResponse> updateVendor(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateVendorRequest request) {

        UUID tenantId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        VendorResponse response = vendorService.updateVendor(id, request, tenantId);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/kyc-status")
    @Operation(summary = "Update KYC")
    public ResponseEntity<VendorResponse> updateKycStatus(
            @PathVariable UUID id,
            @RequestParam String kycStatus) {

        UUID tenantId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        KycStatus statusEnum = KycStatus.valueOf(kycStatus.toUpperCase());
        VendorResponse response = vendorService.updateKycStatus(id, statusEnum, tenantId);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete vendor")
    public ResponseEntity<Void> deleteVendor(@PathVariable UUID id) {

        UUID tenantId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        vendorService.deleteVendor(id, tenantId);
        return ResponseEntity.noContent().build();
    }
}
