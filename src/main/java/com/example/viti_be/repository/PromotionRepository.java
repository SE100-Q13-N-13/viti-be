package com.example.viti_be.repository;

import com.example.viti_be.model.*;
import com.example.viti_be.model.model_enum.PromotionScope;
import com.example.viti_be.model.model_enum.PromotionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PromotionRepository extends JpaRepository<Promotion, UUID> {

    Optional<Promotion> findByCodeAndIsDeletedFalse(String code);

    List<Promotion> findByIsDeletedFalse();
    Page<Promotion> findByIsDeletedFalse(Pageable pageable);

    List<Promotion> findByStatusAndIsDeletedFalse(PromotionStatus status);

    /**
     * Tìm active promotions (ACTIVE + trong thời gian hiệu lực)
     */
    @Query("SELECT p FROM Promotion p WHERE p.status = 'ACTIVE' " +
            "AND p.startDate <= :now AND p.endDate > :now " +
            "AND p.isDeleted = false")
    Page<Promotion> findActivePromotions(@Param("now") LocalDateTime now, Pageable pageable);

    /**
     * Tìm promotions cần auto-expire (end_date qua + status vẫn ACTIVE)
     */
    @Query("SELECT p FROM Promotion p WHERE p.status = 'ACTIVE' " +
            "AND p.endDate < :now AND p.isDeleted = false")
    List<Promotion> findPromotionsToExpire(@Param("now") LocalDateTime now);

    /**
     * Tìm promotions cần auto-activate (start_date đến + status SCHEDULED)
     */
    @Query("SELECT p FROM Promotion p WHERE p.status = 'SCHEDULED' " +
            "AND p.startDate <= :now AND p.endDate > :now " +
            "AND p.isDeleted = false")
    List<Promotion> findPromotionsToActivate(@Param("now") LocalDateTime now);

    /**
     * Tìm applicable promotions cho product
     */
    @Query("SELECT DISTINCT p FROM Promotion p " +
            "LEFT JOIN p.promotionProducts pp " +
            "LEFT JOIN p.promotionCategories pc " +
            "WHERE p.status = 'ACTIVE' " +
            "AND p.scope = 'PRODUCT' " +
            "AND p.startDate <= :now AND p.endDate > :now " +
            "AND p.isDeleted = false " +
            "AND (pp.product.id = :productId OR pc.category.id = :categoryId)")
    List<Promotion> findApplicableProductPromotions(
            @Param("productId") UUID productId,
            @Param("categoryId") UUID categoryId,
            @Param("now") LocalDateTime now);

    /**
     * Tìm applicable promotions cho order
     */
    @Query("SELECT p FROM Promotion p WHERE p.status = 'ACTIVE' " +
            "AND p.scope = 'ORDER' " +
            "AND p.startDate <= :now AND p.endDate > :now " +
            "AND p.isDeleted = false " +
            "ORDER BY p.priority DESC")
    List<Promotion> findApplicableOrderPromotions(@Param("now") LocalDateTime now);
}