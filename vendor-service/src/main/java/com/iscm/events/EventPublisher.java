// package com.iscm.events;

// import com.fasterxml.jackson.core.JsonProcessingException;
// import com.fasterxml.jackson.databind.ObjectMapper;
// import lombok.RequiredArgsConstructor;
// import lombok.extern.slf4j.Slf4j;
// import org.springframework.kafka.core.KafkaTemplate;
// import org.springframework.stereotype.Component;

// @Component
// @RequiredArgsConstructor
// @Slf4j
// public class EventPublisher {
    
//     private static final String VENDOR_EVENTS_TOPIC = "vendor.events";
    
//     private final KafkaTemplate<String, String> kafkaTemplate;
//     private final ObjectMapper objectMapper;
    
//     public void publishVendorOnboarded(VendorOnboardedEvent event) {
//         try {
//             String eventJson = objectMapper.writeValueAsString(event);
//             kafkaTemplate.send(VENDOR_EVENTS_TOPIC, event.getVendorId().toString(), eventJson)
//                 .whenComplete((result, ex) -> {
//                     if (ex == null) {
//                         log.info("Published VendorOnboarded event for vendor: {}", event.getVendorId());
//                     } else {
//                         log.error("Failed to publish VendorOnboarded event", ex);
//                     }
//                 });
//         } catch (JsonProcessingException e) {
//             log.error("Error serializing VendorOnboarded event", e);
//         }
//     }
    
//     public void publishWarehouseCreated(WarehouseCreatedEvent event) {
//         try {
//             String eventJson = objectMapper.writeValueAsString(event);
//             kafkaTemplate.send(VENDOR_EVENTS_TOPIC, event.getWarehouseId().toString(), eventJson)
//                 .whenComplete((result, ex) -> {
//                     if (ex == null) {
//                         log.info("Published WarehouseCreated event for warehouse: {}", event.getWarehouseId());
//                     } else {
//                         log.error("Failed to publish WarehouseCreated event", ex);
//                     }
//                 });
//         } catch (JsonProcessingException e) {
//             log.error("Error serializing WarehouseCreated event", e);
//         }
//     }
// }
