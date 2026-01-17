package com.example.viti_be.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.util.UUID;

/**
 * Event được publish khi đơn hàng mới được tạo thành công.
 * Sử dụng Spring Application Events để decouple notification logic khỏi order creation.
 */
@Getter
public class OrderCreatedEvent extends ApplicationEvent {

    private final UUID orderId;
    private final String orderNumber;

    public OrderCreatedEvent(Object source, UUID orderId, String orderNumber) {
        super(source);
        this.orderId = orderId;
        this.orderNumber = orderNumber;
    }
}
