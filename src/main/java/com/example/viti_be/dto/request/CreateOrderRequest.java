package com.example.viti_be.dto.request;

import com.example.viti_be.model.model_enum.OrderType;
import com.example.viti_be.model.model_enum.PaymentMethod;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
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

    @NotNull(message = "Order type is required")
    private OrderType orderType; // OFFLINE, ONLINE_COD, ONLINE_TRANSFER

    @NotNull(message = "Payment method is required")
    private PaymentMethod paymentMethod; // CASH, TRANSFER, COD

    @NotEmpty(message = "Order must have at least one item")
    @Valid
    private List<OrderItemRequest> items; // Required for online orders

    private List<UUID> promotionIds; // Khuyến mãi áp dụng

    /**
     * Số điểm loyalty muốn sử dụng cho đơn hàng này
     * Nullable - Nếu null hoặc 0 = không dùng điểm
     */
    @Min(value = 0, message = "Loyalty points must be non-negative")
    private Integer loyaltyPointsToUse;

    // Guest info (nếu customerId == null)
    private String guestName;
    private String guestPhone;
    private String guestEmail;
}