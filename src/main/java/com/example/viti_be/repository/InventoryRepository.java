package com.example.viti_be.repository;

import com.example.viti_be.model.Inventory;
import com.example.viti_be.repository.projection.InventoryProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface InventoryRepository extends JpaRepository<Inventory, UUID> {
    
    Optional<Inventory> findByIdAndIsDeletedFalse(UUID id);
    
    Optional<Inventory> findByProductVariantIdAndIsDeletedFalse(UUID productVariantId);
    
    Page<Inventory> findAllByIsDeletedFalse(Pageable pageable);
    
    Page<Inventory> findByProductVariantIsNotNullAndIsDeletedFalse(Pageable pageable);
    
    Page<Inventory> findByPartComponentIdIsNotNullAndIsDeletedFalse(Pageable pageable);
    
    @Query("SELECT i FROM Inventory i WHERE i.productVariant.id = :productVariantId AND i.isDeleted = false")
    Optional<Inventory> findByProductVariantId(@Param("productVariantId") UUID productVariantId);
    
    @Query("SELECT i FROM Inventory i WHERE i.quantityAvailable <= i.minThreshold AND i.isDeleted = false")
    List<Inventory> findLowStockItems();
    
    @Query("SELECT i FROM Inventory i JOIN i.productVariant pv WHERE pv.product.id = :productId AND i.isDeleted = false")
    List<Inventory> findByProductId(@Param("productId") UUID productId);

    @Query("SELECT i FROM Inventory i WHERE i.partComponentId = :partComponentId AND i.isDeleted = false")
    Optional<Inventory> findByPartComponentId(@Param("partComponentId") UUID partComponentId);

    /**
     * Get inventory status with low stock flag
     */
    @Query("""
        SELECT 
            i.productVariant.id as productVariantId,
            i.productVariant.product.name as productName,
            i.productVariant.variantName as variantName,
            i.productVariant.sku as sku,
            i.quantityPhysical as quantityPhysical,
            i.quantityReserved as quantityReserved,
            i.quantityAvailable as quantityAvailable,
            i.minThreshold as minThreshold,
            CASE WHEN i.quantityAvailable < i.minThreshold THEN true ELSE false END as isLowStock
        FROM Inventory i
        WHERE (:categoryId IS NULL OR i.productVariant.product.category.id = :categoryId)
            AND (:lowStockOnly = false OR i.quantityAvailable < i.minThreshold)
    """)
    Page<InventoryProjection> getInventoryReport(
            @Param("categoryId") UUID categoryId,
            @Param("lowStockOnly") Boolean lowStockOnly,
            Pageable pageable
    );

    /**
     * Get fast-moving products (high sales velocity)
     */
    @Query(value = """
        SELECT 
            pv.id as productVariantId,
            p.name as productName,
            pv.variant_name as variantName,
            pv.sku as sku,
            COALESCE(SUM(oi.quantity), 0) as quantitySold,
            EXTRACT(DAY FROM (CURRENT_DATE - MIN(o.created_at))) as daysSinceFirstSale,
            CASE 
                WHEN EXTRACT(DAY FROM (CURRENT_DATE - MIN(o.created_at))) > 0 
                THEN COALESCE(SUM(oi.quantity), 0)::DECIMAL / EXTRACT(DAY FROM (CURRENT_DATE - MIN(o.created_at)))
                ELSE 0 
            END as averageDailySales
        FROM product_variants pv
        INNER JOIN products p ON pv.product_id = p.id
        LEFT JOIN order_items oi ON oi.product_variant_id = pv.id
        LEFT JOIN orders o ON oi.order_id = o.id 
            AND o.created_at >= :startDate 
            AND o.status = 'COMPLETED'
        WHERE (:categoryId IS NULL OR p.category_id = CAST(:categoryId AS UUID))
        GROUP BY pv.id, p.name, pv.variant_name, pv.sku
        HAVING COALESCE(SUM(oi.quantity), 0) > 0
        ORDER BY averageDailySales DESC
    """, nativeQuery = true)
    List<Object[]> getFastMovingProductsNative(
            @Param("startDate") LocalDateTime startDate,
            @Param("categoryId") String categoryId
    );

    /**
     * Count low stock products (only product variants, not parts)
     */
    @Query("SELECT COUNT(i) FROM Inventory i WHERE i.productVariant IS NOT NULL AND i.quantityAvailable < i.minThreshold AND i.isDeleted = false")
    Long countLowStockProducts();

    /**
     * Count low stock components (parts)
     */
    @Query("SELECT COUNT(i) FROM Inventory i WHERE i.partComponentId IS NOT NULL AND i.quantityAvailable < i.minThreshold AND i.isDeleted = false")
    Long countLowStockComponents();

    /**
     * Count out of stock products
     */
    @Query("SELECT COUNT(i) FROM Inventory i WHERE i.quantityAvailable = 0")
    Long countOutOfStockProducts();

    /**
     * Calculate total stock value for products (quantity * selling price)
     */
    @Query(value = """
        SELECT COALESCE(SUM(i.quantity_available * pv.selling_price), 0)
        FROM inventory i
        INNER JOIN product_variants pv ON i.product_variant_id = pv.id
        WHERE i.is_deleted = false AND pv.is_deleted = false
    """, nativeQuery = true)
    BigDecimal calculateTotalProductStockValue();

    /**
     * Calculate total stock value for parts/components
     */
    @Query(value = """
        SELECT COALESCE(SUM(i.quantity_available * pc.selling_price), 0)
        FROM inventory i
        INNER JOIN part_components pc ON i.part_component_id = pc.id
        WHERE i.is_deleted = false AND pc.is_deleted = false
    """, nativeQuery = true)
    BigDecimal calculateTotalComponentStockValue();

    /**
     * Get low stock products (product variants with quantity below threshold)
     */
    @Query("SELECT i FROM Inventory i JOIN FETCH i.productVariant pv JOIN FETCH pv.product p " +
           "WHERE i.productVariant IS NOT NULL AND i.quantityAvailable < i.minThreshold AND i.isDeleted = false " +
           "ORDER BY (i.quantityAvailable * 1.0 / NULLIF(i.minThreshold, 0)) ASC")
    List<Inventory> findLowStockProducts();

    /**
     * Get low stock components
     */
    @Query("SELECT i FROM Inventory i WHERE i.partComponentId IS NOT NULL AND i.quantityAvailable < i.minThreshold AND i.isDeleted = false")
    List<Inventory> findLowStockComponents();

    /**
     * Get all product inventories
     */
    @Query("SELECT i FROM Inventory i JOIN FETCH i.productVariant pv JOIN FETCH pv.product WHERE i.productVariant IS NOT NULL AND i.isDeleted = false")
    List<Inventory> findAllProductInventories();

    /**
     * Get all component inventories
     */
    @Query("SELECT i FROM Inventory i WHERE i.partComponentId IS NOT NULL AND i.isDeleted = false")
    List<Inventory> findAllComponentInventories();
}
