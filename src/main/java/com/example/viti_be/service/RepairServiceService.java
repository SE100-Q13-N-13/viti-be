package com.example.viti_be.service;

import com.example.viti_be.dto.request.RepairServiceRequest;
import com.example.viti_be.dto.response.RepairServiceResponse;

import java.util.List;
import java.util.UUID;

public interface RepairServiceService {
    /**
     * CRUD RepairService
     */
    RepairServiceResponse createRepairService(RepairServiceRequest request, UUID actorId);
    RepairServiceResponse updateRepairService(UUID id, RepairServiceRequest request, UUID actorId);
    void deleteRepairService(UUID id, UUID actorId);
    RepairServiceResponse getRepairServiceById(UUID id);
    List<RepairServiceResponse> getAllRepairServices();
    List<RepairServiceResponse> getActiveRepairServices();
}
