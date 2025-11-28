// TEMPORARILY COMMENTED OUT - KAFKA DISABLED
/*
package com.iscm.events;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VendorOnboardedEvent {
    private UUID vendorId;
    private String vendorCode;
    private String legalName;
    private UUID organizationId;
    private String status;
    private UUID tenantId;

    @Builder.Default
    private Instant timestamp = Instant.now();

    @Builder.Default
    private String eventType = "VendorOnboarded";

    @Builder.Default
    private UUID eventId = UUID.randomUUID();
}
*/