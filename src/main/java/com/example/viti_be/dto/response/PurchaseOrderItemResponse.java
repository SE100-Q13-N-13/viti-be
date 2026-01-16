package com.example.viti_be.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
public class PurchaseOrderItemResponse {
    private UUID id;
    private ProductVariantInfo productVariant;
    private PartComponentResponse partComponent;
    private Integer quantityOrdered;
    private Integer quantityReceived;
    private BigDecimal unitPrice;
    private BigDecimal subtotal;
    private Integer warrantyPeriod;
    private UUID referenceTicketId;
    
    @Data
    @Builder
    public static class ProductVariantInfo {
        private UUID id;
        private String sku;
        private String variantName;
        private String productName;
        private BigDecimal purchasePriceAvg;
        private BigDecimal sellingPrice;
    }
}
