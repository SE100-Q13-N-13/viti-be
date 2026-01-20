package com.example.viti_be.service;

import com.example.viti_be.dto.response.ExportReportResponse;
import com.example.viti_be.model.model_enum.ExportFormat;

public interface ExportService {

    /**
     * Export report data to file and upload to Supabase Storage
     */
    ExportReportResponse exportReport(Object reportData, String reportType, ExportFormat format);

    /**
     * Generate CSV from report data
     */
    byte[] generateCSV(Object reportData, String reportType);

    /**
     * Generate PDF from report data
     */
    byte[] generatePDF(Object reportData, String reportType);
}