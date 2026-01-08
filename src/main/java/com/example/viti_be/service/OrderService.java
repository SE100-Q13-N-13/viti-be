package com.example.viti_be.service;

import com.example.viti_be.dto.request.OrderRequest;
import com.example.viti_be.model.Order;

import java.util.List;
import java.util.UUID;

public interface OrderService {
    Order getOrderById(UUID id);
    List<Order> getAllOrders();
    Order createOrder(OrderRequest request);
    Order confirmOrder(UUID orderId, UUID employeeId);
    void deleteOrder(UUID id);
}
