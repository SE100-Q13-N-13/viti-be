package com.example.viti_be.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RepairServiceRequest {

    @NotBlank(message = "Service name is required")
    private String name;

    private String description;

    @NotNull(message = "Standard price is required")
    private BigDecimal standardPrice;

    private Integer estimatedMinutes;  // Thời gian ước tính (phút)

    @Builder.Default
    private String status = "ACTIVE";  // ACTIVE, INACTIVE

    private String notes;
}