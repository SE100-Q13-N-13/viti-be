package com.example.viti_be.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "loyalty_points")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LoyaltyPoint extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", unique = true)
    private Customer customer;

    @Column(name = "total_points")
    private Integer totalPoints = 0;

    @Column(name = "points_available")
    private Integer pointsAvailable = 0;

    @Column(name = "points_used")
    private Integer pointsUsed = 0;

    @Column(name = "point_rate", precision = 10, scale = 2)
    private BigDecimal pointRate = BigDecimal.ZERO;

    @Column(name = "last_earned_at")
    private LocalDateTime lastEarnedAt;

    @Column(name = "last_used_at")
    private LocalDateTime lastUsedAt;
}
