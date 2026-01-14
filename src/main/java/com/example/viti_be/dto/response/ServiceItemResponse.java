package com.example.viti_be.dto.response;

import com.example.viti_be.model.model_enum.WarrantyServiceStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ServiceItemResponse {

    private UUID id;
    private UUID repairServiceId;
    private String serviceName;
    private String serviceDescription;
    private WarrantyServiceStatus status;
    private BigDecimal unitPrice;
    private BigDecimal additionalCost;
    private BigDecimal totalCost;
    private LocalDateTime completedAt;
    private String notes;
}