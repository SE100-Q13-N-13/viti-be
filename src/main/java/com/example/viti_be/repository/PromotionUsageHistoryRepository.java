package com.example.viti_be.repository;

import com.example.viti_be.model.PromotionUsageHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PromotionUsageHistoryRepository extends JpaRepository<PromotionUsageHistory, UUID> {

    /**
     * Đếm số lần customer đã dùng promotion
     */
    @Query("SELECT COUNT(puh) FROM PromotionUsageHistory puh " +
            "WHERE puh.promotion.id = :promotionId " +
            "AND puh.customer.id = :customerId")
    long countByPromotionIdAndCustomerId(
            @Param("promotionId") UUID promotionId,
            @Param("customerId") UUID customerId);

    /**
     * Lấy usage history của promotion
     */
    List<PromotionUsageHistory> findByPromotionIdOrderByUsedAtDesc(UUID promotionId);

    /**
     * Xóa usage history khi cancel order
     */
    void deleteByOrderId(UUID orderId);
}
