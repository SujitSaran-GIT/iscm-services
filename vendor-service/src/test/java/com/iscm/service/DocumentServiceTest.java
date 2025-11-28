package com.iscm.service;

import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.BucketExistsArgs;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import com.iscm.dtos.request.UploadDocumentRequest;
import com.iscm.dtos.response.DocumentResponse;
import com.iscm.entity.Organization;
import com.iscm.entity.Vendor;
import com.iscm.entity.VendorDocument;
import com.iscm.entity.enums.DocumentStatus;
import com.iscm.entity.enums.DocumentType;
import com.iscm.exception.DocumentUploadException;
import com.iscm.exception.VendorNotFoundException;
import com.iscm.repository.VendorDocumentRepository;
import com.iscm.repository.VendorRepository;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DocumentServiceTest {
    
    @Mock
    private MinioClient minioClient;
    
    @Mock
    private VendorDocumentRepository documentRepository;
    
    @Mock
    private VendorRepository vendorRepository;
    
    @InjectMocks
    private DocumentService documentService;
    
    private UUID tenantId;
    private UUID vendorId;
    private UUID uploadedBy;
    private Vendor vendor;
    
    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        vendorId = UUID.randomUUID();
        uploadedBy = UUID.randomUUID();
        
        ReflectionTestUtils.setField(documentService, "bucketName", "test-bucket");
        ReflectionTestUtils.setField(documentService, "documentExpiryDays", 7);
        
        Organization organization = Organization.builder()
            .name("Test Org")
            .build();
        organization.setId(UUID.randomUUID());

        vendor = Vendor.builder()
            .vendorCode("V001")
            .displayName("Test Vendor")
            .organization(organization)
            .build();
        vendor.setId(vendorId);
        vendor.setTenantId(tenantId);
    }
    
    @Test
    void uploadDocument_Success() throws Exception {
        // Given
        MultipartFile file = new MockMultipartFile(
            "file",
            "test-document.pdf",
            "application/pdf",
            "test content".getBytes()
        );
        
        UploadDocumentRequest request = new UploadDocumentRequest();
        request.setType("TAX_CERTIFICATE");
        request.setExpiryDate(LocalDate.now().plusYears(1));
        request.setNotes("Test notes");
        
        when(vendorRepository.findById(vendorId)).thenReturn(Optional.of(vendor));
        when(minioClient.bucketExists(any(BucketExistsArgs.class))).thenReturn(true);
        
        VendorDocument savedDocument = VendorDocument.builder()
            .vendor(vendor)
            .type(DocumentType.TAX_CERTIFICATE)
            .documentName(file.getOriginalFilename())
            .s3Key("vendors/" + vendorId + "/documents/tax_certificate/test.pdf")
            .s3Bucket("test-bucket")
            .fileSize(file.getSize())
            .mimeType(file.getContentType())
            .status(DocumentStatus.PENDING_REVIEW)
            .build();
        savedDocument.setId(UUID.randomUUID());
        
        when(documentRepository.save(any(VendorDocument.class))).thenReturn(savedDocument);
        
        // When
        DocumentResponse result = documentService.uploadDocument(
            vendorId, file, request, tenantId, uploadedBy);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getVendorId()).isEqualTo(vendorId);
        assertThat(result.getType()).isEqualTo("TAX_CERTIFICATE");
        assertThat(result.getStatus()).isEqualTo("PENDING_REVIEW");
        
        verify(minioClient, atLeastOnce()).putObject(any(PutObjectArgs.class));
        verify(documentRepository).save(any(VendorDocument.class));
    }
    
    @Test
    void uploadDocument_VendorNotFound_ThrowsException() throws Exception {
        // Given
        MultipartFile file = new MockMultipartFile(
            "file",
            "test.pdf",
            "application/pdf",
            "content".getBytes()
        );

        UploadDocumentRequest request = new UploadDocumentRequest();
        request.setType("TAX_CERTIFICATE");

        when(vendorRepository.findById(vendorId)).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> documentService.uploadDocument(
            vendorId, file, request, tenantId, uploadedBy))
            .isInstanceOf(VendorNotFoundException.class);

        // Verify that MinIO operations were not called
        verify(minioClient, never()).putObject(any(PutObjectArgs.class));
    }
    
    @Test
    void uploadDocument_EmptyFile_ThrowsException() throws Exception {
        // Given
        MultipartFile file = new MockMultipartFile(
            "file",
            "test.pdf",
            "application/pdf",
            new byte[0]
        );
        
        UploadDocumentRequest request = new UploadDocumentRequest();
        request.setType("TAX_CERTIFICATE");
        
        when(vendorRepository.findById(vendorId)).thenReturn(Optional.of(vendor));
        
        // When/Then
        assertThatThrownBy(() -> documentService.uploadDocument(
            vendorId, file, request, tenantId, uploadedBy))
            .isInstanceOf(DocumentUploadException.class)
            .hasMessageContaining("empty");
    }
    
    @Test
    void updateDocumentStatus_ToApproved_Success() throws Exception {
        // Given
        UUID documentId = UUID.randomUUID();
        UUID verifiedBy = UUID.randomUUID();
        
        VendorDocument document = VendorDocument.builder()
            .vendor(vendor)
            .type(DocumentType.TAX_CERTIFICATE)
            .status(DocumentStatus.PENDING_REVIEW)
            .build();
        document.setId(documentId);
        document.setTenantId(tenantId);
        
        when(documentRepository.findById(documentId)).thenReturn(Optional.of(document));
        when(documentRepository.save(any(VendorDocument.class))).thenReturn(document);
        
        // When
        DocumentResponse result = documentService.updateDocumentStatus(
            documentId, DocumentStatus.APPROVED, tenantId, verifiedBy);
        
        // Then
        assertThat(result).isNotNull();
        verify(documentRepository).save(argThat(doc -> 
            doc.getStatus() == DocumentStatus.APPROVED &&
            doc.getVerifiedBy().equals(verifiedBy) &&
            doc.getVerifiedAt() != null
        ));
    }
    
    @Test
    void deleteDocument_Success() throws Exception {
        // Given
        UUID documentId = UUID.randomUUID();
        
        VendorDocument document = VendorDocument.builder()
            .vendor(vendor)
            .s3Key("test/key.pdf")
            .s3Bucket("test-bucket")
            .build();
        document.setId(documentId);
        document.setTenantId(tenantId);
        
        when(documentRepository.findById(documentId)).thenReturn(Optional.of(document));
        
        // When
        documentService.deleteDocument(documentId, tenantId);
        
        // Then
        verify(minioClient).removeObject(any());
        verify(documentRepository).delete(document);
    }
}