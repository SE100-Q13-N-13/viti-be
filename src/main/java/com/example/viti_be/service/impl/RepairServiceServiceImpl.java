package com.example.viti_be.service.impl;

import com.example.viti_be.dto.request.RepairServiceRequest;
import com.example.viti_be.dto.response.RepairServiceResponse;
import com.example.viti_be.exception.ResourceNotFoundException;
import com.example.viti_be.mapper.WarrantyMapper;
import com.example.viti_be.model.RepairService;
import com.example.viti_be.model.model_enum.AuditAction;
import com.example.viti_be.model.model_enum.AuditModule;
import com.example.viti_be.repository.*;
import com.example.viti_be.service.AuditLogService;
import com.example.viti_be.service.InventoryService;
import com.example.viti_be.service.RepairServiceService;
import com.example.viti_be.service.SystemConfigService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Slf4j
public class RepairServiceServiceImpl implements RepairServiceService {

    private final RepairServiceRepository repairServiceRepository;
    private final AuditLogService auditLogService;

    private final WarrantyMapper mapper;

    public RepairServiceServiceImpl(RepairServiceRepository repairServiceRepository, AuditLogService auditLogService, WarrantyMapper mapper) {
        this.repairServiceRepository = repairServiceRepository;
        this.auditLogService = auditLogService;
        this.mapper = mapper;
    }


    // ========================================
    // REPAIR SERVICE CRUD
    // ========================================

    @Override
    @Transactional
    public RepairServiceResponse createRepairService(RepairServiceRequest request, UUID actorId) {
        log.info("Creating repair service: {}", request.getName());

        RepairService repairService = RepairService.builder()
                .name(request.getName())
                .description(request.getDescription())
                .standardPrice(request.getStandardPrice())
                .estimatedDuration(request.getEstimatedDuration())
                .category(request.getCategory())
                .isActive(true)
                .build();

        repairService.setCreatedBy(actorId);
        RepairService saved = repairServiceRepository.save(repairService);

        auditLogService.log(actorId, AuditModule.WARRANTY, AuditAction.CREATE,
                saved.getId().toString(), "repair_service", null,
                saved.getName(), "SUCCESS");

        return mapper.toRepairServiceResponse(saved);
    }

    @Override
    @Transactional
    public RepairServiceResponse updateRepairService(UUID id, RepairServiceRequest request, UUID actorId) {
        RepairService repairService = repairServiceRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Repair service not found: " + id));

        String oldName = repairService.getName();

        // Update fields
        if (request.getName() != null) repairService.setName(request.getName());
        if (request.getDescription() != null) repairService.setDescription(request.getDescription());
        if (request.getStandardPrice() != null) repairService.setStandardPrice(request.getStandardPrice());
        if (request.getEstimatedDuration() != null) repairService.setEstimatedDuration(request.getEstimatedDuration());
        if (request.getCategory() != null) repairService.setCategory(request.getCategory());
        if (request.getIsActive() != null) repairService.setIsActive(request.getIsActive());

        repairService.setUpdatedBy(actorId);
        RepairService saved = repairServiceRepository.save(repairService);

        auditLogService.log(actorId, AuditModule.WARRANTY, AuditAction.UPDATE,
                id.toString(), "repair_service", oldName,
                saved.getName(), "SUCCESS");

        return mapper.toRepairServiceResponse(saved);
    }

    @Override
    @Transactional
    public void deleteRepairService(UUID id, UUID actorId) {
        RepairService repairService = repairServiceRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Repair service not found: " + id));

        repairService.setIsDeleted(true);
        repairService.setUpdatedBy(actorId);
        repairServiceRepository.save(repairService);

        auditLogService.log(actorId, AuditModule.WARRANTY, AuditAction.DELETE,
                id.toString(), "repair_service", repairService.getName(),
                null, "SUCCESS");

        log.info("Deleted repair service: {}", repairService.getName());
    }

    @Override
    @Transactional(readOnly = true)
    public RepairServiceResponse getRepairServiceById(UUID id) {
        RepairService repairService = repairServiceRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Repair service not found: " + id));
        return mapper.toRepairServiceResponse(repairService);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RepairServiceResponse> getAllRepairServices() {
        List<RepairService> services = repairServiceRepository.findAllByIsDeletedFalseOrderByName();
        return mapper.toRepairServiceResponseList(services);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RepairServiceResponse> getActiveRepairServices() {
        List<RepairService> services = repairServiceRepository.findByIsActiveTrueAndIsDeletedFalseOrderByName();
        return mapper.toRepairServiceResponseList(services);
    }
}
