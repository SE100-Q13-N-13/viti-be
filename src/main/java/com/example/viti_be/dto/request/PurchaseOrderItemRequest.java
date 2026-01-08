package com.example.viti_be.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class PurchaseOrderItemRequest {
    
    @NotNull(message = "Product Variant ID is required")
    private UUID productVariantId;
    
    @NotNull(message = "Quantity ordered is required")
    @Positive(message = "Quantity must be positive")
    private Integer quantityOrdered;
    
    @NotNull(message = "Unit price is required")
    @Positive(message = "Unit price must be positive")
    private BigDecimal unitPrice;
    
    @Min(value = 0, message = "Warranty period must be non-negative")
    private Integer warrantyPeriod; // Months
    
    private UUID referenceTicketId; // Optional, for warranty parts
}
