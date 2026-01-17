package com.example.viti_be.service.impl;

import com.example.viti_be.dto.response.NotificationResponse;
import com.example.viti_be.dto.response.pagnitation.PageResponse;
import com.example.viti_be.exception.ResourceNotFoundException;
import com.example.viti_be.mapper.NotificationMapper;
import com.example.viti_be.model.Notification;
import com.example.viti_be.model.model_enum.NotificationType;
import com.example.viti_be.repository.NotificationRepository;
import com.example.viti_be.service.NotificationService;
import com.example.viti_be.service.SseEmitterService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Implementation của NotificationService.
 * Xử lý tạo, lấy danh sách và quản lý trạng thái notifications.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final SseEmitterService sseEmitterService;

    // ===============================
    // NOTIFICATION CREATION
    // ===============================

    @Override
    @Transactional
    public Notification createNotification(NotificationType type, String title, String content,
                                           String entityType, UUID entityId) {
        log.info("Creating notification: type={}, entityType={}, entityId={}", type, entityType, entityId);

        Notification notification = Notification.builder()
                .type(type)
                .title(title)
                .content(content)
                .entityType(entityType)
                .entityId(entityId)
                .isRead(false)
                .build();

        notification = notificationRepository.save(notification);

        // Push event qua SSE cho tất cả admin đang subscribe
        NotificationResponse response = NotificationMapper.toResponse(notification);
        sseEmitterService.sendEventToAll(type.name(), response);

        log.info("Notification created and pushed via SSE: id={}", notification.getId());
        return notification;
    }

    @Override
    @Transactional
    public void notifyNewOrder(UUID orderId, String orderNumber) {
        String title = "Đơn hàng mới";
        String content = String.format("Đơn hàng %s vừa được tạo", orderNumber);

        createNotification(
                NotificationType.ORDER_NEW,
                title,
                content,
                "ORDER",
                orderId
        );
    }

    // ===============================
    // NOTIFICATION RETRIEVAL
    // ===============================

    @Override
    public PageResponse<NotificationResponse> getAllNotifications(Pageable pageable) {
        Page<Notification> page = notificationRepository.findAllActiveNotifications(pageable);

        // Sử dụng PageResponse.from() theo cấu trúc pagination hiện có của hệ thống
        return PageResponse.from(page, NotificationMapper::toResponse);
    }

    @Override
    public NotificationResponse getNotificationById(UUID id) {
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found with id: " + id));

        return NotificationMapper.toResponse(notification);
    }

    @Override
    public long getUnreadCount() {
        return notificationRepository.countUnreadNotifications();
    }

    // ===============================
    // NOTIFICATION ACTIONS
    // ===============================

    @Override
    @Transactional
    public void markAsRead(UUID id) {
        // Validate notification exists
        if (!notificationRepository.existsById(id)) {
            throw new ResourceNotFoundException("Notification not found with id: " + id);
        }

        notificationRepository.markAsRead(id);
        log.info("Notification marked as read: id={}", id);
    }

    @Override
    @Transactional
    public void markAllAsRead() {
        notificationRepository.markAllAsRead();
        log.info("All notifications marked as read");
    }
}
