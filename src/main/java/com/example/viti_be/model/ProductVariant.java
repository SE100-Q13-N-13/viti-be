package com.example.viti_be.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "product_variants")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class ProductVariant extends BaseEntity {

    @ManyToOne
    @JoinColumn(name = "product_id")
    private Product product;

    @Column(unique = true, nullable = false)
    private String sku;

    private String barcode;

    @Column(name = "variant_name")
    private String variantName; // VD: Đen - 8GB - 256GB

    // Thuộc tính riêng (VD: {"color": "Black", "ram": "8GB"})
    @Column(name = "variant_specs", columnDefinition = "TEXT")
    private String variantSpecs;

    @Column(name = "purchase_price_avg")
    private BigDecimal purchasePriceAvg;

    @Column(name = "selling_price")
    private BigDecimal sellingPrice;
}