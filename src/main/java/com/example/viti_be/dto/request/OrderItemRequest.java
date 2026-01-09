package com.example.viti_be.dto.request;

import jakarta.persistence.Column;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class OrderItemRequest {

    @NotNull(message = "Variant ID không được để trống")
    private UUID productVariantId;
    private UUID productSerialId;
    private Integer quantity;
    private BigDecimal discount;

}
