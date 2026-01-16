package com.example.viti_be.dto.response;

import com.example.viti_be.model.model_enum.StockTransactionType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class StockTransactionResponse {
    private UUID id;
    private UUID inventoryId;

    private UUID productVariantId;
    private String productVariantName;
    private String sku;

    private StockTransactionType type;
    private Integer quantity;
    private Integer quantityBefore;
    private Integer quantityAfter;
    private String reason;

    private String referenceId;
    private LocalDateTime createdAt;
    private UUID createdBy;

    private String createdByName;
}
