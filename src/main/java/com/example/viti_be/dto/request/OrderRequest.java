package com.example.viti_be.dto.request;

import com.example.viti_be.model.OrderItem;

import java.util.List;
import java.util.UUID;

public class OrderRequest {
    private UUID customerId;
    private UUID employeeId;
    private List<OrderItemRequest> orderItems;
    private UUID promotionId;
}
