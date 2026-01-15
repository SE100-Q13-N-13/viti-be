package com.example.viti_be.controller;

import com.example.viti_be.dto.request.CustomerTierRequest;
import com.example.viti_be.dto.response.ApiResponse;
import com.example.viti_be.dto.response.CustomerTierResponse;
import com.example.viti_be.security.services.UserDetailsImpl;
import com.example.viti_be.service.CustomerTierService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/customer-tiers")
@RequiredArgsConstructor
public class CustomerTierController {

    private final CustomerTierService customerTierService;

    /**
     * Lấy tất cả tiers (Admin/Accountant)
     * GET /api/customer-tiers
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'ACCOUNTANT')")
    public ResponseEntity<ApiResponse<List<CustomerTierResponse>>> getAllTiers() {
        List<CustomerTierResponse> tiers = customerTierService.getAllTiers();
        return ResponseEntity.ok(ApiResponse.success(tiers, "Tiers retrieved successfully"));
    }

    /**
     * Lấy tất cả tiers đang active (Public - để show cho customer)
     * GET /api/customer-tiers/active
     */
    @GetMapping("/active")
    public ResponseEntity<ApiResponse<List<CustomerTierResponse>>> getActiveTiers() {
        List<CustomerTierResponse> tiers = customerTierService.getActiveTiers();
        return ResponseEntity.ok(ApiResponse.success(tiers, "Active tiers retrieved successfully"));
    }

    /**
     * Lấy tier theo ID
     * GET /api/customer-tiers/{id}
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ACCOUNTANT')")
    public ResponseEntity<ApiResponse<CustomerTierResponse>> getTierById(@PathVariable UUID id) {
        CustomerTierResponse tier = customerTierService.getTierById(id);
        return ResponseEntity.ok(ApiResponse.success(tier, "Tier retrieved successfully"));
    }

    /**
     * Tạo tier mới (Admin only)
     * POST /api/customer-tiers
     * Body: { "name": "DIAMOND", "minPoint": 20000, "discountRate": 20.00, "description": "..." }
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<CustomerTierResponse>> createTier(
            @Valid @RequestBody CustomerTierRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        UUID adminId = ((UserDetailsImpl) userDetails).getId();
        CustomerTierResponse tier = customerTierService.createTier(request, adminId);
        return ResponseEntity.ok(ApiResponse.success(tier, "Tier created successfully"));
    }

    /**
     * Cập nhật tier (Admin only)
     * PUT /api/customer-tiers/{id}
     * Body: { "minPoint": 6000, "discountRate": 12.00, ... }
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<CustomerTierResponse>> updateTier(
            @PathVariable UUID id,
            @Valid @RequestBody CustomerTierRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        UUID adminId = ((UserDetailsImpl) userDetails).getId();
        CustomerTierResponse tier = customerTierService.updateTier(id, request, adminId);
        return ResponseEntity.ok(ApiResponse.success(tier, "Tier updated successfully. All customers re-calculated."));
    }

    /**
     * Xóa tier (Admin only)
     * DELETE /api/customer-tiers/{id}
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteTier(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails userDetails) {

        UUID adminId = ((UserDetailsImpl) userDetails).getId();
        customerTierService.deleteTier(id, adminId);
        return ResponseEntity.ok(ApiResponse.success(null, "Tier deleted successfully"));
    }

    /**
     * Bật/tắt tier (Admin only)
     * POST /api/customer-tiers/{id}/toggle-status
     */
    @PostMapping("/{id}/toggle-status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<CustomerTierResponse>> toggleTierStatus(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails userDetails) {
        UUID adminId = ((UserDetailsImpl) userDetails).getId();
        CustomerTierResponse tier = customerTierService.toggleTierStatus(id, adminId);
        return ResponseEntity.ok(ApiResponse.success(tier, "Tier status updated successfully"));
    }

    /**
     * Re-calculate tất cả customers' tier (Admin only)
     * POST /api/customer-tiers/recalculate-all
     * Use case: Sau khi admin đổi thresholds, cần update lại tất cả customers
     */
    @PostMapping("/recalculate-all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> recalculateAllCustomerTiers(
            @AuthenticationPrincipal UserDetails userDetails) {
        UUID adminId = ((UserDetailsImpl) userDetails).getId();
        customerTierService.recalculateAllCustomerTiers(adminId);
        return ResponseEntity.ok(ApiResponse.success(null, "All customer tiers recalculated successfully"));
    }
}