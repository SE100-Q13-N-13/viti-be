package com.example.viti_be.repository;

import com.example.viti_be.model.Report;
import com.example.viti_be.model.model_enum.ReportType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface ReportRepository extends JpaRepository<Report, UUID> {

    List<Report> findByReportTypeAndStartDateAndEndDate(
            ReportType reportType,
            LocalDate startDate,
            LocalDate endDate
    );
}