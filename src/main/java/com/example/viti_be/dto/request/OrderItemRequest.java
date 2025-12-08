package com.example.viti_be.dto.request;

import jakarta.persistence.Column;
import lombok.Data;

import java.util.UUID;

@Data
public class OrderItemRequest {
    private UUID productId;
    private Integer quantity;
    private Double discount;
}
