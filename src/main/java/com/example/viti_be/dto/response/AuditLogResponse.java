package com.example.viti_be.dto.response;

import com.example.viti_be.model.model_enum.AuditAction;
import com.example.viti_be.model.model_enum.AuditModule;
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
@Schema(description = "Thông tin audit log")
public class AuditLogResponse {

    @Schema(description = "ID của audit log")
    private UUID id;

    @Schema(description = "ID của người thực hiện")
    private UUID actorId;

    @Schema(description = "Thông tin người thực hiện")
    private UserResponse actor;

    @Schema(description = "Module thực hiện")
    private AuditModule module;

    @Schema(description = "Hành động thực hiện")
    private AuditAction action;

    @Schema(description = "ID của tài nguyên bị ảnh hưởng")
    private String resourceId;

    @Schema(description = "Loại tài ngu yên")
    private String resourceType;

    @Schema(description = "Giá trị cũ (JSON)")
    private String oldValue;

    @Schema(description = "Giá trị mới (JSON)")
    private String newValue;

    @Schema(description = "Trạng thái (SUCCESS, FAILED)")
    private String status;

    @Schema(description = "Thời gian tạo")
    private LocalDateTime createdAt;
}
