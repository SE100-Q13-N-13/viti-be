package com.example.viti_be.controller;

import com.example.viti_be.dto.response.ApiResponse;
import com.example.viti_be.dto.response.AuditLogResponse;
import com.example.viti_be.dto.response.pagnitation.PageResponse;
import com.example.viti_be.model.model_enum.AuditAction;
import com.example.viti_be.model.model_enum.AuditModule;
import com.example.viti_be.service.AuditLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/audit-logs")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Audit Log", description = "API quản lý audit logs")
public class AuditLogController {

    private final AuditLogService auditLogService;

    /**
     * Lấy danh sách audit logs với các bộ lọc
     * GET /api/admin/audit-logs
     */
    @GetMapping
    @Operation(
        summary = "Get all audit logs with filters",
        description = "Lấy danh sách audit logs với phân trang và các bộ lọc: time range, actor, action, module"
    )
    public ResponseEntity<ApiResponse<PageResponse<AuditLogResponse>>> getAllAuditLogs(
            @Parameter(description = "Thời gian bắt đầu (format: yyyy-MM-dd'T'HH:mm:ss)")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime startDate,
            
            @Parameter(description = "Thời gian kết thúc (format: yyyy-MM-dd'T'HH:mm:ss)")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime endDate,
            
            @Parameter(description = "ID người thực hiện")
            @RequestParam(required = false)
            UUID actorId,
            
            @Parameter(description = "Hành động (CREATE, UPDATE, DELETE, LOCK, UNLOCK, EARN_POINTS, REDEEM_POINTS, ADJUST_POINTS, RESET_POINTS, RECEIVE_GOODS, CLOSE_PO)")
            @RequestParam(required = false)
            AuditAction action,
            
            @Parameter(description = "Module (INVOICE, PRODUCT, INVENTORY, SUPPLIER, STAFF, PROMOTION, CONFIG, WARRANTY, LOYALTY_POINTS, PURCHASE_ORDER, ORDER)")
            @RequestParam(required = false)
            AuditModule module,
            
            @ParameterObject
            @PageableDefault(size = 20, sort = "createdAt")
            Pageable pageable
    ) {
        PageResponse<AuditLogResponse> auditLogs = auditLogService.getAllAuditLogs(
                startDate, endDate, actorId, action, module, pageable);
        
        return ResponseEntity.ok(ApiResponse.success(auditLogs, "Lấy danh sách audit logs thành công"));
    }
}
