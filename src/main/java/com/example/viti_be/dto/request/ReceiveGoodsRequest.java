package com.example.viti_be.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class ReceiveGoodsRequest {
    
    private LocalDateTime actualDeliveryDate; // If null, use current timestamp
    
    @NotEmpty(message = "At least one item is required")
    @Valid
    private List<ReceiveGoodsItemRequest> items;
}
