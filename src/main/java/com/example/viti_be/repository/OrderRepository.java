package com.example.viti_be.repository;

import com.example.viti_be.model.Order;
import com.example.viti_be.model.model_enum.OrderStatus;
import com.example.viti_be.model.model_enum.OrderType;
import com.example.viti_be.model.model_enum.PaymentMethod;
import com.example.viti_be.repository.projection.ProfitProjection;
import com.example.viti_be.repository.projection.RevenueByTypeProjection;
import com.example.viti_be.repository.projection.RevenueProjection;
import com.example.viti_be.repository.projection.TopProductProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<Order, UUID> {
    Optional<Order> findByOrderNumber(String orderNumber);
    Page<Order> findByCustomer_Id(UUID customerId, Pageable pageable);
    Page<Order> findByEmployee_Id(UUID employeeId, Pageable pageable);
    @Query("SELECT o FROM Order o WHERE o.status = :status " +
            "AND o.createdAt BETWEEN :startDate AND :endDate")
    List<Order> findByStatusAndDateRange(
            @Param("status") OrderStatus status,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    // Count orders by customer for tier calculation
    @Query("SELECT COUNT(o) FROM Order o WHERE o.customer.id = :customerId " +
            "AND o.status = 'COMPLETED'")
    Long countCompletedOrdersByCustomer(@Param("customerId") UUID customerId);

    // ============================================
    // REVENUE QUERIES
    // ============================================

    /**
     * Get revenue breakdown by DAY
     */
    @Query("""
        SELECT 
            CAST(o.createdAt AS LocalDate) as period,
            SUM(o.finalAmount) as totalRevenue,
            COUNT(o.id) as orderCount
        FROM Order o
        WHERE o.createdAt BETWEEN :startDate AND :endDate
            AND (:status IS NULL OR o.status = :status)
            AND (:orderType IS NULL OR o.orderType = :orderType)
            AND (:paymentMethod IS NULL OR o.paymentMethod = :paymentMethod)
            AND (:employeeId IS NULL OR o.employee.id = :employeeId)
            AND (:customerTier IS NULL OR o.customerTier.id = :customerTier)
        GROUP BY CAST(o.createdAt AS LocalDate)
        ORDER BY period
    """)
    List<RevenueProjection> getRevenueByDay(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            @Param("status") OrderStatus status,
            @Param("orderType") OrderType orderType,
            @Param("paymentMethod") PaymentMethod paymentMethod,
            @Param("employeeId") UUID employeeId,
            @Param("customerTier") UUID customerTier
    );

    /**
     * Get revenue breakdown by WEEK
     */
    @Query(value = """
        SELECT 
            DATE_TRUNC('week', o.created_at) as period,
            SUM(o.final_amount) as totalRevenue,
            COUNT(o.id) as orderCount
        FROM orders o
        WHERE o.created_at BETWEEN :startDate AND :endDate
            AND (:status IS NULL OR o.status = CAST(:status AS VARCHAR))
            AND (:orderType IS NULL OR o.order_type = CAST(:orderType AS VARCHAR))
            AND (:paymentMethod IS NULL OR o.payment_method = CAST(:paymentMethod AS VARCHAR))
            AND (:employeeId IS NULL OR o.employee_id = CAST(:employeeId AS UUID))
            AND (:customerTier IS NULL OR o.customer_tier_id = CAST(:customerTier AS UUID))
        GROUP BY DATE_TRUNC('week', o.created_at)
        ORDER BY period
    """, nativeQuery = true)
    List<Object[]> getRevenueByWeekNative(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            @Param("status") String status,
            @Param("orderType") String orderType,
            @Param("paymentMethod") String paymentMethod,
            @Param("employeeId") String employeeId,
            @Param("customerTier") String customerTier
    );

    /**
     * Get revenue breakdown by MONTH
     */
    @Query(value = """
        SELECT 
            DATE_TRUNC('month', o.created_at) as period,
            SUM(o.final_amount) as totalRevenue,
            COUNT(o.id) as orderCount
        FROM orders o
        WHERE o.created_at BETWEEN :startDate AND :endDate
            AND (:status IS NULL OR o.status = CAST(:status AS VARCHAR))
            AND (:orderType IS NULL OR o.order_type = CAST(:orderType AS VARCHAR))
            AND (:paymentMethod IS NULL OR o.payment_method = CAST(:paymentMethod AS VARCHAR))
            AND (:employeeId IS NULL OR o.employee_id = CAST(:employeeId AS UUID))
            AND (:customerTier IS NULL OR o.customer_tier_id = CAST(:customerTier AS UUID))
        GROUP BY DATE_TRUNC('month', o.created_at)
        ORDER BY period
    """, nativeQuery = true)
    List<Object[]> getRevenueByMonthNative(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            @Param("status") String status,
            @Param("orderType") String orderType,
            @Param("paymentMethod") String paymentMethod,
            @Param("employeeId") String employeeId,
            @Param("customerTier") String customerTier
    );

    /**
     * Get revenue breakdown by Order Type
     */
    @Query("""
        SELECT 
            o.orderType as orderType,
            SUM(o.finalAmount) as totalRevenue,
            COUNT(o.id) as orderCount
        FROM Order o
        WHERE o.createdAt BETWEEN :startDate AND :endDate
            AND (:status IS NULL OR o.status = :status)
            AND (:customerTier IS NULL OR o.customerTier.id = :customerTier)
        GROUP BY o.orderType
        ORDER BY totalRevenue DESC
    """)
    List<RevenueByTypeProjection> getRevenueByOrderType(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            @Param("status") OrderStatus status,
            @Param("customerTier") UUID customerTier
    );

    // ============================================
    // PROFIT QUERIES
    // ============================================

    /**
     * Get profit by product variant
     */
    @Query("""
        SELECT 
            oi.productVariant.id as productVariantId,
            oi.product.name as productName,
            oi.productVariant.variantName as variantName,
            oi.productVariant.sku as sku,
            SUM(oi.quantity) as quantitySold,
            SUM(oi.subtotal - COALESCE(oi.discount, 0)) as totalRevenue,
            SUM(oi.quantity * oi.costPrice) as totalCost,
            SUM(oi.subtotal - COALESCE(oi.discount, 0) - (oi.quantity * oi.costPrice)) as grossProfit
        FROM OrderItem oi
        JOIN oi.order o
        WHERE o.createdAt BETWEEN :startDate AND :endDate
            AND (:status IS NULL OR o.status = :status)
            AND (:categoryId IS NULL OR oi.product.category.id = :categoryId)
            AND (:supplierId IS NULL OR oi.product.supplier.id = :supplierId)
        GROUP BY oi.productVariant.id, oi.product.name, oi.productVariant.variantName, oi.productVariant.sku
        ORDER BY grossProfit DESC
    """)
    List<ProfitProjection> getProfitByProduct(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            @Param("status") OrderStatus status,
            @Param("categoryId") UUID categoryId,
            @Param("supplierId") UUID supplierId
    );

    // ============================================
    // TOP PRODUCTS QUERIES
    // ============================================

    /**
     * Get top products by quantity sold
     */
    @Query("""
        SELECT 
            oi.productVariant.id as productVariantId,
            oi.product.name as productName,
            oi.productVariant.variantName as variantName,
            oi.productVariant.sku as sku,
            SUM(oi.quantity) as quantitySold,
            SUM(oi.subtotal - COALESCE(oi.discount, 0)) as totalRevenue
        FROM OrderItem oi
        JOIN oi.order o
        WHERE o.createdAt BETWEEN :startDate AND :endDate
            AND (:status IS NULL OR o.status = :status)
            AND (:categoryId IS NULL OR oi.product.category.id = :categoryId)
        GROUP BY oi.productVariant.id, oi.product.name, oi.productVariant.variantName, oi.productVariant.sku
        ORDER BY quantitySold DESC
    """)
    List<TopProductProjection> getTopProductsByQuantity(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            @Param("status") OrderStatus status,
            @Param("categoryId") UUID categoryId
    );

    /**
     * Get top products by revenue
     */
    @Query("""
        SELECT 
            oi.productVariant.id as productVariantId,
            oi.product.name as productName,
            oi.productVariant.variantName as variantName,
            oi.productVariant.sku as sku,
            SUM(oi.quantity) as quantitySold,
            SUM(oi.subtotal - COALESCE(oi.discount, 0)) as totalRevenue
        FROM OrderItem oi
        JOIN oi.order o
        WHERE o.createdAt BETWEEN :startDate AND :endDate
            AND (:status IS NULL OR o.status = :status)
            AND (:categoryId IS NULL OR oi.product.category.id = :categoryId)
        GROUP BY oi.productVariant.id, oi.product.name, oi.productVariant.variantName, oi.productVariant.sku
        ORDER BY totalRevenue DESC
    """)
    List<TopProductProjection> getTopProductsByRevenue(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            @Param("status") OrderStatus status,
            @Param("categoryId") UUID categoryId
    );
}
