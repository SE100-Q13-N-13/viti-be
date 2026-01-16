package com.example.viti_be.controller;

import com.example.viti_be.dto.request.AdjustPointsRequest;
import com.example.viti_be.dto.request.ResetPointsRequest;
import com.example.viti_be.dto.request.UpdateLoyaltyConfigRequest;
import com.example.viti_be.dto.response.*;
import com.example.viti_be.security.services.UserDetailsImpl;
import com.example.viti_be.service.LoyaltyPointService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/loyalty-points")
@RequiredArgsConstructor
public class LoyaltyPointController {

    private final LoyaltyPointService loyaltyPointService;

    // ========== CUSTOMER POINTS INQUIRY ==========

    /**
     * Xem điểm tích lũy của customer
     * GET /api/loyalty-points/{customerId}
     */
    @GetMapping("/{customerId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CASHIER', 'ACCOUNTANT')")
    public ResponseEntity<ApiResponse<LoyaltyPointResponse>> getCustomerPoints(
            @PathVariable UUID customerId) {
        LoyaltyPointResponse response = loyaltyPointService.getCustomerPoints(customerId);
        return ResponseEntity.ok(ApiResponse.success(response, "Customer points retrieved successfully"));
    }

    /**
     * Xem lịch sử giao dịch điểm (phân trang)
     * GET /api/loyalty-points/{customerId}/transactions?page=0&size=20
     */
    @GetMapping("/{customerId}/transactions")
    @PreAuthorize("hasAnyRole('ADMIN', 'CASHIER', 'ACCOUNTANT')")
    public ResponseEntity<ApiResponse<Page<LoyaltyPointTransactionResponse>>> getTransactionHistory(
            @PathVariable UUID customerId,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<LoyaltyPointTransactionResponse> response = loyaltyPointService.getTransactionHistory(customerId, pageable);
        return ResponseEntity.ok(ApiResponse.success(response, "Transaction history retrieved successfully"));
    }

    /**
     * Xem lịch sử giao dịch + tổng hợp tháng này
     * GET /api/loyalty-points/{customerId}/transactions/detailed
     */
    @GetMapping("/{customerId}/transactions/detailed")
    @PreAuthorize("hasAnyRole('ADMIN', 'CASHIER', 'ACCOUNTANT')")
    public ResponseEntity<ApiResponse<TransactionHistoryResponse>> getDetailedTransactionHistory(
            @PathVariable UUID customerId,
            @PageableDefault(size = 20) Pageable pageable) {
        TransactionHistoryResponse response = loyaltyPointService.getDetailedTransactionHistory(customerId, pageable);
        return ResponseEntity.ok(ApiResponse.success(response, "Detailed history retrieved successfully"));
    }

    // ========== ADMIN OPERATIONS ==========

    /**
     * Điều chỉnh điểm thủ công (Admin only)
     * POST /api/loyalty-points/{customerId}/adjust
     * Body: { "pointsChange": 100, "reason": "Compensation for issue" }
     */
    @PostMapping("/{customerId}/adjust")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<LoyaltyPointResponse>> adjustPoints(
            @PathVariable UUID customerId,
            @Valid @RequestBody AdjustPointsRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        UUID adminId = ((UserDetailsImpl) userDetails).getId();
        LoyaltyPointResponse response = loyaltyPointService.adjustPoints(customerId, request, adminId);
        return ResponseEntity.ok(ApiResponse.success(response, "Points adjusted successfully"));
    }

    /**
     * Reset điểm của 1 customer (Admin only)
     * POST /api/loyalty-points/{customerId}/reset
     * Body: { "reason": "Annual reset" }
     */
    @PostMapping("/{customerId}/reset")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<LoyaltyPointResponse>> resetCustomerPoints(
            @PathVariable UUID customerId,
            @Valid @RequestBody ResetPointsRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        UUID adminId = ((UserDetailsImpl) userDetails).getId();
        LoyaltyPointResponse response = loyaltyPointService.resetCustomerPoints(customerId, request, adminId);
        return ResponseEntity.ok(ApiResponse.success(response, "Customer points reset successfully"));
    }

    /**
     * Reset tất cả customers (Admin only - Manual trigger)
     * POST /api/loyalty-points/reset-all
     * Body: { "reason": "Annual reset 2026" }
     */
    @PostMapping("/reset-all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ResetResultResponse>> resetAllCustomerPoints(
            @Valid @RequestBody ResetPointsRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        UUID adminId = ((UserDetailsImpl) userDetails).getId();
        ResetResultResponse response = loyaltyPointService.resetAllCustomerPoints(request.getReason(), adminId);
        return ResponseEntity.ok(ApiResponse.success(response, "All customer points reset successfully"));
    }

    // ========== CONFIG MANAGEMENT ==========

    /**
     * Lấy cấu hình loyalty hiện tại
     * GET /api/loyalty-points/config
     */
    @GetMapping("/config")
    @PreAuthorize("hasAnyRole('ADMIN', 'ACCOUNTANT')")
    public ResponseEntity<ApiResponse<LoyaltyConfigResponse>> getLoyaltyConfig() {
        LoyaltyConfigResponse response = loyaltyPointService.getLoyaltyConfig();
        return ResponseEntity.ok(ApiResponse.success(response, "Loyalty config retrieved successfully"));
    }

    /**
     * Cập nhật cấu hình loyalty (Admin only)
     * PUT /api/loyalty-points/config
     * Body: { "earnRate": 50000, "redeemRate": 1000, ... }
     */
    @PutMapping("/config")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<LoyaltyConfigResponse>> updateLoyaltyConfig(
            @Valid @RequestBody UpdateLoyaltyConfigRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        UUID adminId = ((UserDetailsImpl) userDetails).getId();
        LoyaltyConfigResponse response = loyaltyPointService.updateLoyaltyConfig(request, adminId);
        return ResponseEntity.ok(ApiResponse.success(response, "Loyalty config updated successfully"));
    }
}