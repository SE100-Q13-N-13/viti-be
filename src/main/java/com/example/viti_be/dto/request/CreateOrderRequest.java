package com.example.viti_be.dto.request;

import com.example.viti_be.model.model_enum.OrderType;
import com.example.viti_be.model.model_enum.PaymentMethod;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@Valid
public class CreateOrderRequest {
    private UUID customerId;  // Nullable for guest checkout
    private UUID employeeId;
    private OrderType orderType; // OFFLINE, ONLINE_COD, ONLINE_TRANSFER
    private PaymentMethod paymentMethod; // CASH, TRANSFER, COD
    private String shippingAddress; // Required for online orders

    @NotNull(message = "Giỏ hàng không được trống")
    private List<OrderItemRequest> items;

    private List<UUID> promotionIds; // Khuyến mãi áp dụng
    private Integer loyaltyPointsUsed; // Điểm sử dụng

    // Guest info (nếu customerId == null)
    private String guestName;
    private String guestPhone;
    private String guestEmail;
}