package com.example.viti_be.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * LINH KIỆN / VẬT TƯ (Master data)
 */
@Entity
@Table(name = "part_components")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PartComponent extends BaseEntity {

    @Column(name = "name", nullable = false, length = 100)
    private String name; // VD: "Pin Laptop Dell 3 cell"

    @Column(name = "part_type", length = 50)
    private String partType; // VD: "RAM", "PIN", "MÀN HÌNH"

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_id")
    private Supplier supplier;

    @Column(name = "unit", length = 20)
    private String unit; // VD: "Cái", "Bộ"

    @Column(name = "purchase_price_avg", precision = 15, scale = 2)
    private BigDecimal purchasePriceAvg;

    @Column(name = "selling_price", precision = 15, scale = 2)
    private BigDecimal sellingPrice;

    @Column(name = "min_stock")
    private Integer minStock; // Tồn tối thiểu

    // Note: Inventory được quản lý trong bảng Inventory (chung với ProductVariant)
}