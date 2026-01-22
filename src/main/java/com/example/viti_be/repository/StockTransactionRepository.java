package com.example.viti_be.repository;

import com.example.viti_be.model.StockTransaction;
import com.example.viti_be.model.model_enum.StockTransactionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface StockTransactionRepository extends JpaRepository<StockTransaction, UUID> {
    
    List<StockTransaction> findAllByInventoryIdAndIsDeletedFalse(UUID inventoryId);
    
    List<StockTransaction> findAllByTypeAndIsDeletedFalse(StockTransactionType type);
    
    List<StockTransaction> findAllByReferenceIdAndIsDeletedFalse(String referenceId);
    
    @Query("SELECT st FROM StockTransaction st WHERE st.inventory.id = :inventoryId AND st.createdAt BETWEEN :startDate AND :endDate AND st.isDeleted = false ORDER BY st.createdAt DESC")
    List<StockTransaction> findByInventoryIdAndDateRange(
            @Param("inventoryId") UUID inventoryId, 
            @Param("startDate") LocalDateTime startDate, 
            @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT st FROM StockTransaction st WHERE st.isDeleted = false ORDER BY st.createdAt DESC")
    List<StockTransaction> findAllOrderByCreatedAtDesc();

    /**
     * Get stock transactions within date range for chart data
     */
    @Query("SELECT st FROM StockTransaction st WHERE st.createdAt BETWEEN :startDate AND :endDate AND st.isDeleted = false ORDER BY st.createdAt ASC")
    List<StockTransaction> findByDateRange(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    /**
     * Get product stock transactions within date range
     */
    @Query("SELECT st FROM StockTransaction st WHERE st.inventory.productVariant IS NOT NULL AND st.createdAt BETWEEN :startDate AND :endDate AND st.isDeleted = false ORDER BY st.createdAt ASC")
    List<StockTransaction> findProductTransactionsByDateRange(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    /**
     * Get component stock transactions within date range
     */
    @Query("SELECT st FROM StockTransaction st WHERE st.partComponentId IS NOT NULL AND st.createdAt BETWEEN :startDate AND :endDate AND st.isDeleted = false ORDER BY st.createdAt ASC")
    List<StockTransaction> findComponentTransactionsByDateRange(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);
}
