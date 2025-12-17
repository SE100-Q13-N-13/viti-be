package com.example.viti_be.controller;

import com.example.viti_be.dto.request.OrderRequest;
import com.example.viti_be.dto.response.ApiResponse;
import com.example.viti_be.model.Order;
import com.example.viti_be.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    @Autowired
    private OrderService orderService;
    @PostMapping
    public ResponseEntity<ApiResponse<Order>> createOrder(@RequestBody OrderRequest request) {
        Order newOrder = orderService.createOrder(request);
        return ResponseEntity.ok(ApiResponse.success(newOrder, "Order created successfully"));
    }

    @PutMapping("/{id}/confirm")
    public ResponseEntity<ApiResponse<Order>> confirmOrder(@PathVariable("id") UUID id, @RequestParam("employeeId") UUID employeeId) {
        Order confirmedOrder = orderService.confirmOrder(id, employeeId);
        return ResponseEntity.ok(ApiResponse.success(confirmedOrder, "Order confirmed"));
    }
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Order>> getOrderById(@PathVariable("id") UUID id) {
        Order order = orderService.getOrderById(id);
        return ResponseEntity.ok(ApiResponse.success(order, "Order fetched successfully"));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<Order>>> getAllOrders() {
        List<Order> orders = orderService.getAllOrders();
        return ResponseEntity.ok(ApiResponse.success(orders, "Orders fetched successfully"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteOrder(@PathVariable("id") UUID id) {
        orderService.deleteOrder(id);return ResponseEntity.ok(ApiResponse.success(null, "Order deleted"));
    }
}
