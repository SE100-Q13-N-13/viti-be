package com.example.viti_be.model;

import com.example.viti_be.model.model_enum.ExportFormat;
import com.example.viti_be.model.model_enum.ReportType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

@Entity
@Table(name = "reports")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Report extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "report_type", nullable = false)
    private ReportType reportType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "generated_by")
    private User generatedBy;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    // Lưu filters dạng JSON (category_id, supplier_id, employee_id...)
    @Column(name = "filters", columnDefinition = "TEXT")
    private String filters; // JSON string

    @Column(name = "total_revenue", precision = 15, scale = 2)
    private BigDecimal totalRevenue;

    @Column(name = "profit", precision = 15, scale = 2)
    private BigDecimal profit;

    @Enumerated(EnumType.STRING)
    @Column(name = "export_format")
    private ExportFormat exportFormat;

    @Column(name = "file_url")
    private String fileUrl; // Supabase Storage URL
}