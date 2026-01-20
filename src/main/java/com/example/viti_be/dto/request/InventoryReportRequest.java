package com.example.viti_be.dto.request;

import lombok.*;

import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryReportRequest {

    private LocalDate startDate; // For fast-moving calculation
    private LocalDate endDate;

    private UUID categoryId;

    // Inventory specific filters
    private Boolean lowStockOnly;
    private Boolean fastMovingOnly;

    @Override
    public String toString() {
        return "InventoryReportRequest{" +
                "start=" + startDate +
                ",end=" + endDate +
                ",cat=" + categoryId +
                ",lowStock=" + lowStockOnly +
                ",fastMoving=" + fastMovingOnly +
                '}';
    }
}