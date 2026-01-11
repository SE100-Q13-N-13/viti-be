package com.example.viti_be.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerTierRequest {

    @NotBlank(message = "Tier name is required")
    @Size(max = 50, message = "Tier name must not exceed 50 characters")
    private String name;

    @NotNull(message = "Min point is required")
    @Min(value = 0, message = "Min point must be non-negative")
    private Integer minPoint;

    @NotNull(message = "Discount rate is required")
    @DecimalMin(value = "0.00", message = "Discount rate must be non-negative")
    @DecimalMax(value = "100.00", message = "Discount rate must not exceed 100%")
    private BigDecimal discountRate;

    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;
}