package com.iscm.controller;

import io.minio.MinioClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import com.iscm.entity.Organization;
import com.iscm.entity.Vendor;
import com.iscm.repository.OrganizationRepository;
import com.iscm.repository.VendorDocumentRepository;
import com.iscm.repository.VendorRepository;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class DocumentControllerIntegrationTest {
    
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");
    
    @Container
    static GenericContainer<?> minio = new GenericContainer<>(DockerImageName.parse("minio/minio:latest"))
        .withCommand("server /data")
        .withExposedPorts(9000)
        .withEnv("MINIO_ROOT_USER", "minioadmin")
        .withEnv("MINIO_ROOT_PASSWORD", "minioadmin");
    
    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        
        registry.add("minio.endpoint", () -> 
            "http://" + minio.getHost() + ":" + minio.getMappedPort(9000));
        registry.add("minio.access-key", () -> "minioadmin");
        registry.add("minio.secret-key", () -> "minioadmin");
        registry.add("minio.bucket-name", () -> "test-bucket");
    }
    
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private OrganizationRepository organizationRepository;
    
    @Autowired
    private VendorRepository vendorRepository;
    
    @Autowired
    private VendorDocumentRepository documentRepository;
    
    @Autowired
    private MinioClient minioClient;
    
    private UUID tenantId;
    private UUID vendorId;
    
    @BeforeEach
    void setUp() {
        documentRepository.deleteAll();
        vendorRepository.deleteAll();
        organizationRepository.deleteAll();
        
        tenantId = UUID.randomUUID();
        
        Organization org = Organization.builder()
            .name("Test Organization")
            .build();
        org.setTenantId(tenantId);
        Organization savedOrg = organizationRepository.save(org);
        
        Vendor vendor = Vendor.builder()
            .vendorCode("V001")
            .legalName("Test Vendor Ltd")
            .displayName("Test Vendor")
            .organization(savedOrg)
            .build();
        vendor.setTenantId(tenantId);
        Vendor savedVendor = vendorRepository.save(vendor);
        vendorId = savedVendor.getId();
    }
    
    @Test
    // @WithMockUser(roles = "ADMIN")
    void uploadDocument_Success() throws Exception {
        // Given
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "test-document.pdf",
            "application/pdf",
            "PDF content here".getBytes()
        );
        
        // When/Then
        mockMvc.perform(multipart("/api/v1/documents/upload")
                .file(file)
                .param("vendorId", vendorId.toString())
                .param("type", "TAX_CERTIFICATE")
                .param("notes", "Test upload"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.vendorId").value(vendorId.toString()))
            .andExpect(jsonPath("$.type").value("TAX_CERTIFICATE"))
            .andExpect(jsonPath("$.status").value("PENDING_REVIEW"));
    }
    
    @Test
    // @WithMockUser(roles = "ADMIN")
    void getVendorDocuments_Success() throws Exception {
        // When/Then
        mockMvc.perform(get("/api/v1/documents/vendor/" + vendorId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray());
    }
    
    @Test
    // @WithMockUser(roles = "BUYER")
    void uploadDocument_Forbidden() throws Exception {
        // Given
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "test.pdf",
            "application/pdf",
            "content".getBytes()
        );
        
        // When/Then
        mockMvc.perform(multipart("/api/v1/documents/upload")
                .file(file)
                .param("vendorId", vendorId.toString())
                .param("type", "TAX_CERTIFICATE"))
            .andExpect(status().isForbidden());
    }
}