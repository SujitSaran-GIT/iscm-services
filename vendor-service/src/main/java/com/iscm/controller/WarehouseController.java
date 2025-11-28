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

import com.iscm.dtos.request.CreateWarehouseRequest;
import com.iscm.dtos.response.WarehouseResponse;
import com.iscm.entity.enums.WarehouseStatus;
import com.iscm.service.WarehouseService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/warehouses")
@RequiredArgsConstructor
@Tag(name = "Warehouses", description = "Warehouse management APIs")
@SecurityRequirement(name = "bearer-jwt")
public class WarehouseController {

    private final WarehouseService warehouseService;

    @PostMapping
    @Operation(summary = "Create a new warehouse")
    public ResponseEntity<WarehouseResponse> createWarehouse(
            @Valid @RequestBody CreateWarehouseRequest request) {

        UUID tenantId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        WarehouseResponse response = warehouseService.createWarehouse(request, tenantId);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get warehouse by ID")
    public ResponseEntity<WarehouseResponse> getWarehouseById(@PathVariable UUID id) {

        UUID tenantId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        WarehouseResponse response = warehouseService.getWarehouseById(id, tenantId);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @Operation(summary = "Get all warehouses")
    public ResponseEntity<Page<WarehouseResponse>> getAllWarehouses(
            @PageableDefault(size = 20) Pageable pageable) {

        UUID tenantId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        Page<WarehouseResponse> response = warehouseService.getAllWarehouses(tenantId, pageable);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/vendor/{vendorId}")
    @Operation(summary = "Warehouses by vendor")
    public ResponseEntity<List<WarehouseResponse>> getWarehousesByVendor(@PathVariable UUID vendorId) {

        UUID tenantId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        List<WarehouseResponse> response = warehouseService.getWarehousesByVendor(vendorId, tenantId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/country/{country}")
    @Operation(summary = "Warehouses by country")
    public ResponseEntity<List<WarehouseResponse>> getWarehousesByCountry(@PathVariable String country) {

        UUID tenantId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        List<WarehouseResponse> response = warehouseService.getWarehousesByCountry(country, tenantId);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Update warehouse status")
    public ResponseEntity<WarehouseResponse> updateWarehouseStatus(
            @PathVariable UUID id,
            @RequestParam String status) {

        UUID tenantId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        WarehouseStatus statusEnum = WarehouseStatus.valueOf(status.toUpperCase());
        WarehouseResponse response = warehouseService.updateWarehouseStatus(id, statusEnum, tenantId);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete warehouse")
    public ResponseEntity<Void> deleteWarehouse(@PathVariable UUID id) {

        UUID tenantId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        warehouseService.deleteWarehouse(id, tenantId);
        return ResponseEntity.noContent().build();
    }
}
