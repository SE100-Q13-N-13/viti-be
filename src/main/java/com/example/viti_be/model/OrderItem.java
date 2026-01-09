package com.example.viti_be.model;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(name = "order_items")
public class OrderItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    @ToString.Exclude
    private Order order;

    // 2. Biến thể sản phẩm (SKU)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_variant_id", nullable = false)
    private ProductVariant productVariant;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_serial_id")
    private ProductSerial productSerial;

    @Column(name = "quantity", nullable = false)
    private int quantity;

    @Column(name = "cost_price", precision = 15, scale = 2)
    private BigDecimal costPrice;

    @Column(name = "unit_price", precision = 15, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "subtotal", precision = 15, scale = 2)
    private BigDecimal subtotal;

    @Column(name = "discount", precision = 15, scale = 2)
    private BigDecimal discount;

    @OneToMany(mappedBy = "orderItem", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<OrderItemPromotion> appliedPromotions = new ArrayList<>();

    @Column(name = "warranty_expire_date")
    private LocalDateTime warrantyPeriodSnapshot;

    public BigDecimal getFinalTotal() {
        if (subtotal == null) return BigDecimal.ZERO;
        BigDecimal disc = discount == null ? BigDecimal.ZERO : discount;
        return subtotal.subtract(disc);
    }
}
