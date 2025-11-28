package com.iscm.service;

import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.http.Method;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.iscm.dtos.request.UploadDocumentRequest;
import com.iscm.dtos.response.DocumentResponse;
import com.iscm.entity.Vendor;
import com.iscm.entity.VendorDocument;
import com.iscm.entity.enums.DocumentStatus;
import com.iscm.entity.enums.DocumentType;
import com.iscm.exception.DocumentUploadException;
import com.iscm.exception.VendorNotFoundException;
import com.iscm.repository.VendorDocumentRepository;
import com.iscm.repository.VendorRepository;

import java.io.InputStream;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentService {
    
    private final MinioClient minioClient;
    private final VendorDocumentRepository documentRepository;
    private final VendorRepository vendorRepository;
    
    @Value("${minio.bucket-name}")
    private String bucketName;
    
    @Value("${minio.document-expiry-days:7}")
    private int documentExpiryDays;
    
    @Transactional
    public DocumentResponse uploadDocument(
            UUID vendorId,
            MultipartFile file,
            UploadDocumentRequest request,
            UUID tenantId,
            UUID uploadedBy) {
        
        log.info("Uploading document for vendor: {}", vendorId);
        
        // Validate vendor exists
        Vendor vendor = vendorRepository.findById(vendorId)
            .filter(v -> v.getTenantId().equals(tenantId))
            .orElseThrow(() -> new VendorNotFoundException("Vendor not found with ID: " + vendorId));
        
        // Validate file
        if (file.isEmpty()) {
            throw new DocumentUploadException("File is empty");
        }
        
        // Generate unique S3 key
        String fileExtension = getFileExtension(file.getOriginalFilename());
        String s3Key = generateS3Key(vendorId, request.getType(), fileExtension);
        
        try {
            // Ensure bucket exists
            ensureBucketExists();
            
            // Upload to MinIO
            uploadToMinIO(s3Key, file);
            
            // Save document metadata
            VendorDocument document = VendorDocument.builder()
                .vendor(vendor)
                .type(DocumentType.valueOf(request.getType()))
                .documentName(file.getOriginalFilename())
                .s3Key(s3Key)
                .s3Bucket(bucketName)
                .fileSize(file.getSize())
                .mimeType(file.getContentType())
                .status(DocumentStatus.PENDING_REVIEW)
                .expiryDate(request.getExpiryDate())
                .uploadedBy(uploadedBy)
                .notes(request.getNotes())
                .build();
            
            document.setTenantId(tenantId);
            
            VendorDocument savedDocument = documentRepository.save(document);
            log.info("Document uploaded successfully with ID: {}", savedDocument.getId());
            
            return toDocumentResponse(savedDocument);
            
        } catch (Exception e) {
            log.error("Failed to upload document", e);
            throw new DocumentUploadException("Failed to upload document: " + e.getMessage(), e);
        }
    }
    
    @Transactional(readOnly = true)
    public List<DocumentResponse> getVendorDocuments(UUID vendorId, UUID tenantId) {
        log.debug("Fetching documents for vendor: {}", vendorId);
        
        return documentRepository.findByVendorIdAndTenantId(vendorId, tenantId)
            .stream()
            .map(this::toDocumentResponse)
            .collect(Collectors.toList());
    }
    
    @Transactional(readOnly = true)
    public DocumentResponse getDocumentById(UUID documentId, UUID tenantId) {
        log.debug("Fetching document with ID: {}", documentId);
        
        VendorDocument document = documentRepository.findById(documentId)
            .filter(d -> d.getTenantId().equals(tenantId))
            .orElseThrow(() -> new IllegalArgumentException("Document not found with ID: " + documentId));
        
        return toDocumentResponse(document);
    }
    
    @Transactional(readOnly = true)
    public String getDownloadUrl(UUID documentId, UUID tenantId) {
        log.debug("Generating download URL for document: {}", documentId);
        
        VendorDocument document = documentRepository.findById(documentId)
            .filter(d -> d.getTenantId().equals(tenantId))
            .orElseThrow(() -> new IllegalArgumentException("Document not found with ID: " + documentId));
        
        try {
            // Generate presigned URL valid for configured days
            return minioClient.getPresignedObjectUrl(
                GetPresignedObjectUrlArgs.builder()
                    .method(Method.GET)
                    .bucket(bucketName)
                    .object(document.getS3Key())
                    .expiry(documentExpiryDays, TimeUnit.DAYS)
                    .build()
            );
        } catch (Exception e) {
            log.error("Failed to generate download URL", e);
            throw new DocumentUploadException("Failed to generate download URL: " + e.getMessage(), e);
        }
    }
    
    @Transactional(readOnly = true)
    public InputStream downloadDocument(UUID documentId, UUID tenantId) {
        log.debug("Downloading document: {}", documentId);
        
        VendorDocument document = documentRepository.findById(documentId)
            .filter(d -> d.getTenantId().equals(tenantId))
            .orElseThrow(() -> new IllegalArgumentException("Document not found with ID: " + documentId));
        
        try {
            return minioClient.getObject(
                GetObjectArgs.builder()
                    .bucket(bucketName)
                    .object(document.getS3Key())
                    .build()
            );
        } catch (Exception e) {
            log.error("Failed to download document", e);
            throw new DocumentUploadException("Failed to download document: " + e.getMessage(), e);
        }
    }
    
    @Transactional
    public DocumentResponse updateDocumentStatus(
            UUID documentId,
            DocumentStatus status,
            UUID tenantId,
            UUID verifiedBy) {
        
        log.info("Updating document status for ID: {} to: {}", documentId, status);
        
        VendorDocument document = documentRepository.findById(documentId)
            .filter(d -> d.getTenantId().equals(tenantId))
            .orElseThrow(() -> new IllegalArgumentException("Document not found with ID: " + documentId));
        
        document.setStatus(status);
        
        if (status == DocumentStatus.APPROVED) {
            document.setVerifiedBy(verifiedBy);
            document.setVerifiedAt(LocalDate.now());
        }
        
        VendorDocument updatedDocument = documentRepository.save(document);
        return toDocumentResponse(updatedDocument);
    }
    
    @Transactional
    public void deleteDocument(UUID documentId, UUID tenantId) {
        log.info("Deleting document with ID: {}", documentId);
        
        VendorDocument document = documentRepository.findById(documentId)
            .filter(d -> d.getTenantId().equals(tenantId))
            .orElseThrow(() -> new IllegalArgumentException("Document not found with ID: " + documentId));
        
        try {
            // Delete from MinIO
            minioClient.removeObject(
                RemoveObjectArgs.builder()
                    .bucket(bucketName)
                    .object(document.getS3Key())
                    .build()
            );
            
            // Delete from database
            documentRepository.delete(document);
            log.info("Document deleted successfully: {}", documentId);
            
        } catch (Exception e) {
            log.error("Failed to delete document", e);
            throw new DocumentUploadException("Failed to delete document: " + e.getMessage(), e);
        }
    }
    
    @Transactional(readOnly = true)
    public List<DocumentResponse> getExpiringDocuments(UUID tenantId, LocalDate beforeDate) {
        log.debug("Fetching documents expiring before: {}", beforeDate);
        
        return documentRepository.findExpiringDocuments(tenantId, beforeDate)
            .stream()
            .map(this::toDocumentResponse)
            .collect(Collectors.toList());
    }
    
    @Transactional(readOnly = true)
    public List<DocumentResponse> getDocumentsByStatus(
            DocumentStatus status,
            UUID tenantId) {
        
        log.debug("Fetching documents with status: {}", status);
        
        return documentRepository.findByTenantIdAndStatus(tenantId, status)
            .stream()
            .map(this::toDocumentResponse)
            .collect(Collectors.toList());
    }
    
    // Private helper methods
    
    private void ensureBucketExists() throws Exception {
        boolean exists = minioClient.bucketExists(
            BucketExistsArgs.builder()
                .bucket(bucketName)
                .build()
        );
        
        if (!exists) {
            log.info("Creating MinIO bucket: {}", bucketName);
            minioClient.makeBucket(
                MakeBucketArgs.builder()
                    .bucket(bucketName)
                    .build()
            );
        }
    }
    
    private void uploadToMinIO(String s3Key, MultipartFile file) throws Exception {
        minioClient.putObject(
            PutObjectArgs.builder()
                .bucket(bucketName)
                .object(s3Key)
                .stream(file.getInputStream(), file.getSize(), -1)
                .contentType(file.getContentType())
                .build()
        );
    }
    
    private String generateS3Key(UUID vendorId, String docType, String extension) {
        return String.format("vendors/%s/documents/%s/%s%s",
            vendorId,
            docType.toLowerCase(),
            UUID.randomUUID(),
            extension);
    }
    
    private String getFileExtension(String filename) {
        if (filename == null || filename.isEmpty()) {
            return "";
        }
        int lastDotIndex = filename.lastIndexOf('.');
        return lastDotIndex > 0 ? filename.substring(lastDotIndex) : "";
    }
    
    private DocumentResponse toDocumentResponse(VendorDocument document) {
        return DocumentResponse.builder()
            .id(document.getId())
            .vendorId(document.getVendor().getId())
            .vendorName(document.getVendor().getDisplayName())
            .type(document.getType().name())
            .documentName(document.getDocumentName())
            .fileSize(document.getFileSize())
            .mimeType(document.getMimeType())
            .status(document.getStatus().name())
            .expiryDate(document.getExpiryDate())
            .uploadedBy(document.getUploadedBy())
            .verifiedBy(document.getVerifiedBy())
            .verifiedAt(document.getVerifiedAt())
            .notes(document.getNotes())
            .createdAt(document.getCreatedAt())
            .updatedAt(document.getUpdatedAt())
            .build();
    }
}
