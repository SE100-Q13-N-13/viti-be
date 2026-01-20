package com.example.viti_be.service;

import com.example.viti_be.dto.request.PartComponentRequest;
import com.example.viti_be.dto.response.PartComponentResponse;
import com.example.viti_be.dto.response.pagnitation.PageResponse;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface PartComponentService {
    /**
     * CRUD PartComponent
     */
    PartComponentResponse createPartComponent(PartComponentRequest request, UUID actorId);
    PartComponentResponse updatePartComponent(UUID id, PartComponentRequest request, UUID actorId);
    void deletePartComponent(UUID id, UUID actorId);
    PartComponentResponse getPartComponentById(UUID id);
    PageResponse<PartComponentResponse> getAllPartComponents(Pageable pageable);
    List<PartComponentResponse> getActivePartComponents();
    List<PartComponentResponse> getLowStockParts();
}
