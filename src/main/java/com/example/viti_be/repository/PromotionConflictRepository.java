package com.example.viti_be.repository;

import com.example.viti_be.model.PromotionConflict;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PromotionConflictRepository extends JpaRepository<PromotionConflict, UUID> {

    @Query("SELECT pc.conflictingPromotion.id FROM PromotionConflict pc " +
            "WHERE pc.promotion.id = :promotionId")
    List<UUID> findConflictingPromotionIds(@Param("promotionId") UUID promotionId);

    void deleteByPromotionId(UUID promotionId);
}