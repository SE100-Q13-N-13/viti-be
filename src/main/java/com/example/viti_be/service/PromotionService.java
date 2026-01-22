package com.example.viti_be.service;

import com.example.viti_be.dto.request.ApplyPromotionCodeRequest;
import com.example.viti_be.dto.request.PromotionRequest;
import com.example.viti_be.dto.response.*;
import com.example.viti_be.dto.response.pagnitation.PageResponse;
import com.example.viti_be.model.Order;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

public interface PromotionService {

    // ========== CRUD OPERATIONS ==========

    /**
     * Tạo promotion mới (Admin)
     */
    PromotionResponse createPromotion(PromotionRequest request, UUID adminId);

    /**
     * Cập nhật promotion (Admin)
     * Yêu cầu: Promotion phải INACTIVE trước khi update
     */
    PromotionResponse updatePromotion(UUID id, PromotionRequest request, UUID adminId);

    /**
     * Xóa promotion (Admin)
     */
    void deletePromotion(UUID id, UUID adminId);

    /**
     * Lấy promotion theo ID
     */
    PromotionResponse getPromotionById(UUID id);

    /**
     * Lấy tất cả promotions
     */
    PageResponse<PromotionResponse> getAllPromotions(Pageable pageable);

    /**
     * Lấy active promotions
     */
    PageResponse<PromotionResponse> getActivePromotions(Pageable pageable);

    @Transactional(readOnly = true)
    List<PromotionResponse> getApplicablePromotionsForCart(ApplyPromotionCodeRequest request);

    /**
     * Toggle promotion status (ACTIVE <-> INACTIVE)
     */
    PromotionResponse togglePromotionStatus(UUID id, UUID adminId);

    // ========== CART OPERATIONS ==========

    /**
     * Apply promotion code vào cart
     * Validate: code exists, active, customer eligible, quota available
     */
    CartDiscountCalculationResponse applyPromotionCode(ApplyPromotionCodeRequest request);

    /**
     * Get applicable promotions for current cart (auto-apply)
     */
    CartDiscountCalculationResponse calculateCartDiscount(ApplyPromotionCodeRequest request);

    /**
     * Remove promotion from cart
     */
    CartDiscountCalculationResponse removePromotionFromCart(String code, ApplyPromotionCodeRequest cartRequest);

    // ========== ORDER INTEGRATION ==========

    /**
     * Apply promotions when creating order
     * Called from OrderService.createOrder()
     */
    void applyPromotionsToOrder(Order order, List<String> promotionCodes, UUID actorId);

    /**
     * Re-validate promotions when confirming order
     * Called from OrderService.confirmOrder()
     */
    void validateAndConfirmPromotions(Order order, UUID actorId);

    /**
     * Restore promotion usage when canceling order
     * Called from OrderService.cancelOrder()
     */
    void restorePromotionUsage(Order order, UUID actorId);

    // ========== SCHEDULED JOBS ==========

    /**
     * Auto-activate promotions khi start_date đến
     */
    void autoActivatePromotions();

    /**
     * Auto-expire promotions khi end_date qua
     */
    void autoExpirePromotions();

    // ========== REPORTS ==========

    /**
     * Usage report của promotion
     */
    PromotionUsageReportResponse getPromotionUsageReport(UUID promotionId);

    /**
     * Top promotions by usage/discount
     */
    List<PromotionUsageReportResponse> getTopPromotions(int limit);
}