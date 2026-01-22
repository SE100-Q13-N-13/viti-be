package com.example.viti_be.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryOverviewResponse {

    // ========== SUMMARY STATISTICS ==========
    private BigDecimal totalStockValue;
    private Long lowStockProductCount;
    private Long lowStockComponentCount;

    // ========== CHART DATA ==========
    private List<StockValueChartData> stockValueChartData;
    private List<OnHandQuantityChartData> onHandQuantityChartData;

    // ========== LOW STOCK LISTS ==========
    private List<LowStockProductItem> lowStockProducts;
    private List<LowStockComponentItem> lowStockComponents;

    /**
     * Chart data for Stock Value over time
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class StockValueChartData {
        private String label;           // e.g., "Week 1", "Jan", "2024"
        private BigDecimal stockValue;  // Total stock value at that period
    }

    /**
     * Chart data for On-hand Quantity over time
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class OnHandQuantityChartData {
        private String label;           // e.g., "Week 1", "Jan", "2024"
        private Long productQuantity;   // Total product quantity
        private Long componentQuantity; // Total component quantity
    }

    /**
     * Low stock product item
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class LowStockProductItem {
        private UUID productVariantId;
        private String productName;
        private String variantName;
        private String sku;
        private Integer currentStock;
        private Integer minThreshold;
        private BigDecimal unitPrice;
        private String imageUrl;
    }

    /**
     * Low stock component item
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class LowStockComponentItem {
        private UUID partComponentId;
        private String name;
        private String partType;
        private String unit;
        private Integer currentStock;
        private Integer minStock;
        private BigDecimal purchasePriceAvg;
    }
}
