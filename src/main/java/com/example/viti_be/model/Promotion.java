package com.example.viti_be.model;

import com.example.viti_be.model.model_enum.PromotionScope;
import com.example.viti_be.model.model_enum.PromotionStatus;
import com.example.viti_be.model.model_enum.PromotionType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "promotions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Promotion extends BaseEntity {

    @Column(unique = true, length = 50)
    private String code; // SUMMER2025, NEWYEAR10, etc. (UPPERCASE, unique)

    @Column(nullable = false, length = 100)
    private String name; // "Giảm giá mùa hè"

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PromotionType type; // PERCENTAGE, FIXED_AMOUNT

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PromotionScope scope; // PRODUCT, ORDER

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal value; // Giá trị giảm (10.00 = 10% hoặc 100000 = 100K VND)

    @Column(name = "min_order_value", precision = 15, scale = 2)
    private BigDecimal minOrderValue; // Đơn tối thiểu để áp dụng

    @Column(name = "max_discount_amount", precision = 15, scale = 2)
    private BigDecimal maxDiscountAmount; // Giảm tối đa (cho PERCENTAGE)

    @Column(name = "applicable_customer_tiers", columnDefinition = "TEXT")
    private String applicableCustomerTiers; // JSON: ["GOLD", "PLATINUM"]

    @Column(name = "start_date", nullable = false)
    private LocalDateTime startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDateTime endDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PromotionStatus status; // SCHEDULED, ACTIVE, INACTIVE, EXPIRED

    @Column(name = "usage_limit")
    private Integer usageLimit; // Tổng số lần sử dụng tối đa (null = unlimited)

    @Column(name = "usage_count")
    private Integer usageCount; // Số lần đã sử dụng

    @Column(name = "usage_per_customer")
    private Integer usagePerCustomer; // Mỗi customer dùng tối đa X lần (null = unlimited)

    @Column(nullable = false)
    private Integer priority; // Số càng lớn ưu tiên càng cao (dùng khi chọn promotion)

    @Column(name = "requires_code")
    @Builder.Default
    private Boolean requiresCode = false;

    @Column(columnDefinition = "TEXT")
    private String description;

    // === RELATIONSHIPS ===

    @OneToMany(mappedBy = "promotion", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<PromotionCategory> promotionCategories = new HashSet<>();

    @OneToMany(mappedBy = "promotion", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<PromotionProduct> promotionProducts = new HashSet<>();

    @OneToMany(mappedBy = "promotion", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<PromotionConflict> conflicts = new HashSet<>();

    @OneToMany(mappedBy = "conflictingPromotion", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<PromotionConflict> conflictedBy = new HashSet<>();

    // === HELPER METHODS ===

    /**
     * Kiểm tra promotion có đang hoạt động không
     */
    public boolean isActive() {
        LocalDateTime now = LocalDateTime.now();
        return status == PromotionStatus.ACTIVE
                && now.isAfter(startDate)
                && now.isBefore(endDate);
    }

    /**
     * Kiểm tra promotion có hết hạn không
     */
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(endDate);
    }

    /**
     * Kiểm tra còn quota sử dụng không
     */
    public boolean hasQuota() {
        if (usageLimit == null) return true; // Unlimited
        return usageCount < usageLimit;
    }

    /**
     * Tăng usage count khi sử dụng
     */
    public void incrementUsage() {
        this.usageCount = (this.usageCount != null ? this.usageCount : 0) + 1;
    }

    /**
     * Giảm usage count khi cancel order
     */
    public void decrementUsage() {
        if (this.usageCount != null && this.usageCount > 0) {
            this.usageCount--;
        }
    }

    /**
     * Tính discount amount cho 1 giá trị
     */
    public BigDecimal calculateDiscount(BigDecimal baseAmount) {
        BigDecimal discount;

        if (type == PromotionType.PERCENTAGE) {
            discount = baseAmount.multiply(value).divide(new BigDecimal(100), 2, java.math.RoundingMode.HALF_DOWN);

            // Cap max discount nếu có
            if (maxDiscountAmount != null && discount.compareTo(maxDiscountAmount) > 0) {
                discount = maxDiscountAmount;
            }
        } else {
            discount = value;
        }

        // Không cho discount vượt quá base amount
        return discount.min(baseAmount);
    }
}