package com.example.viti_be.service;

import com.example.viti_be.dto.request.AdjustPointsRequest;
import com.example.viti_be.dto.request.ResetPointsRequest;
import com.example.viti_be.dto.request.UpdateLoyaltyConfigRequest;
import com.example.viti_be.dto.response.*;
import com.example.viti_be.model.CustomerTier;
import com.example.viti_be.model.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.UUID;

public interface LoyaltyPointService {

    // ========== CUSTOMER OPERATIONS ==========

    /**
     * Lấy thông tin điểm của customer
     */
    LoyaltyPointResponse getCustomerPoints(UUID customerId);

    /**
     * Lấy lịch sử giao dịch điểm (phân trang)
     */
    Page<LoyaltyPointTransactionResponse> getTransactionHistory(UUID customerId, Pageable pageable);

    /**
     * Lấy lịch sử giao dịch + tổng hợp tháng này
     */
    TransactionHistoryResponse getDetailedTransactionHistory(UUID customerId, Pageable pageable);

    // ========== EARN & REDEEM OPERATIONS ==========

    /**
     * Tích điểm khi confirm order
     * Được gọi từ OrderService.confirmOrder()
     */
    Integer earnPointsFromOrder(Order order, UUID employeeId);

    /**
     * Validate xem có thể dùng X điểm cho đơn hàng không
     * Được gọi trước khi tạo order
     */
    void validateRedemption(UUID customerId, Integer pointsToRedeem, BigDecimal orderSubtotal);

    /**
     * Tính số tiền được giảm khi dùng điểm
     */
    BigDecimal calculateRedemptionAmount(Integer points);

    // ========== ADMIN OPERATIONS ==========

    /**
     * Điều chỉnh điểm thủ công (Admin only)
     */
    LoyaltyPointResponse adjustPoints(UUID customerId, AdjustPointsRequest request, UUID adminId);

    /**
     * Reset điểm của 1 customer (Admin only)
     */
    LoyaltyPointResponse resetCustomerPoints(UUID customerId, ResetPointsRequest request, UUID adminId);

    /**
     * Reset tất cả customers (Scheduled job hoặc manual)
     */
    ResetResultResponse resetAllCustomerPoints(String reason, UUID adminId);

    // ========== TIER MANAGEMENT ==========

    /**
     * Cập nhật tier của customer dựa trên total_points
     * Được gọi sau mỗi lần earn points
     */
    void updateCustomerTier(UUID customerId);

    /**
     * Lấy tier object dựa trên số điểm
     */
    CustomerTier getTierByPoints(Integer points);

    /**
     * Tính số điểm cần để lên tier tiếp theo
     */
    Integer getPointsToNextTier(Integer currentPoints);

    // ========== CONFIG MANAGEMENT ==========

    /**
     * Lấy cấu hình loyalty hiện tại
     */
    LoyaltyConfigResponse getLoyaltyConfig();

    /**
     * Cập nhật cấu hình loyalty (Admin only)
     */
    LoyaltyConfigResponse updateLoyaltyConfig(UpdateLoyaltyConfigRequest request, UUID adminId);
}