package com.example.viti_be.service;

import com.example.viti_be.dto.request.CreateOrderRequest;
import com.example.viti_be.dto.response.OrderResponse;
import com.example.viti_be.model.Order;
import com.example.viti_be.model.model_enum.OrderStatus;
import jakarta.transaction.Transactional;

import java.util.List;
import java.util.UUID;

public interface OrderService {
    OrderResponse getOrderById(UUID id);
    List<OrderResponse> getAllOrders();
    OrderResponse createOrder(CreateOrderRequest request, UUID employeeId);
    @Transactional
    OrderResponse updateOrderStatus(UUID orderId, OrderStatus newStatus, String reason, UUID userId);

    void deleteOrder(UUID id);
}
