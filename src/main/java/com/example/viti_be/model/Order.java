package com.example.viti_be.model;

import com.example.viti_be.model.model_enum.OrderStatus;
import com.example.viti_be.model.model_enum.OrderType;
import com.example.viti_be.model.model_enum.PaymentMethod;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(name = "orders")
public class Order extends BaseEntity {

    @Column(name = "order_number", unique = true, nullable = false, length = 50)
    private String orderNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = true)
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id")
    private User employee;

    @Enumerated(EnumType.STRING)
    @Column(name = "order_type", length = 20)
    private OrderType orderType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_tier_id")
    private CustomerTier customerTier;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<OrderItem> items = new ArrayList<>();

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL)
    private List<OrderPromotion> appliedPromotions;

    @Column(name = "loyalty_points_used")
    @Builder.Default
    private Integer loyaltyPointsUsed = 0;

    // ==========================================
    // KHU VỰC TÀI CHÍNH (Dùng BigDecimal)
    // ==========================================

    @Column(name = "subtotal", precision = 15, scale = 2)
    private BigDecimal subtotal;

    @Column(name = "total_discount", precision = 15, scale = 2)
    private BigDecimal totalDiscount;

    @Column(name = "final_amount", precision = 15, scale = 2)
    private BigDecimal finalAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", length = 20)
    private PaymentMethod paymentMethod;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20)
    private OrderStatus status;

    @Column(name = "invoice_url")
    private String invoiceUrl;

    // ==========================================
    // KHU VỰC SNAPSHOT (Lưu vết lịch sử)
    // Dữ liệu này không thay đổi dù cấu hình hệ thống có đổi
    // ==========================================

    @Column(name = "tier_name_snapshot")
    private String tierName;

    @Column(name = "tier_discount_rate")
    private BigDecimal tierDiscountRate;

    @Column(name = "tier_discount_amount", precision = 15, scale = 2)
    private BigDecimal tierDiscountAmount;

    @Column(name = "point_rate_snapshot", precision = 10, scale = 2)
    private BigDecimal pointRateSnapshot;

    @Column(name = "point_discount_amount", precision = 15, scale = 2)
    private BigDecimal pointDiscountAmount;
}


