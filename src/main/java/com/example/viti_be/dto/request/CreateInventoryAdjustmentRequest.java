package com.example.viti_be.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateInventoryAdjustmentRequest {
    
    @NotBlank(message = "Reason is required")
    private String reason;
    
    @NotEmpty(message = "Transaction items cannot be empty")
    @Valid
    private List<StockTransactionItemRequest> items;
}