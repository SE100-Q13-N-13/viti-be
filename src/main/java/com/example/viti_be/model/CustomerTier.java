package com.example.viti_be.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "customer_tiers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CustomerTier extends BaseEntity {

    @Column(nullable = false, length = 50)
    private String name;

    @Column(name = "min_point")
    private Integer minPoint;

    @Column(name = "discount_rate", precision = 5, scale = 2)
    private BigDecimal discountRate;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 20)
    private String status;
}
