package com.example.viti_be.mapper;

import com.example.viti_be.dto.response.NotificationResponse;
import com.example.viti_be.model.Notification;

/**
 * Mapper chuyển đổi Notification entity sang NotificationResponse DTO
 */
public class NotificationMapper {

    private NotificationMapper() {
        // Utility class - private constructor
    }

    /**
     * Map Notification entity to NotificationResponse DTO
     *
     * @param notification Entity cần chuyển đổi
     * @return NotificationResponse DTO
     */
    public static NotificationResponse toResponse(Notification notification) {
        if (notification == null) {
            return null;
        }

        return NotificationResponse.builder()
                .id(notification.getId())
                .type(notification.getType())
                .title(notification.getTitle())
                .content(notification.getContent())
                .entityType(notification.getEntityType())
                .entityId(notification.getEntityId())
                .isRead(notification.getIsRead())
                .createdAt(notification.getCreatedAt())
                .build();
    }
}
