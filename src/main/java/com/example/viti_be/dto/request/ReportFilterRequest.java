package com.example.viti_be.dto.request;

import com.example.viti_be.model.model_enum.*;
import lombok.*;

import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReportFilterRequest {

    private LocalDate startDate;
    private LocalDate endDate;

    // Group by for revenue/profit reports
    private GroupBy groupBy; // DAY, WEEK, MONTH

    // Filters
    private UUID categoryId;
    private UUID supplierId;
    private UUID employeeId;
    private UUID customerTierId;
    private OrderType orderType;
    private OrderStatus orderStatus;
    private PaymentMethod paymentMethod;

    // For top products ONLY
    private Integer limit; // Top 10, 20, 50...
    private SortBy sortBy; // QUANTITY, REVENUE (for top products only)

    @Override
    public String toString() {
        return "ReportFilter{" +
                "start=" + startDate +
                ",end=" + endDate +
                ",group=" + groupBy +
                ",cat=" + categoryId +
                ",sup=" + supplierId +
                ",emp=" + employeeId +
                ",tier=" + customerTierId +
                ",type=" + orderType +
                ",status=" + orderStatus +
                ",payment=" + paymentMethod +
                ",limit=" + limit +
                ",sort=" + sortBy +
                '}';
    }
}