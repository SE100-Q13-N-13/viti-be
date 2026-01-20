package com.example.viti_be.service.impl;

import com.example.viti_be.dto.request.InventoryReportRequest;
import com.example.viti_be.dto.request.ReportFilterRequest;
import com.example.viti_be.dto.response.*;
import com.example.viti_be.mapper.ReportMapper;
import com.example.viti_be.model.model_enum.*;
import com.example.viti_be.repository.InventoryRepository;
import com.example.viti_be.repository.OrderRepository;
import com.example.viti_be.repository.projection.*;
import com.example.viti_be.service.ExportService;
import com.example.viti_be.service.ReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ReportServiceImpl implements ReportService {

    private final OrderRepository orderRepository;
    private final InventoryRepository inventoryRepository;
    private final ReportMapper reportMapper;
    private final ExportService exportService;

    // ============================================
    // REVENUE REPORT
    // ============================================

    @Override
    @Cacheable(value = "revenueReport", key = "#filter.toString()", unless = "#result == null")
    public RevenueReportResponse getRevenueReport(ReportFilterRequest filter) {
        log.info("Generating revenue report for period: {} to {}", filter.getStartDate(), filter.getEndDate());

        LocalDateTime startDateTime = filter.getStartDate().atStartOfDay();
        LocalDateTime endDateTime = filter.getEndDate().atTime(LocalTime.MAX);

        // Get breakdown by period
        List<RevenueReportResponse.RevenueByPeriod> breakdown = getRevenueBreakdown(
                startDateTime,
                endDateTime,
                filter
        );

        // Calculate total revenue
        BigDecimal totalRevenue = breakdown.stream()
                .map(RevenueReportResponse.RevenueByPeriod::getRevenue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Long totalOrders = breakdown.stream()
                .map(RevenueReportResponse.RevenueByPeriod::getOrderCount)
                .reduce(0L, Long::sum);

        // Get breakdown by order type
        List<RevenueByTypeProjection> byTypeProjections = orderRepository.getRevenueByOrderType(
                startDateTime,
                endDateTime,
                filter.getOrderStatus(),
                filter.getCustomerTierId() != null ?
                        UUID.fromString(filter.getCustomerTierId().toString()) : null
        );

        List<RevenueReportResponse.RevenueByType> byOrderType = byTypeProjections.stream()
                .map(reportMapper::toRevenueByType)
                .collect(Collectors.toList());

        // Calculate comparison with previous period
        RevenueReportResponse.ComparisonData comparison = calculateComparison(
                filter.getStartDate(),
                filter.getEndDate(),
                totalRevenue,
                filter
        );

        return RevenueReportResponse.builder()
                .totalRevenue(totalRevenue)
                .totalOrders(totalOrders)
                .breakdown(breakdown)
                .byOrderType(byOrderType)
                .comparison(comparison)
                .build();
    }

    private List<RevenueReportResponse.RevenueByPeriod> getRevenueBreakdown(
            LocalDateTime startDateTime,
            LocalDateTime endDateTime,
            ReportFilterRequest filter) {

        GroupBy groupBy = filter.getGroupBy() != null ? filter.getGroupBy() : GroupBy.DAY;

        return switch (groupBy) {
            case DAY -> {
                List<RevenueProjection> dayProjections = orderRepository.getRevenueByDay(
                        startDateTime,
                        endDateTime,
                        filter.getOrderStatus(),
                        filter.getOrderType(),
                        filter.getPaymentMethod(),
                        filter.getEmployeeId(),
                        filter.getCustomerTierId() != null ?
                                UUID.fromString(filter.getCustomerTierId().toString()) : null
                );
                yield dayProjections.stream()
                        .map(reportMapper::toRevenueByPeriod)
                        .collect(Collectors.toList());
            }
            case WEEK -> {
                List<Object[]> weekRows = orderRepository.getRevenueByWeekNative(
                        startDateTime,
                        endDateTime,
                        filter.getOrderStatus() != null ? filter.getOrderStatus().name() : null,
                        filter.getOrderType() != null ? filter.getOrderType().name() : null,
                        filter.getPaymentMethod() != null ? filter.getPaymentMethod().name() : null,
                        filter.getEmployeeId() != null ? filter.getEmployeeId().toString() : null,
                        filter.getCustomerTierId() != null ? filter.getCustomerTierId().toString() : null
                );
                yield weekRows.stream()
                        .map(reportMapper::toRevenueByPeriodFromNative)
                        .collect(Collectors.toList());
            }
            case MONTH -> {
                List<Object[]> monthRows = orderRepository.getRevenueByMonthNative(
                        startDateTime,
                        endDateTime,
                        filter.getOrderStatus() != null ? filter.getOrderStatus().name() : null,
                        filter.getOrderType() != null ? filter.getOrderType().name() : null,
                        filter.getPaymentMethod() != null ? filter.getPaymentMethod().name() : null,
                        filter.getEmployeeId() != null ? filter.getEmployeeId().toString() : null,
                        filter.getCustomerTierId() != null ? filter.getCustomerTierId().toString() : null
                );
                yield monthRows.stream()
                        .map(reportMapper::toRevenueByPeriodFromNative)
                        .collect(Collectors.toList());
            }
            default -> throw new IllegalArgumentException("Invalid groupBy value: " + groupBy);
        };
    }

    private RevenueReportResponse.ComparisonData calculateComparison(
            LocalDate startDate,
            LocalDate endDate,
            BigDecimal currentRevenue,
            ReportFilterRequest filter) {

        // Calculate previous period
        long daysBetween = ChronoUnit.DAYS.between(startDate, endDate) + 1;
        LocalDate previousStartDate = startDate.minusDays(daysBetween);
        LocalDate previousEndDate = startDate.minusDays(1);

        LocalDateTime previousStartDateTime = previousStartDate.atStartOfDay();
        LocalDateTime previousEndDateTime = previousEndDate.atTime(LocalTime.MAX);

        // Get previous period revenue
        List<RevenueProjection> previousProjections = orderRepository.getRevenueByDay(
                previousStartDateTime,
                previousEndDateTime,
                filter.getOrderStatus(),
                filter.getOrderType(),
                filter.getPaymentMethod(),
                filter.getEmployeeId(),
                filter.getCustomerTierId() != null ?
                        UUID.fromString(filter.getCustomerTierId().toString()) : null
        );

        BigDecimal previousRevenue = previousProjections.stream()
                .map(RevenueProjection::getTotalRevenue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return reportMapper.buildComparison(currentRevenue, previousRevenue);
    }

    // ============================================
    // PROFIT REPORT
    // ============================================

    @Override
    @Cacheable(value = "profitReport", key = "#filter.toString()", unless = "#result == null")
    public ProfitReportResponse getProfitReport(ReportFilterRequest filter) {
        log.info("Generating profit report for period: {} to {}", filter.getStartDate(), filter.getEndDate());

        LocalDateTime startDateTime = filter.getStartDate().atStartOfDay();
        LocalDateTime endDateTime = filter.getEndDate().atTime(LocalTime.MAX);

        List<ProfitProjection> projections = orderRepository.getProfitByProduct(
                startDateTime,
                endDateTime,
                filter.getOrderStatus(),
                filter.getCategoryId(),
                filter.getSupplierId()
        );

        List<ProfitReportResponse.ProfitByProduct> details = projections.stream()
                .map(reportMapper::toProfitByProduct)
                .collect(Collectors.toList());

        // Calculate totals
        BigDecimal totalRevenue = details.stream()
                .map(ProfitReportResponse.ProfitByProduct::getRevenue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalCost = details.stream()
                .map(ProfitReportResponse.ProfitByProduct::getCost)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal grossProfit = totalRevenue.subtract(totalCost);

        double profitMargin = 0.0;
        if (totalRevenue.compareTo(BigDecimal.ZERO) > 0) {
            profitMargin = grossProfit
                    .divide(totalRevenue, 4, java.math.RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .doubleValue();
        }

        return ProfitReportResponse.builder()
                .totalRevenue(totalRevenue)
                .totalCost(totalCost)
                .grossProfit(grossProfit)
                .profitMargin(profitMargin)
                .details(details)
                .build();
    }

    // ============================================
    // TOP PRODUCTS REPORT
    // ============================================

    @Override
    @Cacheable(value = "topProductsReport", key = "#filter.toString()", unless = "#result == null")
    public TopProductsReportResponse getTopProductsReport(ReportFilterRequest filter) {
        log.info("Generating top products report for period: {} to {}",
                filter.getStartDate(), filter.getEndDate());

        LocalDateTime startDateTime = filter.getStartDate().atStartOfDay();
        LocalDateTime endDateTime = filter.getEndDate().atTime(LocalTime.MAX);

        SortBy sortBy = filter.getSortBy() != null ? filter.getSortBy() : SortBy.REVENUE;

        List<TopProductProjection> projections;

        if (sortBy == SortBy.QUANTITY) {
            projections = orderRepository.getTopProductsByQuantity(
                    startDateTime,
                    endDateTime,
                    filter.getOrderStatus(),
                    filter.getCategoryId()
            );
        } else {
            projections = orderRepository.getTopProductsByRevenue(
                    startDateTime,
                    endDateTime,
                    filter.getOrderStatus(),
                    filter.getCategoryId()
            );
        }

        // Apply limit
        int limit = filter.getLimit() != null ? filter.getLimit() : 10;

        AtomicInteger rank = new AtomicInteger(1);
        List<TopProductsReportResponse.TopProduct> topProducts = projections.stream()
                .limit(limit)
                .map(p -> reportMapper.toTopProduct(p, rank.getAndIncrement()))
                .collect(Collectors.toList());

        return TopProductsReportResponse.builder()
                .topProducts(topProducts)
                .build();
    }

    // ============================================
    // INVENTORY REPORT
    // ============================================

    @Override
    public InventoryReportResponse getInventoryReport(InventoryReportRequest filter, Pageable pageable) {
        log.info("Generating inventory report");

        Boolean lowStockOnly = filter.getLowStockOnly() != null ? filter.getLowStockOnly() : false;

        // Get inventory items
        Page<InventoryProjection> inventoryPage = inventoryRepository.getInventoryReport(
                filter.getCategoryId(),
                lowStockOnly,
                pageable
        );

        List<InventoryReportResponse.InventoryItem> items = inventoryPage.getContent().stream()
                .map(reportMapper::toInventoryItem)
                .collect(Collectors.toList());

        // Get statistics
        Long lowStockCount = inventoryRepository.countLowStockProducts();
        Long outOfStockCount = inventoryRepository.countOutOfStockProducts();

        // Get fast-moving items if requested
        List<InventoryReportResponse.FastMovingItem> fastMovingItems = null;

        if (filter.getFastMovingOnly() != null && filter.getFastMovingOnly()) {
            LocalDateTime startDate = filter.getStartDate() != null
                    ? filter.getStartDate().atStartOfDay()
                    : LocalDateTime.now().minusDays(30); // Default: Last 30 days

            List<Object[]> fastMovingRows = inventoryRepository.getFastMovingProductsNative(
                    startDate,
                    filter.getCategoryId() != null ? filter.getCategoryId().toString() : null
            );

            fastMovingItems = fastMovingRows.stream()
                    .limit(20) // Top 20 fast-moving
                    .map(reportMapper::toFastMovingItem)
                    .collect(Collectors.toList());
        }

        return InventoryReportResponse.builder()
                .totalProducts((int) inventoryPage.getTotalElements())
                .lowStockProducts(lowStockCount.intValue())
                .outOfStockProducts(outOfStockCount.intValue())
                .items(items)
                .fastMovingItems(fastMovingItems)
                .build();
    }

    @Override
    public ExportReportResponse exportInventoryReport(
            InventoryReportRequest filter,
            ExportFormat format,
            Pageable pageable) {

        log.info("Exporting inventory report in {} format", format);

        InventoryReportResponse reportData = getInventoryReport(filter, pageable);

        return exportService.exportReport(reportData, "INVENTORY", format);
    }

    // Keep existing exportReport for Revenue/Profit/TopProducts
    @Override
    public ExportReportResponse exportReport(
            ReportFilterRequest filter,
            String reportType,
            ExportFormat format) {

        log.info("Exporting {} report in {} format", reportType, format);

        Object reportData = switch (reportType.toUpperCase()) {
            case "REVENUE" -> getRevenueReport(filter);
            case "PROFIT" -> getProfitReport(filter);
            case "TOP_PRODUCTS" -> getTopProductsReport(filter);
            default -> throw new IllegalArgumentException("Invalid report type: " + reportType);
        };

        return exportService.exportReport(reportData, reportType, format);
    }
}