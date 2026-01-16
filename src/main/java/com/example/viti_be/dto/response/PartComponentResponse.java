package com.example.viti_be.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PartComponentResponse {

    private UUID id;
    private String name;
    private String partType;
    private SupplierInfo supplier;
    private String unit;
    private BigDecimal unitPrice;
    private Integer minStock;
    private Integer currentStock; // From inventory
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SupplierInfo {
        private UUID supplierId;
        private String name;
    }
}
