package com.example.viti_be.dto.request;

import lombok.Data;
import java.util.UUID;

@Data
public class ProductRequest {
    private String name;
    private UUID categoryId;
    private UUID supplierId;
    private String description;
    private Integer minStockThreshold;
    private Integer warrantyPeriod;
    private String commonSpecs; // JSON String từ FE gửi lên
}