package com.example.viti_be.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class InventoryResponse {
    private UUID id;
    private ProductVariantInfo productVariant;
    private Integer quantityPhysical;
    private Integer quantityReserved;
    private Integer quantityAvailable;
    private Integer minThreshold;
    private LocalDateTime lastCountedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    @Data
    @Builder
    public static class ProductVariantInfo {
        private UUID id;
        private String sku;
        private String variantName;
        private String productName;
    }
}
