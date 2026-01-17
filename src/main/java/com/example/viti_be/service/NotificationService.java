package com.example.viti_be.service;

import com.example.viti_be.dto.response.NotificationResponse;
import com.example.viti_be.dto.response.pagnitation.PageResponse;
import com.example.viti_be.model.Notification;
import com.example.viti_be.model.model_enum.NotificationType;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

/**
 * Service interface for Notification operations.
 * Thiết kế event-based, có thể mở rộng cho nhiều loại notification khác nhau.
 */
public interface NotificationService {

    // ===============================
    // NOTIFICATION CREATION (Event-based)
    // ===============================

    /**
     * Tạo notification cho một event cụ thể.
     * Method generic để có thể sử dụng cho nhiều loại event.
     *
     * @param type       Loại notification (ORDER_NEW, INVENTORY_LOW, etc.)
     * @param title      Tiêu đề notification
     * @param content    Nội dung chi tiết
     * @param entityType Loại entity liên quan (ORDER, PRODUCT, etc.)
     * @param entityId   ID của entity liên quan
     * @return Notification đã được tạo
     */
    Notification createNotification(NotificationType type, String title, String content,
                                    String entityType, UUID entityId);

    /**
     * Tạo notification khi có đơn hàng mới.
     * Convenience method cho event ORDER_NEW.
     *
     * @param orderId     ID của đơn hàng
     * @param orderNumber Mã đơn hàng
     */
    void notifyNewOrder(UUID orderId, String orderNumber);

    // ===============================
    // NOTIFICATION RETRIEVAL
    // ===============================

    /**
     * Lấy danh sách tất cả notifications (phân trang)
     *
     * @param pageable Thông tin phân trang
     * @return PageResponse chứa danh sách NotificationResponse
     */
    PageResponse<NotificationResponse> getAllNotifications(Pageable pageable);

    /**
     * Lấy chi tiết một notification theo ID
     *
     * @param id ID của notification
     * @return NotificationResponse
     */
    NotificationResponse getNotificationById(UUID id);

    /**
     * Đếm số notification chưa đọc
     *
     * @return Số lượng notification chưa đọc
     */
    long getUnreadCount();

    // ===============================
    // NOTIFICATION ACTIONS
    // ===============================

    /**
     * Đánh dấu một notification đã đọc
     *
     * @param id ID của notification
     */
    void markAsRead(UUID id);

    /**
     * Đánh dấu tất cả notifications đã đọc
     */
    void markAllAsRead();
}
