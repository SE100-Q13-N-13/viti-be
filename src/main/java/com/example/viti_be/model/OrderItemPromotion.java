package com.example.viti_be.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "order_item_promotions") // Bảng lưu khuyến mãi cấp sản phẩm
@Getter
@Setter
public class OrderItemPromotion {
    @Id
    @GeneratedValue
    private UUID id;

    // Link về OrderItem
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_item_id")
    private OrderItem orderItem;

    // Link sang Promotion (Có thể dùng chung bảng Promotion với Order)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "promotion_id")
    private Promotion promotion;

    private BigDecimal value; // Số tiền giảm riêng cho món này
}