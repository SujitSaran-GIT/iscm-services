package com.iscm.dtos.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDate;

@Data
public class UploadDocumentRequest {
    
    @NotBlank(message = "Document type is required")
    private String type; // TAX_CERTIFICATE, BUSINESS_LICENSE, etc.
    
    private LocalDate expiryDate;
    
    private String notes;
}