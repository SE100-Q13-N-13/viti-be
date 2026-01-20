package com.example.viti_be.repository;

import com.example.viti_be.model.Inventory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface InventoryRepository extends JpaRepository<Inventory, UUID> {
    
    Optional<Inventory> findByIdAndIsDeletedFalse(UUID id);
    
    Optional<Inventory> findByProductVariantIdAndIsDeletedFalse(UUID productVariantId);
    
    Page<Inventory> findAllByIsDeletedFalse(Pageable pageable);
    
    @Query("SELECT i FROM Inventory i WHERE i.productVariant.id = :productVariantId AND i.isDeleted = false")
    Optional<Inventory> findByProductVariantId(@Param("productVariantId") UUID productVariantId);
    
    @Query("SELECT i FROM Inventory i WHERE i.quantityAvailable <= i.minThreshold AND i.isDeleted = false")
    List<Inventory> findLowStockItems();
    
    @Query("SELECT i FROM Inventory i JOIN i.productVariant pv WHERE pv.product.id = :productId AND i.isDeleted = false")
    List<Inventory> findByProductId(@Param("productId") UUID productId);

    @Query("SELECT i FROM Inventory i WHERE i.partComponentId = :partComponentId AND i.isDeleted = false")
    Optional<Inventory> findByPartComponentId(@Param("partComponentId") UUID partComponentId);
}
