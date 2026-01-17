package com.example.viti_be.service.impl;

import com.example.viti_be.dto.response.AuditLogResponse;
import com.example.viti_be.dto.response.UserResponse;
import com.example.viti_be.dto.response.pagnitation.PageResponse;
import com.example.viti_be.model.AuditLog;
import com.example.viti_be.model.User;
import com.example.viti_be.model.model_enum.AuditAction;
import com.example.viti_be.model.model_enum.AuditModule;
import com.example.viti_be.repository.AuditLogRepository;
import com.example.viti_be.repository.UserRepository;
import com.example.viti_be.service.AuditLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class AuditLogServiceImpl implements AuditLogService {

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private UserRepository userRepository;

    @Override
    public AuditLog log(UUID actorId, AuditModule module, AuditAction action, String resourceId, String resourceType,
                        String oldValue, String newValue, String status) {
        AuditLog auditLog = AuditLog.builder()
                .actorId(actorId)
                .module(module)
                .action(action)
                .resourceId(resourceId)
                .resourceType(resourceType)
                .oldValue(oldValue)
                .newValue(newValue)
                .status(status)
                .build();
        
        return auditLogRepository.save(auditLog);
    }

    @Override
    public AuditLog logSuccess(UUID actorId, AuditModule module, AuditAction action, String resourceId, String resourceType,
                               String oldValue, String newValue) {
        return log(actorId, module, action, resourceId, resourceType, oldValue, newValue, "SUCCESS");
    }

    @Override
    public AuditLog logFailure(UUID actorId, AuditModule module, AuditAction action, String resourceId, String resourceType,
                               String oldValue, String newValue) {
        return log(actorId, module, action, resourceId, resourceType, oldValue, newValue, "FAILED");
    }

    @Override
    public PageResponse<AuditLogResponse> getAllAuditLogs(
            LocalDateTime startDate,
            LocalDateTime endDate,
            UUID actorId,
            AuditAction action,
            AuditModule module,
            Pageable pageable) {
        
        Page<AuditLog> auditLogPage = auditLogRepository.findAllWithFilters(
                startDate, endDate, actorId, action, module, pageable);
        
        return PageResponse.from(auditLogPage, this::toAuditLogResponse);
    }

    private AuditLogResponse toAuditLogResponse(AuditLog auditLog) {
        // Load User nếu có actorId
        UserResponse actorResponse = null;
        if (auditLog.getActorId() != null) {
            actorResponse = userRepository.findById(auditLog.getActorId())
                    .map(UserResponse::fromEntity)
                    .orElse(null);
        }

        return AuditLogResponse.builder()
                .id(auditLog.getId())
                .actorId(auditLog.getActorId())
                .actor(actorResponse)
                .module(auditLog.getModule())
                .action(auditLog.getAction())
                .resourceId(auditLog.getResourceId())
                .resourceType(auditLog.getResourceType())
                .oldValue(auditLog.getOldValue())
                .newValue(auditLog.getNewValue())
                .status(auditLog.getStatus())
                .createdAt(auditLog.getCreatedAt())
                .build();
    }
}
