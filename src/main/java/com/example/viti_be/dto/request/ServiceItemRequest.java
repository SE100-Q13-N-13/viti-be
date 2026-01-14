package com.example.viti_be.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ServiceItemRequest {

    @NotNull(message = "Repair service ID is required")
    private UUID repairServiceId;

    private BigDecimal additionalCost; // Phí phát sinh (optional)

    private String notes;
}
