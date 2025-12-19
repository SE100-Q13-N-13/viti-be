package com.example.viti_be.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
public class PurchaseOrderRequest {
    
    @NotNull(message = "Supplier ID is required")
    private UUID supplierId;
    
    private LocalDateTime expectedDeliveryDate;
    
    @NotEmpty(message = "At least one item is required")
    @Valid
    private List<PurchaseOrderItemRequest> items;
}
