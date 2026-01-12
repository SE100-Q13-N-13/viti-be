package com.example.viti_be.model;

import com.example.viti_be.model.model_enum.TransactionType;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "loyalty_point_transactions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoyaltyPointTransaction extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "loyalty_point_id", nullable = false)
    private LoyaltyPoint loyaltyPoint;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private Order order;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false, length = 50)
    private TransactionType transactionType;

    @Column(name = "points_change", nullable = false)
    private Integer pointsChange;

    @Column(name = "points_total_after", nullable = false)
    private Integer pointsTotalAfter;

    @Column(name = "points_available_after", nullable = false)
    private Integer pointsAvailableAfter;

    @Column(name = "reason", columnDefinition = "TEXT")
    private String reason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "performed_by")
    private User performedBy;

    // === FACTORY METHODS ===

    /**
     * Tạo giao dịch EARN (tích điểm từ đơn hàng)
     */
    public static LoyaltyPointTransaction createEarnTransaction(
            LoyaltyPoint loyaltyPoint,
            Order order,
            Integer points,
            User performedBy) {
        return LoyaltyPointTransaction.builder()
                .loyaltyPoint(loyaltyPoint)
                .order(order)
                .transactionType(TransactionType.EARN)
                .pointsChange(points)
                .pointsTotalAfter(loyaltyPoint.getTotalPoints())
                .pointsAvailableAfter(loyaltyPoint.getPointsAvailable())
                .reason("Earned from order #" + order.getId())
                .performedBy(performedBy)
                .build();
    }

    /**
     * Tạo giao dịch REDEEM (sử dụng điểm)
     */
    public static LoyaltyPointTransaction createRedeemTransaction(
            LoyaltyPoint loyaltyPoint,
            Order order,
            Integer points,
            User performedBy) {
        return LoyaltyPointTransaction.builder()
                .loyaltyPoint(loyaltyPoint)
                .order(order)
                .transactionType(TransactionType.REDEEM)
                .pointsChange(-points)
                .pointsTotalAfter(loyaltyPoint.getTotalPoints())
                .pointsAvailableAfter(loyaltyPoint.getPointsAvailable())
                .reason("Redeemed for order #" + order.getId())
                .performedBy(performedBy)
                .build();
    }

    /**
     * Tạo giao dịch MANUAL_ADJUST (điều chỉnh thủ công)
     */
    public static LoyaltyPointTransaction createManualAdjustTransaction(
            LoyaltyPoint loyaltyPoint,
            Integer points,
            String reason,
            User admin) {
        return LoyaltyPointTransaction.builder()
                .loyaltyPoint(loyaltyPoint)
                .transactionType(TransactionType.MANUAL_ADJUST)
                .pointsChange(points)
                .pointsTotalAfter(loyaltyPoint.getTotalPoints())
                .pointsAvailableAfter(loyaltyPoint.getPointsAvailable())
                .reason(reason)
                .performedBy(admin)
                .build();
    }

    /**
     * Tạo giao dịch RESET (reset định kỳ)
     */
    public static LoyaltyPointTransaction createResetTransaction(
            LoyaltyPoint loyaltyPoint,
            String reason,
            User admin) {
        Integer oldTotal = loyaltyPoint.getTotalPoints();
        return LoyaltyPointTransaction.builder()
                .loyaltyPoint(loyaltyPoint)
                .transactionType(TransactionType.RESET)
                .pointsChange(-oldTotal)
                .pointsTotalAfter(0)
                .pointsAvailableAfter(0)
                .reason(reason)
                .performedBy(admin)
                .build();
    }

    // === HELPER METHODS ===

    public String getTransactionSummary() {
        String sign = pointsChange >= 0 ? "+" : "";
        return String.format("%s%d points (%s)", sign, pointsChange, transactionType);
    }

    public boolean isPositiveChange() {
        return pointsChange > 0;
    }
}