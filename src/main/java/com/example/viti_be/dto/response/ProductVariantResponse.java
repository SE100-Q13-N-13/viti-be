package com.example.viti_be.dto.response;

import lombok.Data;
import java.math.BigDecimal;
import java.util.UUID;

@Data
public class ProductVariantResponse {
    private UUID id;
    private String sku;
    private String barcode;
    private String variantName;
    private String variantSpecs;
    private BigDecimal sellingPrice;
}