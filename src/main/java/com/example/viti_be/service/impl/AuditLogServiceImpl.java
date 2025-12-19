package com.example.viti_be.service.impl;

import com.example.viti_be.model.AuditLog;
import com.example.viti_be.repository.AuditLogRepository;
import com.example.viti_be.service.AuditLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class AuditLogServiceImpl implements AuditLogService {

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Override
    public AuditLog log(UUID actorId, String module, String action, String resourceId,
                        String oldValue, String newValue, String status) {
        AuditLog auditLog = AuditLog.builder()
                .actorId(actorId)
                .module(module)
                .action(action)
                .resourceId(resourceId)
                .oldValue(oldValue)
                .newValue(newValue)
                .status(status)
                .build();
        
        return auditLogRepository.save(auditLog);
    }

    @Override
    public AuditLog logSuccess(UUID actorId, String module, String action, String resourceId,
                               String oldValue, String newValue) {
        return log(actorId, module, action, resourceId, oldValue, newValue, "SUCCESS");
    }

    @Override
    public AuditLog logFailure(UUID actorId, String module, String action, String resourceId,
                               String oldValue, String newValue) {
        return log(actorId, module, action, resourceId, oldValue, newValue, "FAILED");
    }
}
