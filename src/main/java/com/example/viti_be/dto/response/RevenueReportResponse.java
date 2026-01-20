package com.example.viti_be.dto.response;

import com.example.viti_be.model.model_enum.OrderType;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RevenueReportResponse {

    private BigDecimal totalRevenue;
    private Long totalOrders;

    // Breakdown by period (day/week/month)
    private List<RevenueByPeriod> breakdown;

    // Breakdown by order type
    private List<RevenueByType> byOrderType;

    // Comparison with previous period
    private ComparisonData comparison;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RevenueByPeriod {
        private LocalDate period;
        private BigDecimal revenue;
        private Long orderCount;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RevenueByType {
        private OrderType orderType;
        private BigDecimal revenue;
        private Long orderCount;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ComparisonData {
        private BigDecimal previousRevenue;
        private BigDecimal difference;
        private Double percentageChange;
    }
}