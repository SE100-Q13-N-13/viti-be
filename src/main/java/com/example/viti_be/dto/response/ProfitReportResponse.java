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
public class ProfitReportResponse {

    private BigDecimal totalRevenue;
    private BigDecimal totalCost;
    private BigDecimal grossProfit;
    private Double profitMargin; // (grossProfit / totalRevenue) * 100

    private List<ProfitByProduct> details;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ProfitByProduct {
        private UUID productVariantId;
        private String productName;
        private String variantName;
        private String sku;
        private Integer quantitySold;
        private BigDecimal revenue;
        private BigDecimal cost;
        private BigDecimal grossProfit;
        private Double profitMargin;
    }
}