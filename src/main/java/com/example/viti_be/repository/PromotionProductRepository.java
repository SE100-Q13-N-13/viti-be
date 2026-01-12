package com.example.viti_be.repository;

import com.example.viti_be.model.PromotionProduct;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PromotionProductRepository extends JpaRepository<PromotionProduct, UUID> {
    List<PromotionProduct> findByPromotionId(UUID promotionId);
    void deleteByPromotionId(UUID promotionId);
}
