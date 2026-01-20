package com.example.viti_be.service;

import com.example.viti_be.dto.request.InventoryReportRequest;
import com.example.viti_be.dto.request.ReportFilterRequest;
import com.example.viti_be.dto.response.*;
import com.example.viti_be.model.model_enum.ExportFormat;
import org.springframework.data.domain.Pageable;

public interface ReportService {

    /**
     * Generate revenue report with breakdown and comparison
     */
    RevenueReportResponse getRevenueReport(ReportFilterRequest filter);

    /**
     * Generate profit report by products
     */
    ProfitReportResponse getProfitReport(ReportFilterRequest filter);

    /**
     * Generate top products report
     */
    TopProductsReportResponse getTopProductsReport(ReportFilterRequest filter);

    /**
     * Generate inventory report with low stock alerts
     */
    InventoryReportResponse getInventoryReport(InventoryReportRequest filter, Pageable pageable);

    /**
     * Export report to CSV/PDF and upload to Supabase Storage
     */
    ExportReportResponse exportReport(
            ReportFilterRequest filter,
            String reportType,
            ExportFormat format
    );

    /**
     * Export inventory report (separate method)
     */
    ExportReportResponse exportInventoryReport(
            InventoryReportRequest filter,
            ExportFormat format,
            Pageable pageable
    );
}