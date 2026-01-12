package com.example.viti_be.service;

import com.example.viti_be.dto.request.CustomerTierRequest;
import com.example.viti_be.dto.response.CustomerTierResponse;

import java.util.List;
import java.util.UUID;

public interface CustomerTierService {

    /**
     * Lấy tất cả tiers (active + inactive)
     */
    List<CustomerTierResponse> getAllTiers();

    /**
     * Lấy tất cả tiers đang active
     */
    List<CustomerTierResponse> getActiveTiers();

    /**
     * Lấy tier theo ID
     */
    CustomerTierResponse getTierById(UUID id);

    /**
     * Tạo tier mới (Admin only)
     */
    CustomerTierResponse createTier(CustomerTierRequest request, UUID adminId);

    /**
     * Cập nhật tier (Admin only)
     */
    CustomerTierResponse updateTier(UUID id, CustomerTierRequest request, UUID adminId);

    /**
     * Xóa/vô hiệu hóa tier (Admin only)
     * Không cho xóa nếu có customers đang ở tier này
     */
    void deleteTier(UUID id, UUID adminId);

    /**
     * Active/deactivate tier
     */
    CustomerTierResponse toggleTierStatus(UUID id, UUID adminId);

    /**
     * Re-calculate tất cả customers' tier sau khi thay đổi thresholds
     * Use case: Admin đổi Gold từ 5000 → 7000 điểm
     */
    void recalculateAllCustomerTiers(UUID adminId);
}