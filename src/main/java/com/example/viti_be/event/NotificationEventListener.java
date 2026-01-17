package com.example.viti_be.event;

import com.example.viti_be.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Event Listener xử lý các events liên quan đến Notification.
 * Sử dụng @TransactionalEventListener để đảm bảo notification được tạo
 * SAU KHI transaction commit thành công.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationEventListener {

    private final NotificationService notificationService;

    /**
     * Xử lý event khi đơn hàng mới được tạo.
     * 
     * - TransactionPhase.AFTER_COMMIT: Chỉ chạy sau khi transaction commit thành công
     * - @Async: Chạy bất đồng bộ để không block luồng tạo đơn hàng
     * 
     * @param event OrderCreatedEvent chứa thông tin đơn hàng
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async
    public void handleOrderCreatedEvent(OrderCreatedEvent event) {
        log.info("Handling OrderCreatedEvent: orderId={}, orderNumber={}", 
                event.getOrderId(), event.getOrderNumber());
        
        try {
            notificationService.notifyNewOrder(event.getOrderId(), event.getOrderNumber());
            log.info("Notification created for new order: {}", event.getOrderNumber());
        } catch (Exception e) {
            // Log error nhưng không throw để không ảnh hưởng đến flow chính
            log.error("Failed to create notification for order {}: {}", 
                    event.getOrderNumber(), e.getMessage(), e);
        }
    }
}
