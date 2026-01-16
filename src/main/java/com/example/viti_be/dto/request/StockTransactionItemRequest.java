package com.example.viti_be.dto.request;

import com.example.viti_be.model.model_enum.StockTransactionType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockTransactionItemRequest {
    
    @NotNull(message = "Product variant ID is required")
    private UUID productVariantId;
    
    @NotNull(message = "Transaction type is required")
    private StockTransactionType type;
    
    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be at least 1")
    private Integer quantity;
    
    private String reason;
}