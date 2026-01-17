package com.example.viti_be.service;

import com.example.viti_be.dto.response.AuditLogResponse;
import com.example.viti_be.dto.response.pagnitation.PageResponse;
import com.example.viti_be.model.AuditLog;
import com.example.viti_be.model.model_enum.AuditAction;
import com.example.viti_be.model.model_enum.AuditModule;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.UUID;

public interface AuditLogService {
    
    /**
     * Log an audit event
     * @param actorId UUID of the user performing the action
     * @param module Module name (e.g., PURCHASE_ORDER, INVENTORY)
     * @param action Action performed (e.g., CREATE, UPDATE, RECEIVE)
     * @param resourceId ID of the affected resource
     * @param resourceType Type of the affected resource
     * @param oldValue JSON representation of old value (null for CREATE)
     * @param newValue JSON representation of new value
     * @param status Status of the operation (SUCCESS, FAILED)
     * @return Created AuditLog
     */
    AuditLog log(UUID actorId, AuditModule module, AuditAction action, String resourceId, String resourceType,
                 String oldValue, String newValue, String status);
    
    /**
     * Log a successful audit event
     */
    AuditLog logSuccess(UUID actorId, AuditModule module, AuditAction action, String resourceId, String resourceType,
                        String oldValue, String newValue);
    
    /**
     * Log a failed audit event
     */
    AuditLog logFailure(UUID actorId, AuditModule module, AuditAction action, String resourceId, String resourceType,
                        String oldValue, String newValue);
    
    /**
     * Get all audit logs with filters and pagination
     * @param startDate Start date filter (optional)
     * @param endDate End date filter (optional)
     * @param actorId Actor ID filter (optional)
     * @param action Action filter (optional)
     * @param module Module filter (optional)
     * @param pageable Pagination information
     * @return PageResponse of AuditLogResponse
     */
    PageResponse<AuditLogResponse> getAllAuditLogs(
            LocalDateTime startDate,
            LocalDateTime endDate,
            UUID actorId,
            AuditAction action,
            AuditModule module,
            Pageable pageable);
}
