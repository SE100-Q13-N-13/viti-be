package com.example.viti_be.repository;

import com.example.viti_be.model.LoyaltyPointTransaction;
import com.example.viti_be.model.model_enum.TransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface LoyaltyPointTransactionRepository extends JpaRepository<LoyaltyPointTransaction, UUID> {

    /**
     * Lấy lịch sử giao dịch của customer (phân trang)
     */
    @Query("SELECT t FROM LoyaltyPointTransaction t " +
            "WHERE t.loyaltyPoint.customer.id = :customerId " +
            "AND t.isDeleted = false " +
            "ORDER BY t.createdAt DESC")
    Page<LoyaltyPointTransaction> findByCustomerId(@Param("customerId") UUID customerId, Pageable pageable);

    /**
     * Lấy tất cả giao dịch theo loại
     */
    List<LoyaltyPointTransaction> findByTransactionTypeAndIsDeletedFalse(TransactionType transactionType);

    /**
     * Kiểm tra order đã tích điểm chưa (tránh duplicate)
     */
    boolean existsByOrderIdAndTransactionTypeAndIsDeletedFalse(UUID orderId, TransactionType transactionType);

    /**
     * Tổng điểm earned trong khoảng thời gian
     */
    @Query("SELECT COALESCE(SUM(t.pointsChange), 0) FROM LoyaltyPointTransaction t " +
            "WHERE t.loyaltyPoint.customer.id = :customerId " +
            "AND t.transactionType = 'EARN' " +
            "AND t.createdAt BETWEEN :startDate AND :endDate " +
            "AND t.isDeleted = false")
    Integer sumEarnedPointsByCustomerAndDateRange(
            @Param("customerId") UUID customerId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    /**
     * Tổng điểm redeemed trong khoảng thời gian
     */
    @Query("SELECT COALESCE(SUM(ABS(t.pointsChange)), 0) FROM LoyaltyPointTransaction t " +
            "WHERE t.loyaltyPoint.customer.id = :customerId " +
            "AND t.transactionType = 'REDEEM' " +
            "AND t.createdAt BETWEEN :startDate AND :endDate " +
            "AND t.isDeleted = false")
    Integer sumRedeemedPointsByCustomerAndDateRange(
            @Param("customerId") UUID customerId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);
}