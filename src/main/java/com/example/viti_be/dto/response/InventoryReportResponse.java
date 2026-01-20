package com.example.viti_be.dto.response;

import lombok.*;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryReportResponse {

    private Integer totalProducts;
    private Integer lowStockProducts;
    private Integer outOfStockProducts;

    private List<InventoryItem> items;
    private List<FastMovingItem> fastMovingItems;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class InventoryItem {
        private UUID productVariantId;
        private String productName;
        private String variantName;
        private String sku;
        private Integer quantityPhysical;
        private Integer quantityReserved;
        private Integer quantityAvailable;
        private Integer minThreshold;
        private Boolean isLowStock;
        private Boolean isOutOfStock;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class FastMovingItem {
        private UUID productVariantId;
        private String productName;
        private String variantName;
        private String sku;
        private Integer quantitySold;
        private Integer daysSinceFirstSale;
        private Double averageDailySales;
    }
}