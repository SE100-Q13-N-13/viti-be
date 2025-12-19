package com.example.viti_be.dto.response;

import com.example.viti_be.model.model_enum.ProductSerialStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class ProductSerialResponse {
    private UUID id;
    private String serialNumber;
    private ProductSerialStatus status;
    private ProductVariantInfo productVariant;
    private UUID purchaseOrderId;
    private UUID orderId;
    private LocalDateTime soldDate;
    private LocalDateTime warrantyExpireDate;
    
    @Data
    @Builder
    public static class ProductVariantInfo {
        private UUID id;
        private String sku;
        private String variantName;
        private String productName;
    }
}
