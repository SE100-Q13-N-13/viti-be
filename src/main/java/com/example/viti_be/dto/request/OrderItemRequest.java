package com.example.viti_be.dto.request;

import jakarta.persistence.Column;

import java.util.UUID;

public class OrderItemRequest {
    private UUID productId;
    private Integer quantity;
    private Double discount;

}
