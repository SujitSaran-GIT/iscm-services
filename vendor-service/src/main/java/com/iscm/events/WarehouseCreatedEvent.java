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
public class WarehouseCreatedEvent {
    private UUID warehouseId;
    private String warehouseCode;
    private String name;
    private UUID organizationId;
    private UUID vendorId;
    private String country;
    private String city;
    private UUID tenantId;

    @Builder.Default
    private Instant timestamp = Instant.now();

    @Builder.Default
    private String eventType = "WarehouseCreated";

    @Builder.Default
    private UUID eventId = UUID.randomUUID();
}
*/
