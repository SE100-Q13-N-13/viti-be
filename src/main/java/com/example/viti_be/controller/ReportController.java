package com.example.viti_be.controller;

import com.example.viti_be.dto.request.InventoryReportRequest;
import com.example.viti_be.dto.request.ReportFilterRequest;
import com.example.viti_be.dto.response.*;
import com.example.viti_be.model.model_enum.ExportFormat;
import com.example.viti_be.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
@Tag(name = "Reports", description = "Report Management APIs")
public class ReportController {

    private final ReportService reportService;

    private static final Set<String> VALID_INVENTORY_SORT_FIELDS = Set.of(
            "quantityPhysical",
            "quantityReserved",
            "quantityAvailable",
            "minThreshold"
    );

    // ============================================
    // REVENUE REPORT
    // ============================================

    @GetMapping("/revenue")
    @PreAuthorize("hasRole('ADMIN') or hasRole('ACCOUNTANT')")
    @Operation(summary = "Get revenue report with breakdown and comparison")
    public ResponseEntity<RevenueReportResponse> getRevenueReport(
            @Valid @ModelAttribute ReportFilterRequest filter) {

        RevenueReportResponse response = reportService.getRevenueReport(filter);
        return ResponseEntity.ok(response);
    }

    // ============================================
    // PROFIT REPORT
    // ============================================

    @GetMapping("/profit")
    @PreAuthorize("hasRole('ADMIN') or hasRole('ACCOUNTANT')")
    @Operation(summary = "Get profit report by products")
    public ResponseEntity<ProfitReportResponse> getProfitReport(
            @Valid @ModelAttribute ReportFilterRequest filter) {

        ProfitReportResponse response = reportService.getProfitReport(filter);
        return ResponseEntity.ok(response);
    }

    // ============================================
    // TOP PRODUCTS REPORT
    // ============================================

    @GetMapping("/top-products")
    @PreAuthorize("hasRole('ADMIN') or hasRole('ACCOUNTANT')")
    @Operation(summary = "Get top products report (by quantity or revenue)")
    public ResponseEntity<TopProductsReportResponse> getTopProductsReport(
            @Valid @ModelAttribute ReportFilterRequest filter) {

        TopProductsReportResponse response = reportService.getTopProductsReport(filter);
        return ResponseEntity.ok(response);
    }

    // ============================================
    // INVENTORY REPORT
    // ============================================

    @GetMapping("/inventory")
    @PreAuthorize("hasAnyRole('ADMIN', 'ACCOUNTANT', 'WAREHOUSE')")
    @Operation(
            summary = "Get inventory report with low stock alerts",
            description = "Valid sortBy fields: quantityPhysical, quantityReserved, quantityAvailable, minThreshold"
    )
    @Parameter(name = "sortBy", description = "Field to sort by",
            schema = @Schema(allowableValues = {"quantityPhysical", "quantityReserved", "quantityAvailable", "minThreshold"}))
    public ResponseEntity<InventoryReportResponse> getInventoryReport(
            @Valid @ModelAttribute InventoryReportRequest filter,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(defaultValue = "quantityAvailable") String sortBy,
            @RequestParam(defaultValue = "ASC") String direction) {

        // Validate sortBy parameter
        if (!VALID_INVENTORY_SORT_FIELDS.contains(sortBy)) {
            throw new IllegalArgumentException(
                    String.format("Invalid sortBy field: %s. Valid fields are: %s",
                            sortBy, String.join(", ", VALID_INVENTORY_SORT_FIELDS))
            );
        }

        Sort.Direction sortDirection = Sort.Direction.fromString(direction);
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sortBy));

        InventoryReportResponse response = reportService.getInventoryReport(filter, pageable);
        return ResponseEntity.ok(response);
    }

    // ============================================
    // EXPORT REPORT
    // ============================================

    @PostMapping("/export")
    @PreAuthorize("hasAnyRole('ADMIN', 'ACCOUNTANT')")
    @Operation(summary = "Export report to CSV or PDF (Revenue, Profit, Top Products)")
    public ResponseEntity<ExportReportResponse> exportReport(
            @Valid @RequestBody ReportFilterRequest filter,
            @RequestParam String type, // REVENUE, PROFIT, TOP_PRODUCTS
            @RequestParam(defaultValue = "CSV") ExportFormat format) {

        ExportReportResponse response = reportService.exportReport(filter, type, format);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/export/inventory")
    @PreAuthorize("hasAnyRole('ADMIN', 'ACCOUNTANT', 'WAREHOUSE')")
    @Operation(summary = "Export inventory report to CSV or PDF")
    public ResponseEntity<ExportReportResponse> exportInventoryReport(
            @Valid @RequestBody InventoryReportRequest filter,
            @RequestParam(defaultValue = "CSV") ExportFormat format,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "1000") int size,
            @RequestParam(defaultValue = "quantityAvailable") String sortBy,
            @RequestParam(defaultValue = "ASC") String direction) {

        // Validate sortBy
        if (!VALID_INVENTORY_SORT_FIELDS.contains(sortBy)) {
            throw new IllegalArgumentException("Invalid sortBy field: " + sortBy);
        }

        Sort.Direction sortDirection = Sort.Direction.fromString(direction);
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sortBy));

        ExportReportResponse response = reportService.exportInventoryReport(filter, format, pageable);
        return ResponseEntity.ok(response);
    }
}