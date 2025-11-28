package com.iscm.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.iscm.dtos.request.UploadDocumentRequest;
import com.iscm.dtos.response.DocumentResponse;
import com.iscm.entity.enums.DocumentStatus;
import com.iscm.service.DocumentService;

import java.io.InputStream;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/documents")
@RequiredArgsConstructor
@Tag(name = "Documents", description = "Document management APIs")
@SecurityRequirement(name = "bearer-jwt")
public class DocumentController {

    private final DocumentService documentService;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload vendor document")
    public ResponseEntity<DocumentResponse> uploadDocument(
            @RequestParam("vendorId") UUID vendorId,
            @RequestParam("file") MultipartFile file,
            @RequestParam("type") String type,
            @RequestParam(value = "expiryDate", required = false) LocalDate expiryDate,
            @RequestParam(value = "notes", required = false) String notes) {

        UUID tenantId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID uploadedBy = UUID.fromString("00000000-0000-0000-0000-000000000002");

        UploadDocumentRequest request = new UploadDocumentRequest();
        request.setType(type);
        request.setExpiryDate(expiryDate);
        request.setNotes(notes);

        DocumentResponse response = documentService.uploadDocument(
                vendorId, file, request, tenantId, uploadedBy);

        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/vendor/{vendorId}")
    @Operation(summary = "Get all documents for a vendor")
    public ResponseEntity<List<DocumentResponse>> getVendorDocuments(
            @PathVariable UUID vendorId) {

        UUID tenantId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        return ResponseEntity.ok(documentService.getVendorDocuments(vendorId, tenantId));
    }

    @GetMapping("/{documentId}")
    @Operation(summary = "Get document by ID")
    public ResponseEntity<DocumentResponse> getDocumentById(
            @PathVariable UUID documentId) {

        UUID tenantId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        return ResponseEntity.ok(documentService.getDocumentById(documentId, tenantId));
    }

    @GetMapping("/{documentId}/download-url")
    @Operation(summary = "Get presigned download URL")
    public ResponseEntity<Map<String, String>> getDownloadUrl(
            @PathVariable UUID documentId) {

        UUID tenantId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        String url = documentService.getDownloadUrl(documentId, tenantId);
        return ResponseEntity.ok(Map.of("downloadUrl", url));
    }

    @GetMapping("/{documentId}/download")
    @Operation(summary = "Download document directly")
    public ResponseEntity<Resource> downloadDocument(
            @PathVariable UUID documentId) {

        UUID tenantId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        DocumentResponse document = documentService.getDocumentById(documentId, tenantId);
        InputStream inputStream = documentService.downloadDocument(documentId, tenantId);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + document.getDocumentName() + "\"")
                .contentType(MediaType.parseMediaType(document.getMimeType()))
                .body(new InputStreamResource(inputStream));
    }

    @PatchMapping("/{documentId}/status")
    @Operation(summary = "Update document status")
    public ResponseEntity<DocumentResponse> updateDocumentStatus(
            @PathVariable UUID documentId,
            @RequestParam String status) {

        UUID tenantId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID verifiedBy = UUID.fromString("00000000-0000-0000-0000-000000000002");

        DocumentStatus docStatus = DocumentStatus.valueOf(status.toUpperCase());

        DocumentResponse response = documentService.updateDocumentStatus(
                documentId, docStatus, tenantId, verifiedBy);

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{documentId}")
    @Operation(summary = "Delete document")
    public ResponseEntity<Void> deleteDocument(
            @PathVariable UUID documentId) {

        UUID tenantId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        documentService.deleteDocument(documentId, tenantId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/expiring")
    @Operation(summary = "Get expiring documents")
    public ResponseEntity<List<DocumentResponse>> getExpiringDocuments(
            @RequestParam LocalDate beforeDate) {

        UUID tenantId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        return ResponseEntity.ok(documentService.getExpiringDocuments(tenantId, beforeDate));
    }

    @GetMapping("/status/{status}")
    @Operation(summary = "Get documents by status")
    public ResponseEntity<List<DocumentResponse>> getDocumentsByStatus(
            @PathVariable String status) {

        UUID tenantId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        DocumentStatus docStatus = DocumentStatus.valueOf(status.toUpperCase());
        return ResponseEntity.ok(documentService.getDocumentsByStatus(docStatus, tenantId));
    }
}
