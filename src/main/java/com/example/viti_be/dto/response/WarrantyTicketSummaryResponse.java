package com.example.viti_be.dto.response;

import com.example.viti_be.model.model_enum.WarrantyTicketStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WarrantyTicketSummaryResponse {

    private UUID id;
    private String ticketNumber;
    private String serialNumber;
    private String productName;
    private String customerName;
    private String customerPhone;
    private String technicianName;
    private WarrantyTicketStatus status;
    private Boolean isUnderWarranty;
    private BigDecimal totalCost;
    private LocalDateTime receivedDate;
    private LocalDateTime expectedReturnDate;
    private LocalDateTime createdAt;
}
