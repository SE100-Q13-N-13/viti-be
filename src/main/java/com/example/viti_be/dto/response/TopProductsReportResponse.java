package com.example.viti_be.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TopProductsReportResponse {

    private List<TopProduct> topProducts;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TopProduct {
        private Integer rank;
        private UUID productVariantId;
        private String productName;
        private String variantName;
        private String sku;
        private Integer quantitySold;
        private BigDecimal totalRevenue;
    }
}