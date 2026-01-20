package com.example.viti_be.mapper;

import com.example.viti_be.dto.response.*;
import com.example.viti_be.model.model_enum.SortBy;
import com.example.viti_be.repository.projection.*;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class ReportMapper {

    // ============================================
    // REVENUE MAPPING
    // ============================================

    public RevenueReportResponse.RevenueByPeriod toRevenueByPeriod(RevenueProjection projection) {
        return RevenueReportResponse.RevenueByPeriod.builder()
                .period(projection.getPeriod())
                .revenue(projection.getTotalRevenue())
                .orderCount(projection.getOrderCount())
                .build();
    }

    public RevenueReportResponse.RevenueByPeriod toRevenueByPeriodFromNative(Object[] row) {
        return RevenueReportResponse.RevenueByPeriod.builder()
                .period(((java.sql.Date) row[0]).toLocalDate())
                .revenue((BigDecimal) row[1])
                .orderCount(((Number) row[2]).longValue())
                .build();
    }

    public RevenueReportResponse.RevenueByType toRevenueByType(RevenueByTypeProjection projection) {
        return RevenueReportResponse.RevenueByType.builder()
                .orderType(projection.getOrderType())
                .revenue(projection.getTotalRevenue())
                .orderCount(projection.getOrderCount())
                .build();
    }

    public RevenueReportResponse.ComparisonData buildComparison(
            BigDecimal currentRevenue,
            BigDecimal previousRevenue) {

        BigDecimal difference = currentRevenue.subtract(previousRevenue);
        Double percentageChange = 0.0;

        if (previousRevenue.compareTo(BigDecimal.ZERO) > 0) {
            percentageChange = difference
                    .divide(previousRevenue, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .doubleValue();
        }

        return RevenueReportResponse.ComparisonData.builder()
                .previousRevenue(previousRevenue)
                .difference(difference)
                .percentageChange(percentageChange)
                .build();
    }

    // ============================================
    // PROFIT MAPPING
    // ============================================

    public ProfitReportResponse.ProfitByProduct toProfitByProduct(ProfitProjection projection) {
        Double profitMargin = 0.0;

        if (projection.getTotalRevenue() != null &&
                projection.getTotalRevenue().compareTo(BigDecimal.ZERO) > 0) {
            profitMargin = projection.getGrossProfit()
                    .divide(projection.getTotalRevenue(), 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .doubleValue();
        }

        return ProfitReportResponse.ProfitByProduct.builder()
                .productVariantId(projection.getProductVariantId())
                .productName(projection.getProductName())
                .variantName(projection.getVariantName())
                .sku(projection.getSku())
                .quantitySold(projection.getQuantitySold())
                .revenue(projection.getTotalRevenue())
                .cost(projection.getTotalCost())
                .grossProfit(projection.getGrossProfit())
                .profitMargin(profitMargin)
                .build();
    }

    // ============================================
    // TOP PRODUCTS MAPPING
    // ============================================

    public TopProductsReportResponse.TopProduct toTopProduct(
            TopProductProjection projection,
            Integer rank) {

        return TopProductsReportResponse.TopProduct.builder()
                .rank(rank)
                .productVariantId(projection.getProductVariantId())
                .productName(projection.getProductName())
                .variantName(projection.getVariantName())
                .sku(projection.getSku())
                .quantitySold(projection.getQuantitySold())
                .totalRevenue(projection.getTotalRevenue())
                .build();
    }

    // ============================================
    // INVENTORY MAPPING
    // ============================================

    public InventoryReportResponse.InventoryItem toInventoryItem(InventoryProjection projection) {
        return InventoryReportResponse.InventoryItem.builder()
                .productVariantId(projection.getProductVariantId())
                .productName(projection.getProductName())
                .variantName(projection.getVariantName())
                .sku(projection.getSku())
                .quantityPhysical(projection.getQuantityPhysical())
                .quantityReserved(projection.getQuantityReserved())
                .quantityAvailable(projection.getQuantityAvailable())
                .minThreshold(projection.getMinThreshold())
                .isLowStock(projection.getIsLowStock())
                .isOutOfStock(projection.getQuantityAvailable() == 0)
                .build();
    }

    public InventoryReportResponse.FastMovingItem toFastMovingItem(Object[] row) {
        return InventoryReportResponse.FastMovingItem.builder()
                .productVariantId((java.util.UUID) row[0])
                .productName((String) row[1])
                .variantName((String) row[2])
                .sku((String) row[3])
                .quantitySold(((Number) row[4]).intValue())
                .daysSinceFirstSale(((Number) row[5]).intValue())
                .averageDailySales(((Number) row[6]).doubleValue())
                .build();
    }
}