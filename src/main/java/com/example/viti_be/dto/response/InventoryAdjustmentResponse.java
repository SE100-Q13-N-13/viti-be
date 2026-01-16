package com.example.viti_be.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryAdjustmentResponse {
    
    private UUID id;
    private UUID referenceCode;
    
    private String reason;
    private String status;
    
    private List<StockTransactionResponse> transactions;
    
    private UUID createdBy;
    private String createdByName;
    
    private UUID updatedBy;
    private String updatedByName;
    
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}