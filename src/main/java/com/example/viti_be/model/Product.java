package com.example.viti_be.model;

import jakarta.persistence.*;
import lombok.*;
import java.util.List;

@Entity
@Table(name = "products")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class Product extends BaseEntity {
    @Column(nullable = false)
    private String name;

    @ManyToOne
    @JoinColumn(name = "category_id")
    private Category category;

    @ManyToOne
    @JoinColumn(name = "supplier_id")
    private Supplier supplier;

    // Lưu JSON thông số chung (VD: {"screen": "15.6 inch"})
    @Column(name = "common_specs", columnDefinition = "TEXT")
    private String commonSpecs;

    @Column(name = "image_url")
    private String imageUrl;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "min_stock_threshold")
    private Integer minStockThreshold;

    private String status; // ACTIVE, HIDDEN

    @Column(name = "warranty_period")
    private Integer warrantyPeriod; // Số tháng bảo hành

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL)
    private List<ProductVariant> variants;
}