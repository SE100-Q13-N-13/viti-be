package com.example.viti_be.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CustomerTierRequest {

    @NotBlank(message = "Name is required")
    private String name;

    private Integer minPoint;

    @DecimalMin(value = "0.0", message = "Discount rate must be positive")
    private BigDecimal discountRate;

    private String description;

    private String status;
}
