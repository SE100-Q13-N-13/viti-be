package com.example.viti_be.dto.response;

import com.example.viti_be.model.Promotion;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class OrderResponse {
    private UUID id;
    private String orderNumber;

    private CustomerResponse customer;

    // Guest Info (Nếu khách vãng lai)
    private String guestName;
    private String guestPhone;

    private String orderType;
    private String status;
    private String paymentMethod;

    // TODO: Shipping logic
//    private String shippingAddress; // Địa chỉ giao hàng
//    private BigDecimal shippingFee; // Phí ship
    
    private List<OrderItemResponse> items;

    private List<Promotion> promotions;
    
    private BigDecimal subtotal;
    private BigDecimal totalDiscount;
    private BigDecimal finalAmount;
    
    private Integer loyaltyPointsUsed;
    private Integer loyaltyPointsEarned;
    
    private LocalDateTime createdAt;
}