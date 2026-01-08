package com.example.viti_be.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "inventory")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Inventory extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_variant_id")
    private ProductVariant productVariant;

    @Column(name = "part_component_id")
    private UUID partComponentId; // For part components (linh kiện)

    @Column(name = "quantity_physical")
    @Builder.Default
    private Integer quantityPhysical = 0;

    @Column(name = "quantity_reserved")
    @Builder.Default
    private Integer quantityReserved = 0;

    @Column(name = "quantity_available")
    @Builder.Default
    private Integer quantityAvailable = 0;

    @Column(name = "min_threshold")
    private Integer minThreshold;

    @Column(name = "last_counted_at")
    private LocalDateTime lastCountedAt;

    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "updated_by")
    private UUID updatedBy;

    // Helper method to add stock
    public void addStock(int quantity) {
        this.quantityPhysical += quantity;
        this.quantityAvailable += quantity;
    }

    // Helper method to reduce stock
    public void reduceStock(int quantity) {
        this.quantityPhysical -= quantity;
        this.quantityAvailable -= quantity;
    }

    // Helper method to reserve stock
    public void reserveStock(int quantity) {
        this.quantityReserved += quantity;
        this.quantityAvailable -= quantity;
    }

    // Helper method to release reserved stock
    public void releaseReservedStock(int quantity) {
        this.quantityReserved -= quantity;
        this.quantityAvailable += quantity;
    }
}
