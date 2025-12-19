package com.example.viti_be.repository;

import com.example.viti_be.model.PurchaseOrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PurchaseOrderItemRepository extends JpaRepository<PurchaseOrderItem, UUID> {
    
    List<PurchaseOrderItem> findAllByPurchaseOrderId(UUID purchaseOrderId);
    
    List<PurchaseOrderItem> findAllByPurchaseOrderIdAndIsDeletedFalse(UUID purchaseOrderId);
    
    Optional<PurchaseOrderItem> findByPurchaseOrderIdAndProductVariantId(UUID purchaseOrderId, UUID productVariantId);
    
    Optional<PurchaseOrderItem> findByIdAndIsDeletedFalse(UUID id);
}
