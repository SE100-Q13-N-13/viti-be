package com.example.viti_be.dto.response;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
public class ProductResponse {
    private UUID id;
    private String name;
    private String imageUrl;
    private String description;
    private String status;

    // Flatten data: Chỉ trả về tên thay vì cả object Category/Supplier
    private String categoryName;
    private UUID categoryId;

    private String supplierName;

    private Integer minStockThreshold;
    private Integer warrantyPeriod;
    private String commonSpecs; // JSON String

    private List<ProductVariantResponse> variants;
}