package com.iscm.dtos.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class DocumentResponse {
    private UUID id;
    private UUID vendorId;
    private String vendorName;
    private String type;
    private String documentName;
    private Long fileSize;
    private String mimeType;
    private String status;
    private LocalDate expiryDate;
    private UUID uploadedBy;
    private UUID verifiedBy;
    private LocalDate verifiedAt;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
