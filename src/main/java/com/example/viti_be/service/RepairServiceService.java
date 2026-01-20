package com.example.viti_be.service;

import com.example.viti_be.dto.request.RepairServiceRequest;
import com.example.viti_be.dto.response.RepairServiceResponse;
import com.example.viti_be.dto.response.pagnitation.PageResponse;
import org.springframework.data.domain.Pageable;

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
    PageResponse<RepairServiceResponse> getAllRepairServices(Pageable pageable);
    List<RepairServiceResponse> getActiveRepairServices();
}
