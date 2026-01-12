package com.example.viti_be.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CartItemRequest {
    @NotNull
    private UUID productId;

    @NotNull
    private UUID productVariantId;

    @NotNull @Min(1)
    private Integer quantity;

    @NotNull
    private BigDecimal unitPrice;
}
