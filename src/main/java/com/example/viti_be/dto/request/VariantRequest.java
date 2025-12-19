package com.example.viti_be.dto.request;

import lombok.Data;
import java.math.BigDecimal;
import java.util.UUID;

@Data
public class VariantRequest {
    private UUID productId;
    private String sku;
    private String variantName;
    private String variantSpecs; // JSON String
    private BigDecimal sellingPrice;
}