package com.example.viti_be.mapper;

import com.example.viti_be.dto.response.CustomerResponse;
import com.example.viti_be.dto.response.OrderItemResponse;
import com.example.viti_be.dto.response.OrderResponse;
import com.example.viti_be.model.Order;

import java.util.List;
import java.util.stream.Collectors;


public class OrderMapper {
    public static OrderResponse mapToOrderResponse(Order order) {
        if (order == null) return null;

        List<OrderItemResponse> itemResponses = order.getItems().stream()
                .map(OrderItemMapper::mapToOrderItemResponse)
                .collect(Collectors.toList());

        // 2. Map Customer (Nếu có)
        CustomerResponse customerRes = null;
        if (order.getCustomer() != null) {
            customerRes = CustomerMapper.mapToCustomerResponse(order.getCustomer());
        }

        // 3. Build OrderResponse
        return OrderResponse.builder()
                .id(order.getId())
                .orderNumber(order.getOrderNumber())
                .customer(customerRes)
                .guestName(order.getCustomer() == null ? "Khách lẻ" : order.getCustomer().getFullName())
                .orderType(order.getOrderType().name()) // Enum -> String
                .status(order.getStatus().name())       // Enum -> String
                .paymentMethod(order.getPaymentMethod() != null ? order.getPaymentMethod().name() : null)
                // .shippingAddress(order.getShippingAddress())
                // .shippingFee(order.getShippingFee())
                .items(itemResponses)
                .subtotal(order.getSubtotal())
                .totalDiscount(order.getTotalDiscount())
                .finalAmount(order.getFinalAmount())
                .loyaltyPointsUsed(order.getLoyaltyPointsUsed())
                .createdAt(order.getCreatedAt())
                .build();
    }

    public static OrderResponse mapToOrderResponse(Order order, Integer loyaltyPointsEarned) {
        if (order == null) return null;

        List<OrderItemResponse> itemResponses = order.getItems().stream()
                .map(OrderItemMapper::mapToOrderItemResponse)
                .collect(Collectors.toList());

        CustomerResponse customerRes = null;
        if (order.getCustomer() != null) {
            customerRes = CustomerMapper.mapToCustomerResponse(order.getCustomer());
        }

        return OrderResponse.builder()
                .id(order.getId())
                .orderNumber(order.getOrderNumber())
                .customer(customerRes)
                .guestName(order.getCustomer() == null ? "Khách lẻ" : order.getCustomer().getFullName())
                .orderType(order.getOrderType().name())
                .status(order.getStatus().name())
                .paymentMethod(order.getPaymentMethod() != null ? order.getPaymentMethod().name() : null)
                .items(itemResponses)
                .subtotal(order.getSubtotal())
                .totalDiscount(order.getTotalDiscount())
                .finalAmount(order.getFinalAmount())
                .loyaltyPointsUsed(order.getLoyaltyPointsUsed())
                .loyaltyPointsEarned(loyaltyPointsEarned)
                .createdAt(order.getCreatedAt())
                .build();
    }
}
