package com.example.viti_be.service;

import com.example.viti_be.model.AuditLog;

import java.util.UUID;

public interface AuditLogService {
    
    /**
     * Log an audit event
     * @param actorId UUID of the user performing the action
     * @param module Module name (e.g., PURCHASE_ORDER, INVENTORY)
     * @param action Action performed (e.g., CREATE, UPDATE, RECEIVE)
     * @param resourceId ID of the affected resource
     * @param oldValue JSON representation of old value (null for CREATE)
     * @param newValue JSON representation of new value
     * @param status Status of the operation (SUCCESS, FAILED)
     * @return Created AuditLog
     */
    AuditLog log(UUID actorId, String module, String action, String resourceId, 
                 String oldValue, String newValue, String status);
    
    /**
     * Log a successful audit event
     */
    AuditLog logSuccess(UUID actorId, String module, String action, String resourceId, 
                        String oldValue, String newValue);
    
    /**
     * Log a failed audit event
     */
    AuditLog logFailure(UUID actorId, String module, String action, String resourceId, 
                        String oldValue, String newValue);
}
