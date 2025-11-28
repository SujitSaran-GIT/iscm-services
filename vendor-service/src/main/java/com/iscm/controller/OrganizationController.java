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

import com.iscm.dtos.request.CreateOrganizationRequest;
import com.iscm.dtos.response.OrganizationResponse;
import com.iscm.service.OrganizationService;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/organizations")
@RequiredArgsConstructor
@Tag(name = "Organizations", description = "Organization management APIs")
@SecurityRequirement(name = "bearer-jwt")
public class OrganizationController {

    private final OrganizationService organizationService;

    @PostMapping
    @Operation(summary = "Create a new organization")
    public ResponseEntity<OrganizationResponse> createOrganization(
            @Valid @RequestBody CreateOrganizationRequest request) {

        UUID tenantId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        OrganizationResponse response = organizationService.createOrganization(request, tenantId);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get organization by ID")
    public ResponseEntity<OrganizationResponse> getOrganizationById(
            @PathVariable UUID id) {

        UUID tenantId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        OrganizationResponse response = organizationService.getOrganizationById(id, tenantId);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @Operation(summary = "Get all organizations with pagination")
    public ResponseEntity<Page<OrganizationResponse>> getAllOrganizations(
            @PageableDefault(size = 20) Pageable pageable) {

        UUID tenantId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        Page<OrganizationResponse> response = organizationService.getAllOrganizations(tenantId, pageable);
        return ResponseEntity.ok(response);
    }
}
