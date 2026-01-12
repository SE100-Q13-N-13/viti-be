package com.example.viti_be.repository;

import com.example.viti_be.model.OrderPromotion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface OrderPromotionRepository extends JpaRepository<OrderPromotion, UUID> {
    List<OrderPromotion> findByOrderId(UUID orderId);
    void deleteByOrderId(UUID orderId);
}
