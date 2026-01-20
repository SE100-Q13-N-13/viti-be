package com.example.viti_be.service.impl;

import com.example.viti_be.dto.response.*;
import com.example.viti_be.model.model_enum.ExportFormat;
import com.example.viti_be.service.ExportService;
import com.example.viti_be.service.SupabaseStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExportServiceImpl implements ExportService {

    private final SupabaseStorageService storageService;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd_HHmmss");

    @Override
    public ExportReportResponse exportReport(Object reportData, String reportType, ExportFormat format) {
        try {
            byte[] fileBytes;
            String fileExtension;
            String contentType;

            if (format == ExportFormat.CSV) {
                fileBytes = generateCSV(reportData, reportType);
                fileExtension = "csv";
                contentType = "text/csv";
            } else if (format == ExportFormat.PDF) {
                fileBytes = generatePDF(reportData, reportType);
                fileExtension = "pdf";
                contentType = "application/pdf";
            } else {
                throw new IllegalArgumentException("Unsupported export format: " + format);
            }

            // Generate file name
            String timestamp = LocalDateTime.now().format(DATETIME_FORMATTER);
            String fileName = String.format("%s_report_%s.%s",
                    reportType.toLowerCase(), timestamp, fileExtension);

            // Upload to Supabase Storage
            String bucketName = "reports";
            String fileUrl = storageService.uploadFile(bucketName, fileName, fileBytes, contentType);

            return ExportReportResponse.builder()
                    .fileUrl(fileUrl)
                    .fileName(fileName)
                    .message("Report exported successfully")
                    .build();

        } catch (Exception e) {
            log.error("Error exporting report: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to export report", e);
        }
    }

    @Override
    public byte[] generateCSV(Object reportData, String reportType) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             OutputStreamWriter osw = new OutputStreamWriter(baos, java.nio.charset.StandardCharsets.UTF_8)) {
            osw.write('\ufeff');

            try (CSVPrinter csvPrinter = new CSVPrinter(osw, CSVFormat.DEFAULT)) {
                switch (reportType.toUpperCase()) {
                    case "REVENUE" -> writeRevenueCSV(csvPrinter, (RevenueReportResponse) reportData);
                    case "PROFIT" -> writeProfitCSV(csvPrinter, (ProfitReportResponse) reportData);
                    case "TOP_PRODUCTS" -> writeTopProductsCSV(csvPrinter, (TopProductsReportResponse) reportData);
                    case "INVENTORY" -> writeInventoryCSV(csvPrinter, (InventoryReportResponse) reportData);
                    default -> throw new IllegalArgumentException("Invalid report type: " + reportType);
                }
                csvPrinter.flush();
            }

            return baos.toByteArray();

        } catch (IOException e) {
            log.error("Error generating CSV: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to generate CSV", e);
        }
    }

    private void writeRevenueCSV(CSVPrinter csv, RevenueReportResponse data) throws IOException {
        // Header
        csv.printRecord("REVENUE REPORT");
        csv.printRecord("Total Revenue", data.getTotalRevenue());
        csv.printRecord("Total Orders", data.getTotalOrders());
        csv.println();

        // Breakdown
        csv.printRecord("Date", "Revenue", "Order Count");
        for (var item : data.getBreakdown()) {
            csv.printRecord(
                    item.getPeriod().format(DATE_FORMATTER),
                    item.getRevenue(),
                    item.getOrderCount()
            );
        }
        csv.println();

        // By Order Type
        csv.printRecord("Order Type", "Revenue", "Order Count");
        for (var item : data.getByOrderType()) {
            csv.printRecord(
                    item.getOrderType(),
                    item.getRevenue(),
                    item.getOrderCount()
            );
        }
    }

    private void writeProfitCSV(CSVPrinter csv, ProfitReportResponse data) throws IOException {
        csv.printRecord("PROFIT REPORT");
        csv.printRecord("Total Revenue", data.getTotalRevenue());
        csv.printRecord("Total Cost", data.getTotalCost());
        csv.printRecord("Gross Profit", data.getGrossProfit());
        csv.printRecord("Profit Margin (%)", data.getProfitMargin());
        csv.println();

        csv.printRecord("Product", "Variant", "SKU", "Quantity Sold", "Revenue", "Cost", "Gross Profit", "Margin (%)");
        for (var item : data.getDetails()) {
            csv.printRecord(
                    item.getProductName(),
                    item.getVariantName(),
                    item.getSku(),
                    item.getQuantitySold(),
                    item.getRevenue(),
                    item.getCost(),
                    item.getGrossProfit(),
                    item.getProfitMargin()
            );
        }
    }

    private void writeTopProductsCSV(CSVPrinter csv, TopProductsReportResponse data) throws IOException {
        csv.printRecord("TOP PRODUCTS REPORT");
        csv.println();

        csv.printRecord("Rank", "Product", "Variant", "SKU", "Quantity Sold", "Total Revenue");
        for (var item : data.getTopProducts()) {
            csv.printRecord(
                    item.getRank(),
                    item.getProductName(),
                    item.getVariantName(),
                    item.getSku(),
                    item.getQuantitySold(),
                    item.getTotalRevenue()
            );
        }
    }

    private void writeInventoryCSV(CSVPrinter csv, InventoryReportResponse data) throws IOException {
        csv.printRecord("INVENTORY REPORT");
        csv.printRecord("Total Products", data.getTotalProducts());
        csv.printRecord("Low Stock Products", data.getLowStockProducts());
        csv.printRecord("Out of Stock Products", data.getOutOfStockProducts());
        csv.println();

        csv.printRecord("Product", "Variant", "SKU", "Physical", "Reserved", "Available", "Min Threshold", "Low Stock?");
        for (var item : data.getItems()) {
            csv.printRecord(
                    item.getProductName(),
                    item.getVariantName(),
                    item.getSku(),
                    item.getQuantityPhysical(),
                    item.getQuantityReserved(),
                    item.getQuantityAvailable(),
                    item.getMinThreshold(),
                    item.getIsLowStock() ? "YES" : "NO"
            );
        }
    }

    @Override
    public byte[] generatePDF(Object reportData, String reportType) {
        try (PDDocument document = new PDDocument();
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            PDPage page = new PDPage();
            document.addPage(page);

            try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                contentStream.setFont(PDType1Font.HELVETICA_BOLD, 16);
                contentStream.beginText();
                contentStream.newLineAtOffset(50, 750);
                contentStream.showText(reportType.toUpperCase() + " REPORT");
                contentStream.endText();

                contentStream.setFont(PDType1Font.HELVETICA, 12);
                float yPosition = 720;

                switch (reportType.toUpperCase()) {
                    case "REVENUE" -> yPosition = writeRevenuePDF(contentStream, (RevenueReportResponse) reportData, yPosition);
                    case "PROFIT" -> yPosition = writeProfitPDF(contentStream, (ProfitReportResponse) reportData, yPosition);
                    case "TOP_PRODUCTS" -> yPosition = writeTopProductsPDF(contentStream, (TopProductsReportResponse) reportData, yPosition);
                    case "INVENTORY" -> yPosition = writeInventoryPDF(contentStream, (InventoryReportResponse) reportData, yPosition);
                }
            }

            document.save(baos);
            return baos.toByteArray();

        } catch (IOException e) {
            log.error("Error generating PDF: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to generate PDF", e);
        }
    }

    private float writeRevenuePDF(PDPageContentStream cs, RevenueReportResponse data, float y) throws IOException {
        cs.beginText();
        cs.newLineAtOffset(50, y);
        cs.showText("Total Revenue: " + formatCurrency(data.getTotalRevenue()));
        cs.newLineAtOffset(0, -20);
        cs.showText("Total Orders: " + data.getTotalOrders());
        cs.endText();
        return y - 40;
    }

    private float writeProfitPDF(PDPageContentStream cs, ProfitReportResponse data, float y) throws IOException {
        cs.beginText();
        cs.newLineAtOffset(50, y);
        cs.showText("Total Revenue: " + formatCurrency(data.getTotalRevenue()));
        cs.newLineAtOffset(0, -20);
        cs.showText("Total Cost: " + formatCurrency(data.getTotalCost()));
        cs.newLineAtOffset(0, -20);
        cs.showText("Gross Profit: " + formatCurrency(data.getGrossProfit()));
        cs.newLineAtOffset(0, -20);
        cs.showText("Profit Margin: " + String.format("%.2f%%", data.getProfitMargin()));
        cs.endText();
        return y - 80;
    }

    private float writeTopProductsPDF(PDPageContentStream cs, TopProductsReportResponse data, float y) throws IOException {
        cs.beginText();
        cs.newLineAtOffset(50, y);

        for (var product : data.getTopProducts()) {
            cs.showText(String.format("#%d - %s (%s): %d units, %s",
                    product.getRank(),
                    product.getProductName(),
                    product.getSku(),
                    product.getQuantitySold(),
                    formatCurrency(product.getTotalRevenue())
            ));
            cs.newLineAtOffset(0, -20);
            y -= 20;
        }

        cs.endText();
        return y;
    }

    private float writeInventoryPDF(PDPageContentStream cs, InventoryReportResponse data, float y) throws IOException {
        cs.beginText();
        cs.newLineAtOffset(50, y);
        cs.showText("Total Products: " + data.getTotalProducts());
        cs.newLineAtOffset(0, -20);
        cs.showText("Low Stock: " + data.getLowStockProducts());
        cs.newLineAtOffset(0, -20);
        cs.showText("Out of Stock: " + data.getOutOfStockProducts());
        cs.endText();
        return y - 60;
    }

    private String formatCurrency(BigDecimal amount) {
        return String.format("%,.0f VND", amount);
    }
}