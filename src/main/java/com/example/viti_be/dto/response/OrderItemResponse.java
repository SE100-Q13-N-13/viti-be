package com.example.viti_be.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class OrderItemResponse {
    private UUID id;

    private UUID productVariantId;
    private String sku;
    private String productName;
    private String variantName;
    private String productImage;

    private String serialNumber;
    private Integer quantity;

    private BigDecimal unitPrice;
    // Not returning cost price for regular customer
    // private BigDecimal costPrice;

    private BigDecimal discount;
    private BigDecimal subtotal;

    private LocalDateTime warrantyExpireDate;
}