package com.example.viti_be.dto.response;

import com.example.viti_be.model.model_enum.NotificationType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Response object for Notification")
public class NotificationResponse {

    @Schema(description = "ID của notification")
    private UUID id;

    @Schema(description = "Loại notification", example = "ORDER_NEW")
    private NotificationType type;

    @Schema(description = "Tiêu đề notification")
    private String title;

    @Schema(description = "Nội dung chi tiết notification")
    private String content;

    @Schema(description = "Loại entity liên quan", example = "ORDER")
    private String entityType;

    @Schema(description = "ID của entity liên quan")
    private UUID entityId;

    @Schema(description = "Trạng thái đã đọc")
    private Boolean isRead;

    @Schema(description = "Thời gian tạo")
    private LocalDateTime createdAt;
}
