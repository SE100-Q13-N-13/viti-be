package com.example.viti_be.service;

import com.example.viti_be.dto.request.CreateInventoryAdjustmentRequest;
import com.example.viti_be.dto.response.InventoryAdjustmentResponse;
import com.example.viti_be.model.model_enum.InventoryAdjustmentStatus;

import java.util.List;
import java.util.UUID;

public interface InventoryAdjustmentService {
    
    /**
     * Create new inventory adjustment (PENDING status)
     * @param request CreateInventoryAdjustmentRequest
     * @param actorId User creating the adjustment
     * @return InventoryAdjustmentResponse
     */
    InventoryAdjustmentResponse createAdjustment(CreateInventoryAdjustmentRequest request, UUID actorId);
    
    /**
     * Get adjustment by ID
     * @param id Adjustment ID
     * @return InventoryAdjustmentResponse
     */
    InventoryAdjustmentResponse getAdjustmentById(UUID id);
    
    /**
     * Get all adjustments
     * @return List of InventoryAdjustmentResponse
     */
    List<InventoryAdjustmentResponse> getAllAdjustments();
    
    /**
     * Get adjustments by status
     * @param status InventoryAdjustmentStatus
     * @return List of InventoryAdjustmentResponse
     */
    List<InventoryAdjustmentResponse> getAdjustmentsByStatus(InventoryAdjustmentStatus status);
    
    /**
     * Approve adjustment and apply inventory changes
     * @param adjustmentId Adjustment ID
     * @param actorId User approving
     * @return Updated InventoryAdjustmentResponse
     */
    InventoryAdjustmentResponse approveAdjustment(UUID adjustmentId, UUID actorId);
    
    /**
     * Reject adjustment (no inventory changes)
     * @param adjustmentId Adjustment ID
     * @param reason Reason for rejection
     * @param actorId User rejecting
     * @return Updated InventoryAdjustmentResponse
     */
    InventoryAdjustmentResponse rejectAdjustment(UUID adjustmentId, String reason, UUID actorId);
    
    /**
     * Delete adjustment (only if PENDING or REJECTED)
     * @param adjustmentId Adjustment ID
     * @param actorId User deleting
     */
    void deleteAdjustment(UUID adjustmentId, UUID actorId);
}