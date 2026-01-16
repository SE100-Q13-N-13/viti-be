package com.example.viti_be.controller;

import com.example.viti_be.dto.request.CreateInventoryAdjustmentRequest;
import com.example.viti_be.dto.response.ApiResponse;
import com.example.viti_be.dto.response.InventoryAdjustmentResponse;
import com.example.viti_be.model.model_enum.InventoryAdjustmentStatus;
import com.example.viti_be.security.services.UserDetailsImpl;
import com.example.viti_be.service.InventoryAdjustmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/inventory-adjustments")
@RequiredArgsConstructor
public class InventoryAdjustmentController {

    private final InventoryAdjustmentService adjustmentService;

    /**
     * POST /api/v1/inventory-adjustments
     * Create new inventory adjustment
     */
    @PostMapping
    public ResponseEntity<ApiResponse<InventoryAdjustmentResponse>> createAdjustment(
            @RequestBody @Valid CreateInventoryAdjustmentRequest request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        UserDetailsImpl userImpl = (UserDetailsImpl) userDetails;
        UUID actorId = userImpl.getId();
        InventoryAdjustmentResponse response = adjustmentService.createAdjustment(request, actorId);

        return ResponseEntity.ok(ApiResponse.success(response, "Tạo điều chỉnh tồn kho thành công"));
    }

    /**
     * GET /api/v1/inventory-adjustments/{id}
     * Get adjustment by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<InventoryAdjustmentResponse>> getAdjustmentById(
            @PathVariable UUID id
    ) {
        InventoryAdjustmentResponse response = adjustmentService.getAdjustmentById(id);

        return ResponseEntity.ok(ApiResponse.success(response, "Lây điều chỉnh tồn kho thành công"));
    }

    /**
     * GET /api/v1/inventory-adjustments
     * Get all adjustments or filter by status
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<InventoryAdjustmentResponse>>> getAdjustments(
            @RequestParam(required = false) InventoryAdjustmentStatus status
    ) {
        List<InventoryAdjustmentResponse> responses;

        if (status != null) {
            responses = adjustmentService.getAdjustmentsByStatus(status);
        } else {
            responses = adjustmentService.getAllAdjustments();
        }

        return ResponseEntity.ok(ApiResponse.success(responses, "Lấy danh sách điều chỉnh tồn kho thành công"));
    }

    /**
     * PUT /api/v1/inventory-adjustments/{id}/approve
     * Approve adjustment and apply inventory changes
     */
    @PutMapping("/{id}/approve")
    public ResponseEntity<ApiResponse<InventoryAdjustmentResponse>> approveAdjustment(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        UserDetailsImpl userImpl = (UserDetailsImpl) userDetails;
        UUID actorId = userImpl.getId();
        InventoryAdjustmentResponse response = adjustmentService.approveAdjustment(id, actorId);

        return ResponseEntity.ok(ApiResponse.success(response, "Chấp nhận điều chỉnh tồn kho thành công"));
    }

    /**
     * PUT /api/v1/inventory-adjustments/{id}/reject
     * Reject adjustment
     */
    @PutMapping("/{id}/reject")
    public ResponseEntity<ApiResponse<InventoryAdjustmentResponse>> rejectAdjustment(
            @PathVariable UUID id,
            @RequestParam String reason,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        UserDetailsImpl userImpl = (UserDetailsImpl) userDetails;
        UUID actorId = userImpl.getId();
        InventoryAdjustmentResponse response = adjustmentService.rejectAdjustment(id, reason, actorId);

        return ResponseEntity.ok(ApiResponse.success(response, "Từ chối điều chỉnh tồn kho thành công"));
    }

    /**
     * DELETE /api/v1/inventory-adjustments/{id}
     * Delete adjustment (only PENDING or REJECTED)
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteAdjustment(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        UserDetailsImpl userImpl = (UserDetailsImpl) userDetails;
        UUID actorId = userImpl.getId();
        adjustmentService.deleteAdjustment(id, actorId);

        return ResponseEntity.ok(ApiResponse.success(null, "Xóa điều chỉnh tồn kho thành công"));
    }
}
